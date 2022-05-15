/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.io.InputStream;
import java.util.*;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExtensionService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    VSCodeIdService vsCodeIdService;

    @Autowired
    UserService users;

    @Autowired
    SearchUtilService search;

    @Autowired
    CacheService cache;

    @Autowired
    ExtensionValidator validator;

    @Autowired
    StorageUtilService storageUtil;

    @Value("${ovsx.publishing.require-license:false}")
    boolean requireLicense;

    @Transactional(TxType.REQUIRED)
    public ExtensionVersion publishVersion(InputStream content, PersonalAccessToken token) {
        try (var processor = new ExtensionProcessor(content)) {
            // Extract extension metadata from its manifest
            var extVersion = createExtensionVersion(processor, token.getUser(), token);
            processor.getExtensionDependencies().forEach(dep -> addDependency(dep, extVersion));
            processor.getBundledExtensions().forEach(dep -> addBundledExtension(dep, extVersion));

            // Check the extension's license
            var resources = processor.getResources(extVersion);
            checkLicense(extVersion, resources);

            // Store file resources in the DB or external storage
            storeResources(extVersion, resources);

            // Update whether extension is active, the search index and evict cache
            updateExtension(extVersion.getExtension());

            return extVersion;
        }
    }

    private ExtensionVersion createExtensionVersion(ExtensionProcessor processor, UserData user, PersonalAccessToken token) {
        var namespaceName = processor.getNamespace();
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new ErrorResultException("Unknown publisher: " + namespaceName
                    + "\nUse the 'create-namespace' command to create a namespace corresponding to your publisher name.");
        }
        if (!users.hasPublishPermission(user, namespace)) {
            throw new ErrorResultException("Insufficient access rights for publisher: " + namespace.getName());
        }

        var extensionName = processor.getExtensionName();
        var nameIssue = validator.validateExtensionName(extensionName);
        if (nameIssue.isPresent()) {
            throw new ErrorResultException(nameIssue.get().toString());
        }
        var extVersion = processor.getMetadata();
        if (extVersion.getDisplayName() != null && extVersion.getDisplayName().trim().isEmpty()) {
            extVersion.setDisplayName(null);
        }
        extVersion.setTimestamp(TimeUtil.getCurrentUTC());
        extVersion.setPublishedWith(token);
        extVersion.setActive(true);

        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            extension = new Extension();
            extension.setName(extensionName);
            extension.setNamespace(namespace);
            extension.setPublishedDate(extVersion.getTimestamp());

            vsCodeIdService.createPublicId(extension);
            entityManager.persist(extension);
        } else {
            var existingVersion = repositories.findVersion(extVersion.getVersion(), extVersion.getTargetPlatform(), extension);
            if (existingVersion != null) {
                throw new ErrorResultException(
                        "Extension " + namespace.getName() + "." + extension.getName()
                        + " " + extVersion.getVersion()
                        + (TargetPlatform.isUniversal(extVersion) ? "" : " (" + extVersion.getTargetPlatform() + ")")
                        + " is already published"
                        + (existingVersion.isActive() ? "." : ", but is currently inactive and therefore not visible."));
            }
        }
        extension.setLastUpdatedDate(extVersion.getTimestamp());
        extVersion.setExtension(extension);
        extension.getVersions().add(extVersion);
        entityManager.persist(extVersion);

        var metadataIssues = validator.validateMetadata(extVersion);
        if (!metadataIssues.isEmpty()) {
            if (metadataIssues.size() == 1) {
                throw new ErrorResultException(metadataIssues.get(0).toString());
            }
            throw new ErrorResultException("Multiple issues were found in the extension metadata:\n"
                    + Joiner.on("\n").join(metadataIssues));
        }
        return extVersion;
    }

    private void addDependency(String dependency, ExtensionVersion extVersion) {
        var split = dependency.split("\\.");
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            throw new ErrorResultException("Invalid 'extensionDependencies' format. Expected: '${namespace}.${name}'");
        }
        var extensionCount = repositories.countExtensions(split[1], split[0]);
        if (extensionCount == 0) {
            throw new ErrorResultException("Cannot resolve dependency: " + dependency);
        }
        var depList = extVersion.getDependencies();
        if (depList == null) {
            depList = new ArrayList<>();
            extVersion.setDependencies(depList);
        }
        depList.add(dependency);
    }

    private void addBundledExtension(String bundled, ExtensionVersion extVersion) {
        var split = bundled.split("\\.");
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            throw new ErrorResultException("Invalid 'extensionPack' format. Expected: '${namespace}.${name}'");
        }
        var depList = extVersion.getBundledExtensions();
        if (depList == null) {
            depList = new ArrayList<>();
            extVersion.setBundledExtensions(depList);
        }
        depList.add(bundled);
    }

    private void checkLicense(ExtensionVersion extVersion, List<FileResource> resources) {
        if (requireLicense
                && Strings.isNullOrEmpty(extVersion.getLicense())
                && resources.stream().noneMatch(r -> r.getType().equals(FileResource.LICENSE))) {
            throw new ErrorResultException("This extension cannot be accepted because it has no license.");
        }
    }

    private void storeResources(ExtensionVersion extVersion, List<FileResource> resources) {
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        resources.forEach(resource -> {
            if (resource.getType().equals(FileResource.DOWNLOAD)) {
                var resourceName = namespace.getName() + "." + extension.getName() + "-" + extVersion.getVersion();
                if(!TargetPlatform.isUniversal(extVersion)) {
                    resourceName += "@" + extVersion.getTargetPlatform();
                }

                resourceName += ".vsix";
                resource.setName(resourceName);
            }
            if (storageUtil.shouldStoreExternally(resource)) {
                storageUtil.uploadFile(resource);
                // Don't store the binary content in the DB - it's now stored externally
                resource.setContent(null);
            } else {
                resource.setStorageType(FileResource.STORAGE_DB);
            }
            entityManager.persist(resource);
        });
    }

    /**
     * Update the given extension after a version has been published
     * or the {@code active} statuses of its versions have changed.
     */
    @Transactional(TxType.REQUIRED)
    public void updateExtension(Extension extension) {
        cache.evictExtensionJsons(extension);

        if (extension.getLatest() != null) {
            // There is at least one active version => activate the extension
            extension.setActive(true);
            search.updateSearchEntry(extension);
        } else if (extension.isActive()) {
            // All versions are deactivated => deactivate the extensions
            extension.setActive(false);
            search.removeSearchEntry(extension);
        }
    }

    /**
     * Reactivate all extension versions that have been published by the given user.
     */
    @Transactional
    public void reactivateExtensions(UserData user) {
        var accessTokens = repositories.findAccessTokens(user);
        var affectedExtensions = new LinkedHashSet<Extension>();
        for (var accessToken : accessTokens) {
            var versions = repositories.findVersionsByAccessToken(accessToken, false);
            for (var version : versions) {
                version.setActive(true);
                affectedExtensions.add(version.getExtension());
            }
        }
        for (var extension : affectedExtensions) {
            updateExtension(extension);
        }
    }
}