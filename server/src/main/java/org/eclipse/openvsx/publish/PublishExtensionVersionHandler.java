/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import com.google.common.base.Joiner;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
public class PublishExtensionVersionHandler {

    @Autowired
    PublishExtensionVersionService service;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    VSCodeIdService vsCodeIdService;

    @Autowired
    UserService users;

    @Autowired
    ExtensionValidator validator;

    @Transactional
    public ExtensionVersion createExtensionVersion(ExtensionProcessor processor, PersonalAccessToken token) {
        // Extract extension metadata from its manifest
        var extVersion = createExtensionVersion(processor, token.getUser(), token);
        var dependencies = processor.getExtensionDependencies().stream()
                .map(this::checkDependency)
                .collect(Collectors.toList());
        var bundledExtensions = processor.getBundledExtensions().stream()
                .map(this::checkBundledExtension)
                .collect(Collectors.toList());

        extVersion.setDependencies(dependencies);
        extVersion.setBundledExtensions(bundledExtensions);
        return extVersion;
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
        extVersion.setActive(false);

        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            extension = new Extension();
            extension.setActive(false);
            extension.setName(extensionName);
            extension.setNamespace(namespace);
            extension.setPublishedDate(extVersion.getTimestamp());

            var updateExistingPublicIds = vsCodeIdService.setPublicIds(extension);
            if(updateExistingPublicIds) {
                vsCodeIdService.updateExistingPublicIds(extension);
            }

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
        extension.getVersions().add(extVersion);
        extVersion.setExtension(extension);

        var metadataIssues = validator.validateMetadata(extVersion);
        if (!metadataIssues.isEmpty()) {
            if (metadataIssues.size() == 1) {
                throw new ErrorResultException(metadataIssues.get(0).toString());
            }
            throw new ErrorResultException("Multiple issues were found in the extension metadata:\n"
                    + Joiner.on("\n").join(metadataIssues));
        }

        entityManager.persist(extVersion);
        return extVersion;
    }

    private String checkDependency(String dependency) {
        var split = dependency.split("\\.");
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            throw new ErrorResultException("Invalid 'extensionDependencies' format. Expected: '${namespace}.${name}'");
        }
        var extensionCount = repositories.countExtensions(split[1], split[0]);
        if (extensionCount == 0) {
            throw new ErrorResultException("Cannot resolve dependency: " + dependency);
        }

        return dependency;
    }

    private String checkBundledExtension(String bundledExtension) {
        var split = bundledExtension.split("\\.");
        if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty()) {
            throw new ErrorResultException("Invalid 'extensionPack' format. Expected: '${namespace}.${name}'");
        }

        return bundledExtension;
    }

    @Async
    @Retryable
    public void publishAsync(FileResource download, Path extensionFile, ExtensionService extensionService) {
        var extVersion = download.getExtension();
        // Delete file resources in case publishAsync is retried
        service.deleteFileResources(extVersion);
        download.setId(0L);

        service.storeDownload(download, extensionFile);
        service.persistResource(download);
        try(var processor = new ExtensionProcessor(extensionFile)) {
            Consumer<FileResource> consumer = resource -> {
                service.storeResource(resource);
                service.persistResource(resource);
            };

            processor.processEachResource(extVersion, consumer);
            processor.getFileResources(extVersion).forEach(consumer);
        }

        // Update whether extension is active, the search index and evict cache
        service.activateExtension(extVersion, extensionService);
    }
}
