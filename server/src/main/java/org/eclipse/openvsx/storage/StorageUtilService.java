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

import java.net.URI;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class StorageUtilService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    SearchService search;

    @Autowired
    GoogleCloudStorageService googleStorage;

    /** Determines which resource types are stored externally. Default: all except the manifest (package.json) */
    @Value("${ovsx.storage.external-resource-types:download,icon,readme,license}")
    String[] externalResourceTypes;

    public boolean shouldStoreExternally(FileResource resource) {
        return Arrays.asList(externalResourceTypes).contains(resource.getType());
    }

    /**
     * Returns an API URL to the file of the given type, if it exists. To be used in JSON response data.
     */
    public String getFileUrl(ExtensionVersion extVersion, String serverUrl, String type) {
        var resource = repositories.findFileByType(extVersion, type);
        if (resource == null)
            return null;
        return getFileUrl(resource, extVersion, serverUrl);
    }

    private String getFileUrl(FileResource resource, ExtensionVersion extVersion, String serverUrl) {
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        return UrlUtil.createApiUrl(serverUrl, "api", namespace.getName(), extension.getName(), extVersion.getVersion(),
                "file", resource.getName());
    }

    /**
     * Returns the actual access location of a resource.
     */
    public URI getLocation(FileResource resource) {
        switch (resource.getStorageType()) {
            case FileResource.STORAGE_GOOGLE:
                return googleStorage.getLocation(resource.getName(), resource.getExtension());
            case FileResource.STORAGE_DB:
                return URI.create(getFileUrl(resource, resource.getExtension(), UrlUtil.getBaseUrl()));
            default:
                return null;
        }
    }

    /**
     * Adds URLs for the given file types to a map to be used in JSON response data.
     */
    public void addFileUrls(ExtensionVersion extVersion, String serverUrl, Map<String, String> type2Url,
            String... types) {
        for (var type : types) {
            var fileUrl = getFileUrl(extVersion, serverUrl, type);
            if (fileUrl != null)
                type2Url.put(type, fileUrl);
        }
    }

    /**
     * Register a package file download by increasing its download count.
     */
    @Transactional
    public void increaseDownloadCount(ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        extension.setDownloadCount(extension.getDownloadCount() + 1);
        search.updateSearchEntry(extension);
    }

    public HttpHeaders getFileResponseHeaders(String fileName) {
        var headers = new HttpHeaders();
        MediaType fileType = getFileType(fileName);
        headers.setContentType(fileType);
        // Files are requested with a version string in the URL, so their content cannot change
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
        if (fileName.endsWith(".vsix")) {
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        return headers;
    }

    public MediaType getFileType(String fileName) {
        if (fileName.endsWith(".vsix")) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        var contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return MediaType.parseMediaType(contentType);
        }
        return MediaType.TEXT_PLAIN;
    }
    
}