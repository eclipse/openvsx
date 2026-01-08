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

import com.google.common.io.ByteStreams;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.admin.RemoveFileJobRequest;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.TargetPlatformVersionJson;
import org.eclipse.openvsx.publish.PublishExtensionVersionHandler;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.scanning.SecretScanningService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerErrorException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ExtensionService {

    private static final int MAX_CONTENT_SIZE = 512 * 1024 * 1024;

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final SearchUtilService search;
    private final CacheService cache;
    private final PublishExtensionVersionHandler publishHandler;
    private final JobRequestScheduler scheduler;
    private final SecretScanningService secretScanningService;

    @Value("${ovsx.publishing.require-license:false}")
    boolean requireLicense;

    @Value("${ovsx.publishing.max-content-size:" + MAX_CONTENT_SIZE + "}")
    int maxContentSize;

    public ExtensionService(
            EntityManager entityManager,
            RepositoryService repositories,
            SearchUtilService search,
            CacheService cache,
            PublishExtensionVersionHandler publishHandler,
            JobRequestScheduler scheduler,
            SecretScanningService secretScanningService
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.search = search;
        this.cache = cache;
        this.publishHandler = publishHandler;
        this.scheduler = scheduler;
        this.secretScanningService = secretScanningService;
    }

    @Transactional
    public ExtensionVersion mirrorVersion(TempFile extensionFile, String signatureName, PersonalAccessToken token, String binaryName, String timestamp) {
        doPublish(extensionFile, binaryName, token, TimeUtil.fromUTCString(timestamp), false);
        publishHandler.mirror(extensionFile, signatureName);
        return extensionFile.getResource().getExtension();
    }

    public ExtensionVersion publishVersion(InputStream content, PersonalAccessToken token) throws ErrorResultException {
        var extensionFile = createExtensionFile(content);
        doPublish(extensionFile, null, token, TimeUtil.getCurrentUTC(), true);
        publishHandler.publishAsync(extensionFile, this);
        var download = extensionFile.getResource();
        publishHandler.schedulePublicIdJob(download);
        return download.getExtension();
    }

    private void doPublish(TempFile extensionFile, String binaryName, PersonalAccessToken token, LocalDateTime timestamp, boolean checkDependencies) {
        // Scan for secrets before processing the extension
        // This fails fast if secrets are detected, preventing publication
        if (secretScanningService.isEnabled()) {
            var scanResult = secretScanningService.scanForSecrets(extensionFile);
            if (scanResult.isSecretsFound()) {
                var findings = scanResult.getFindings();
                var errorMessage = new StringBuilder();
                errorMessage.append("Extension publication blocked: potential secrets detected in the package.\n\n");
                errorMessage.append("The following potential secrets were found:\n");
                
                int maxFindings = Math.min(5, findings.size());
                for (int i = 0; i < maxFindings; i++) {
                    errorMessage.append("  ").append(i + 1).append(". ").append(findings.get(i).toString()).append("\n");
                }
                
                if (findings.size() > maxFindings) {
                    errorMessage.append("  ... and ").append(findings.size() - maxFindings).append(" more\n");
                }
                
                errorMessage.append("\nPlease remove these secrets before publishing. ");
                errorMessage.append("Consider using environment variables or configuration files that are not included in the package. ");
                
                errorMessage.append("Refer to the publishing guidelines: https://github.com/EclipseFdn/open-vsx.org/wiki/Publishing-Extensions");
                
                throw new ErrorResultException(errorMessage.toString());
            }
        }
        
        try (var processor = new ExtensionProcessor(extensionFile)) {
            var extVersion = publishHandler.createExtensionVersion(processor, token, timestamp, checkDependencies);
            if (requireLicense) {
                // Check the extension's license
                try(var licenseFile = processor.getLicense(extVersion)) {
                    checkLicense(extVersion, licenseFile);
                } catch (IOException e) {
                    throw new ServerErrorException("Failed read license file", e);
                }
            }

            var download = processor.getBinary(extVersion, binaryName);
            extensionFile.setResource(download);
        }
    }

    private TempFile createExtensionFile(InputStream content) {
        try (var input = ByteStreams.limit(new BufferedInputStream(content), maxContentSize + 1)) {
            long size;
            var extensionFile = new TempFile("extension_", ".vsix");
            try(var out = Files.newOutputStream(extensionFile.getPath())) {
                size = input.transferTo(out);
            }

            if (size > maxContentSize) {
                try {
                    extensionFile.close();
                } catch (IOException _) {}
                var maxSize = FileUtils.byteCountToDisplaySize(maxContentSize);
                throw new ErrorResultException("The extension package exceeds the size limit of " + maxSize + ".", HttpStatus.PAYLOAD_TOO_LARGE);
            }

            return extensionFile;
        } catch (IOException e) {
            throw new ErrorResultException("Failed to read extension file", e);
        }
    }

    private void checkLicense(ExtensionVersion extVersion, TempFile licenseFile) {
        if (StringUtils.isEmpty(extVersion.getLicense()) && (licenseFile == null || !licenseFile.getResource().getType().equals(FileResource.LICENSE))) {
            throw new ErrorResultException("This extension cannot be accepted because it has no license.");
        }
    }

    /**
     * Update the given extension after a version has been published
     * or the {@code active} statuses of its versions have changed.
     */
    @Transactional(TxType.REQUIRED)
    public void updateExtension(Extension extension) {
        cache.evictNamespaceDetails(extension);
        cache.evictLatestExtensionVersion(extension);
        cache.evictExtensionJsons(extension);

        if (extension.getVersions().stream().anyMatch(ExtensionVersion::isActive)) {
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
        var affectedExtensions = new LinkedHashSet<Extension>();
        var versions = repositories.findVersionsByUser(user, false);
        for (var version : versions) {
            version.setActive(true);
            affectedExtensions.add(version.getExtension());
        }
        for (var extension : affectedExtensions) {
            updateExtension(extension);
        }
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(
            String namespaceName,
            String extensionName,
            List<TargetPlatformVersionJson> targetVersions,
            UserData user
    ) throws ErrorResultException {
        var results = new ArrayList<ResultJson>();
        if(repositories.isDeleteAllVersions(namespaceName, extensionName, targetVersions, user)) {
            var extension = repositories.findExtension(extensionName, namespaceName);
            results.add(deleteExtension(extension));
        } else {
            for (var targetVersion : targetVersions) {
                var extVersion = repositories.findVersion(user, targetVersion.version(), targetVersion.targetPlatform(), extensionName, namespaceName);
                if (extVersion == null) {
                    var message = "Extension not found: " + NamingUtil.toLogFormat(namespaceName, extensionName, targetVersion.targetPlatform(), targetVersion.version());
                    throw new ErrorResultException(message, HttpStatus.NOT_FOUND);
                }

                results.add(deleteExtension(extVersion));
            }
        }

        var result = new ResultJson();
        result.setError(results.stream().map(ResultJson::getError).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        result.setSuccess(results.stream().map(ResultJson::getSuccess).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        return result;
    }

    protected ResultJson deleteExtension(Extension extension) throws ErrorResultException {
        var bundledRefs = repositories.findBundledExtensionsReference(extension);
        if (!bundledRefs.isEmpty()) {
            throw new ErrorResultException("Extension " + NamingUtil.toExtensionId(extension)
                    + " is bundled by the following extension packs: "
                    + bundledRefs.stream()
                    .map(NamingUtil::toFileFormat)
                    .collect(Collectors.joining(", ")));
        }
        var dependRefs = repositories.findDependenciesReference(extension);
        if (!dependRefs.isEmpty()) {
            throw new ErrorResultException("The following extensions have a dependency on " + NamingUtil.toExtensionId(extension) + ": "
                    + dependRefs.stream()
                    .map(NamingUtil::toFileFormat)
                    .collect(Collectors.joining(", ")));
        }

        cache.evictExtensionJsons(extension);
        for (var extVersion : repositories.findVersions(extension)) {
            removeExtensionVersion(extVersion);
        }
        for (var review : repositories.findAllReviews(extension)) {
            entityManager.remove(review);
        }

        var deprecatedExtensions = repositories.findDeprecatedExtensions(extension);
        for(var deprecatedExtension : deprecatedExtensions) {
            deprecatedExtension.setReplacement(null);
            cache.evictExtensionJsons(deprecatedExtension);
        }

        entityManager.remove(extension);
        search.removeSearchEntry(extension);

        return ResultJson.success("Deleted " + NamingUtil.toExtensionId(extension));
    }

    protected ResultJson deleteExtension(ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        removeExtensionVersion(extVersion);
        extension.getVersions().remove(extVersion);
        updateExtension(extension);

        return ResultJson.success("Deleted " + NamingUtil.toLogFormat(extVersion));
    }

    private void removeExtensionVersion(ExtensionVersion extVersion) {
        repositories.findFiles(extVersion).map(RemoveFileJobRequest::new).forEach(scheduler::enqueue);
        repositories.deleteFiles(extVersion);
        entityManager.remove(extVersion);
    }
}