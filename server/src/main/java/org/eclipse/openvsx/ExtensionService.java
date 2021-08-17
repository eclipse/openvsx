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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExtensionService {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UserService users;

    @Autowired
    SearchService search;

    @Autowired
    ExtensionValidator validator;

    @Autowired
    StorageUtilService storageUtil;

    @Value("${ovsx.publishing.require-license:false}")
    boolean requireLicense;

    @Transactional(TxType.REQUIRED)
    public ExtensionVersion publishVersion(InputStream content, PersonalAccessToken token, PublishOptions publishOptions) {
        try (var processor = new ExtensionProcessor(content, publishOptions)) {
            // Extract extension metadata from its manifest
            var extVersion = createExtensionVersion(processor, token.getUser(), token);
            processor.getExtensionDependencies().forEach(dep -> addDependency(dep, extVersion));
            processor.getBundledExtensions().forEach(dep -> addBundledExtension(dep, extVersion));

            // Check the extension's license
            var resources = processor.getResources(extVersion);
            checkLicense(extVersion, resources);

            // Update the 'latest' / 'preview' references and the search index
            updateExtension(extVersion.getExtension());

            // Store file resources in the DB or external storage
            storeResources(extVersion, resources);

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
            entityManager.persist(extension);
        } else {
            var existingVersion = repositories.findVersion(extVersion.getVersion(), extension);
            if (existingVersion != null) {
                throw new ErrorResultException(
                        "Extension " + namespace.getName() + "." + extension.getName()
                        + " version " + extVersion.getVersion()
                        + " is already published"
                        + (existingVersion.isActive() ? "." : ", but is currently inactive and therefore not visible."));
            }
        }
        extVersion.setExtension(extension);
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
        var extensionCount = repositories.countExtensions(split[1], split[0]);
        if (extensionCount == 0) {
            throw new ErrorResultException("Cannot resolve bundled extension: " + bundled);
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
                resource.setName(namespace.getName() + "." + extension.getName() + "-" + extVersion.getVersion() + ".vsix");
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
        extension.setLatest(getLatestVersion(extension, false));
        extension.setPreview(getLatestVersion(extension, true));
        if (extension.getLatest() == null) {
            // Use a preview version as latest if it's the only available version
            extension.setLatest(extension.getPreview());
        }

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

    private ExtensionVersion getLatestVersion(Extension extension, boolean preview) {
        ExtensionVersion latest = null;
        SemanticVersion latestSemver = null;
        for (var extVer : repositories.findActiveVersions(extension, preview)) {
            var semver = extVer.getSemanticVersion();
            if (latestSemver == null || latestSemver.compareTo(semver) < 0) {
                latest = extVer;
                latestSemver = semver;
            }
        }
        return latest;
    }

    /**
     * Update the given extension after one or more version have been deleted. The given list
     * of versions should reflect the applied deletion.
     */
    @Transactional(TxType.REQUIRED)
    public void updateExtension(Extension extension, Iterable<ExtensionVersion> versions) {
        extension.setLatest(getLatestVersion(versions, false));
        extension.setPreview(getLatestVersion(versions, true));
        if (extension.getLatest() == null) {
            // Use a preview version as latest if it's the only available version
            extension.setLatest(extension.getPreview());
        }

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

    private ExtensionVersion getLatestVersion(Iterable<ExtensionVersion> versions, boolean preview) {
        ExtensionVersion latest = null;
        SemanticVersion latestSemver = null;
        for (var extVer : versions) {
            if (extVer.isActive() && extVer.isPreview() == preview) {
                var semver = extVer.getSemanticVersion();
                if (latestSemver == null || latestSemver.compareTo(semver) < 0) {
                    latest = extVer;
                    latestSemver = semver;
                }
            }
        }
        return latest;
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