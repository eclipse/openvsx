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
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.adapter.VSCodeIdNewExtensionJobRequest;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.extension_control.ExtensionControlService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.ExtensionId;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

@Component
public class PublishExtensionVersionHandler {

    protected final Logger logger = LoggerFactory.getLogger(PublishExtensionVersionHandler.class);

    private final PublishExtensionVersionService service;
    private final ExtensionVersionIntegrityService integrityService;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final JobRequestScheduler scheduler;
    private final UserService users;
    private final ExtensionValidator validator;
    private final ExtensionControlService extensionControl;

    public PublishExtensionVersionHandler(
            PublishExtensionVersionService service,
            ExtensionVersionIntegrityService integrityService,
            EntityManager entityManager,
            RepositoryService repositories,
            JobRequestScheduler scheduler,
            UserService users,
            ExtensionValidator validator,
            ExtensionControlService extensionControl
    ) {
        this.service = service;
        this.integrityService = integrityService;
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.scheduler = scheduler;
        this.users = users;
        this.validator = validator;
        this.extensionControl = extensionControl;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ExtensionVersion createExtensionVersion(ExtensionProcessor processor, PersonalAccessToken token, LocalDateTime timestamp, boolean checkDependencies) {
        // Extract extension metadata from its manifest
        var extVersion = createExtensionVersion(processor, token.getUser(), token, timestamp);
        var dependencies = processor.getExtensionDependencies();
        var bundledExtensions = processor.getBundledExtensions();
        if (checkDependencies) {
            var parsedDependencies = dependencies.stream()
                    .map(id -> parseExtensionId(id, "extensionDependencies"))
                    .toList();

            if(!parsedDependencies.isEmpty()) {
                checkDependencies(parsedDependencies);
            }
            bundledExtensions.forEach(id -> parseExtensionId(id, "extensionPack"));
        }

        extVersion.setDependencies(dependencies);
        extVersion.setBundledExtensions(bundledExtensions);
        if(integrityService.isEnabled()) {
            extVersion.setSignatureKeyPair(repositories.findActiveKeyPair());
        }

        return extVersion;
    }

    private ExtensionVersion createExtensionVersion(ExtensionProcessor processor, UserData user, PersonalAccessToken token, LocalDateTime timestamp) {
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
        if(isMalicious(namespaceName, extensionName)) {
            throw new ErrorResultException(NamingUtil.toExtensionId(namespaceName, extensionName) + " is a known malicious extension");
        }

        var version = processor.getVersion();
        var versionIssue = validator.validateExtensionVersion(version);
        if (versionIssue.isPresent()) {
            throw new ErrorResultException(versionIssue.get().toString());
        }

        var extVersion = processor.getMetadata();
        if (extVersion.getDisplayName() != null && extVersion.getDisplayName().trim().isEmpty()) {
            extVersion.setDisplayName(null);
        }
        extVersion.setTimestamp(timestamp);
        extVersion.setPublishedWith(token);
        extVersion.setActive(false);

        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            extension = new Extension();
            extension.setActive(false);
            extension.setName(extensionName);
            extension.setNamespace(namespace);
            extension.setPublishedDate(extVersion.getTimestamp());
            extension.setDeprecated(false);
            extension.setDownloadable(true);

            entityManager.persist(extension);
        } else {
            var existingVersion = repositories.findVersion(extVersion.getVersion(), extVersion.getTargetPlatform(), extension);
            if (existingVersion != null) {
                var extVersionId = NamingUtil.toLogFormat(namespaceName, extensionName, extVersion.getTargetPlatform(), extVersion.getVersion());
                var message = "Extension " + extVersionId + " is already published";
                message += existingVersion.isActive() ? "." : ", but currently isn't active and therefore not visible.";
                throw new ErrorResultException(message);
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

    private boolean isMalicious(String namespace, String extension) {
        try {
            var maliciousExtensionIds = extensionControl.getMaliciousExtensionIds();
            return maliciousExtensionIds.contains(NamingUtil.toExtensionId(namespace, extension));
        } catch(IOException e) {
            logger.warn("Failed to check whether extension is malicious or not", e);
            return false;
        }
    }

    private void checkDependencies(List<ExtensionId> dependencies) {
        var unresolvedDependency = repositories.findFirstUnresolvedDependency(dependencies);
        if (unresolvedDependency != null) {
            throw new ErrorResultException("Cannot resolve dependency: " + unresolvedDependency);
        }
    }

    private ExtensionId parseExtensionId(String extensionIdText, String formatType) {
        var extensionId = NamingUtil.fromExtensionId(extensionIdText);
        if (extensionId == null) {
            throw new ErrorResultException("Invalid '" + formatType + "' format. Expected: '${namespace}.${name}'");
        }

        return extensionId;
    }

    @Async
    @Retryable
    public void publishAsync(TempFile extensionFile, ExtensionService extensionService) {
        var download = extensionFile.getResource();
        var extVersion = download.getExtension();

        // Delete file resources in case publishAsync is retried
        service.deleteFileResources(extVersion);
        download.setId(0L);

        service.storeResource(extensionFile);
        service.persistResource(download);
        try(var processor = new ExtensionProcessor(extensionFile)) {
            extVersion.setPotentiallyMalicious(processor.isPotentiallyMalicious());
            if (extVersion.isPotentiallyMalicious()) {
                logger.warn("Extension version is potentially malicious: {}", NamingUtil.toLogFormat(extVersion));
                return;
            }

            Consumer<TempFile> consumer = tempFile -> {
                service.storeResource(tempFile);
                service.persistResource(tempFile.getResource());
            };

            if(integrityService.isEnabled()) {
                var keyPair = extVersion.getSignatureKeyPair();
                if(keyPair != null) {
                    try(var signature = integrityService.generateSignature(extensionFile, keyPair)) {
                        consumer.accept(signature);
                    }
                } else {
                    // Can happen when GenerateKeyPairJobRequestHandler hasn't run yet and there is no active SignatureKeyPair.
                    // This extension version should be assigned a SignatureKeyPair and a signature FileResource should be created
                    // by the ExtensionVersionSignatureJobRequestHandler migration.
                    logger.warn("Integrity service is enabled, but {} did not have an active key pair", NamingUtil.toLogFormat(extVersion));
                }
            }

            processor.getFileResources(extVersion, consumer);
            try (var sha256File = processor.generateSha256Checksum(extVersion)) {
                consumer.accept(sha256File);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                extensionFile.close();
            } catch (IOException e) {
                logger.error("failed to delete temp file", e);
            }
        }

        // Update whether extension is active, the search index and evict cache
        service.activateExtension(extVersion, extensionService);
    }

    public void mirror(TempFile extensionFile, String signatureName) {
        var download = extensionFile.getResource();
        var extVersion = download.getExtension();
        service.mirrorResource(extensionFile);
        if(signatureName != null) {
            service.mirrorResource(getSignatureResource(signatureName, extVersion));
        }
        try(var processor = new ExtensionProcessor(extensionFile)) {
            // don't store file resources, they can be generated on the fly to avoid traversing entire zip file
            processor.getFileResources(extVersion, service::mirrorResource);
            try (var sha256File = processor.generateSha256Checksum(extVersion)) {
                service.mirrorResource(sha256File);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private FileResource getSignatureResource(String signatureName, ExtensionVersion extVersion) {
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName(signatureName);
        resource.setType(FileResource.DOWNLOAD_SIG);
        return resource;
    }

    public void schedulePublicIdJob(FileResource download) {
        var extension = download.getExtension().getExtension();
        if (StringUtils.isEmpty(extension.getPublicId())) {
            var namespace = extension.getNamespace();
            scheduler.enqueue(new VSCodeIdNewExtensionJobRequest(namespace.getName(), extension.getName()));
        }
    }
}
