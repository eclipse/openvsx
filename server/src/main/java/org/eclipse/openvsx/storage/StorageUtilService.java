/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Download;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.entities.FileResource.*;

/**
 * Provides utility around storing file resources and acts as a composite storage
 * service around the actually configured external services.
 */
@Component
public class StorageUtilService implements IStorageService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Autowired
    AzureDownloadCountService azureDownloadCountService;

    @Autowired
    SearchUtilService search;

    @Autowired
    CacheService cache;

    @Autowired
    EntityManager entityManager;

    /** Determines which external storage service to use in case multiple services are configured. */
    @Value("${ovsx.storage.primary-service:}")
    String primaryService;

    /** Determines which resource types are stored externally. Default: all resources ({@code *}) */
    @Value("${ovsx.storage.external-resource-types:*}")
    String[] externalResourceTypes;

    public boolean shouldStoreExternally(FileResource resource) {
        if (!isEnabled()) {
            return false;
        }
        if (externalResourceTypes.length == 1 && "*".equals(externalResourceTypes[0])) {
            return true;
        }
        return Arrays.asList(externalResourceTypes).contains(resource.getType());
    }

    @Override
    public boolean isEnabled() {
        return googleStorage.isEnabled() || azureStorage.isEnabled();
    }

    public String getActiveStorageType() {
        var storageTypes = new ArrayList<String>(2);
        if (googleStorage.isEnabled())
            storageTypes.add(STORAGE_GOOGLE);
        if (azureStorage.isEnabled())
            storageTypes.add(STORAGE_AZURE);
        if (!Strings.isNullOrEmpty(primaryService)) {
            if (!storageTypes.contains(primaryService))
                throw new RuntimeException("The selected primary storage service is not available.");
            return primaryService;
        }
        if (storageTypes.isEmpty())
            return STORAGE_DB;
        if (storageTypes.size() == 1)
            return storageTypes.get(0);
        throw new RuntimeException("Multiple external storage services are available. Please select a primary service.");
    }

    @Override
    public void uploadFile(FileResource resource) {
        var storageType = getActiveStorageType();
        switch (storageType) {
            case STORAGE_GOOGLE:
                googleStorage.uploadFile(resource);
                break;
            case STORAGE_AZURE:
                azureStorage.uploadFile(resource);
                break;
            default:
                throw new RuntimeException("External storage is not available.");
        }

        resource.setStorageType(storageType);
    }

    @Override
    public void uploadFile(FileResource resource, Path filePath) {
        var storageType = getActiveStorageType();
        switch (storageType) {
            case STORAGE_GOOGLE:
                googleStorage.uploadFile(resource, filePath);
                break;
            case STORAGE_AZURE:
                azureStorage.uploadFile(resource, filePath);
                break;
            default:
                throw new RuntimeException("External storage is not available.");
        }

        resource.setStorageType(storageType);
    }

    @Override
    public void removeFile(FileResource resource) {
        switch (resource.getStorageType()) {
            case STORAGE_GOOGLE:
                googleStorage.removeFile(resource);
                break;
            case STORAGE_AZURE:
                azureStorage.removeFile(resource);
                break;
        }
    }

    @Override
    public URI getLocation(FileResource resource) {
        switch (resource.getStorageType()) {
            case STORAGE_GOOGLE:
                return googleStorage.getLocation(resource);
            case STORAGE_AZURE:
                return azureStorage.getLocation(resource);
            case STORAGE_DB:
                return URI.create(getFileUrl(resource.getName(), resource.getExtension(), UrlUtil.getBaseUrl()));
            default:
                return null;
        }
    }

    private String getFileUrl(String name, ExtensionVersion extVersion, String serverUrl) {
        return UrlUtil.createApiFileUrl(serverUrl, extVersion, name);
    }

    public Map<String, String> getFileUrls(ExtensionVersion extVersion, String serverUrl, String... types) {
        var fileUrls = getFileUrls(List.of(extVersion), serverUrl, types);
        return fileUrls.get(extVersion.getId());
    }

    /**
     * Returns URLs for the given file types as a map of ExtensionVersion.id by a map of type by file URL, to be used in JSON response data.
     */
    public Map<Long, Map<String, String>> getFileUrls(Collection<ExtensionVersion> extVersions, String serverUrl, String... types) {
        var type2Url = extVersions.stream()
                .map(ev -> new AbstractMap.SimpleEntry<Long, Map<String, String>>(ev.getId(), Maps.newLinkedHashMapWithExpectedSize(types.length)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var resources = repositories.findFilesByType(extVersions, Arrays.asList(types));
        for (var resource : resources) {
            var extVersion = resource.getExtension();
            type2Url.get(extVersion.getId()).put(resource.getType(), getFileUrl(resource.getName(), extVersion, serverUrl));
        }

        return type2Url;
    }

    @Transactional
    public void increaseDownloadCount(FileResource resource) {
        if(azureDownloadCountService.isEnabled()) {
            // don't count downloads twice
            return;
        }

        var download = new Download();
        download.setAmount(1);
        download.setTimestamp(TimeUtil.getCurrentUTC());
        download.setFileResourceId(resource.getId());
        entityManager.persist(download);

        resource = entityManager.merge(resource);
        var extension = resource.getExtension().getExtension();
        extension.setDownloadCount(extension.getDownloadCount() + 1);

        cache.evictExtensionJsons(extension);
        if (extension.isActive()) {
            search.updateSearchEntry(extension);
        }
    }

    public HttpHeaders getFileResponseHeaders(String fileName) {
        var headers = new HttpHeaders();
        headers.setContentType(StorageUtil.getFileType(fileName));
        if (fileName.endsWith(".vsix")) {
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        } else {
            headers.setCacheControl(StorageUtil.getCacheControl(fileName));
        }
        return headers;
    }

    @Transactional
    public ResponseEntity<byte[]> getFileResponse(FileResource resource) {
        resource = entityManager.merge(resource);
        if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var headers = getFileResponseHeaders(resource.getName());
            return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(getLocation(resource))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .build();
        }
    }
}