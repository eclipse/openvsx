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
import org.apache.commons.io.IOUtils;
import org.eclipse.openvsx.admin.RemoveFileJobRequest;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.publish.PublishExtensionVersionHandler;
import org.eclipse.openvsx.publish.PublishingConfig;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.scanning.ExtensionScanPersistenceService;
import org.eclipse.openvsx.scanning.ExtensionScanService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.*;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExtensionService {
    private final PublishingConfig publishingConfig;
    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final SearchUtilService search;
    private final CacheService cache;
    private final LogService logs;
    private final PublishExtensionVersionHandler publishHandler;
    private final JobRequestScheduler scheduler;
    private final ExtensionScanService scanService;
    private final ExtensionScanPersistenceService scanPersistenceService;

    public ExtensionService(
            PublishingConfig publishingConfig,
            EntityManager entityManager,
            RepositoryService repositories,
            SearchUtilService search,
            CacheService cache,
            LogService logs,
            PublishExtensionVersionHandler publishHandler,
            JobRequestScheduler scheduler,
            ExtensionScanService scanService,
            ExtensionScanPersistenceService scanPersistenceService
    ) {
        this.publishingConfig = publishingConfig;
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.search = search;
        this.cache = cache;
        this.logs = logs;
        this.publishHandler = publishHandler;
        this.scheduler = scheduler;
        this.scanService = scanService;
        this.scanPersistenceService = scanPersistenceService;
    }

    private long getMaxContentSize() {
        return publishingConfig.getMaxContentSize();
    }

    @Transactional
    public ExtensionVersion mirrorVersion(TempFile extensionFile, String signatureName, PersonalAccessToken token, String binaryName, String timestamp) {
        doPublish(extensionFile, binaryName, token, TimeUtil.fromUTCString(timestamp), false);
        publishHandler.mirror(extensionFile, signatureName);
        return extensionFile.getResource().getExtension();
    }

    public ExtensionVersion publishVersion(InputStream content, PersonalAccessToken token) throws ErrorResultException {
        if (scanService.isEnabled()) {
            return publishVersionWithScan(content, token);
        } else {
            var extensionFile = createExtensionFile(content);
            try {
                doPublish(extensionFile, null, token, TimeUtil.getCurrentUTC(), true);
            } catch (ErrorResultException exc) {
                // In case publication fails early on we need to
                // delete the temporary extension file, otherwise
                // it's deleted within the publishAsync method.
                IOUtils.closeQuietly(extensionFile);
                throw exc;
            }
            publishHandler.publishAsync(extensionFile, this);
            var download = extensionFile.getResource();
            publishHandler.schedulePublicIdJob(download);
            return download.getExtension();
        }
    }

    private ExtensionVersion publishVersionWithScan(InputStream content, PersonalAccessToken token) throws ErrorResultException {
        var extensionFile = createExtensionFile(content);
        ExtensionScan scan = null;

        try (var processor = new ExtensionProcessor(extensionFile)) {
            scan = scanService.initializeScan(processor, token.getUser());

            scanService.runValidation(scan, extensionFile, token.getUser());

            doPublish(extensionFile, null, token, TimeUtil.getCurrentUTC(), true);

            // Publish async handles requesting the longrunning scans
            publishHandler.publishAsync(extensionFile, this, scan);
            var download = extensionFile.getResource();
            publishHandler.schedulePublicIdJob(extensionFile.getResource());
            return download.getExtension();
        } catch (ErrorResultException e) {
            // ErrorResultException is thrown by doPublish when the extension is not valid, so we can remove the scan
            if (scan != null && !scan.isCompleted()) {
                scanService.removeScan(scan);
            }

            // In case publication fails early on we need to
            // delete the temporary extension file, otherwise
            // it's deleted within the publishAsync method.
            IOUtils.closeQuietly(extensionFile);
            throw e;
        }  catch (Exception e) {
            if (scan != null && !scan.isCompleted()) {
                scanService.markScanAsErrored(scan, "Unexpected error: " + e.getMessage());
            }
            throw e;
        }
    }

    private void doPublish(TempFile extensionFile, String binaryName, PersonalAccessToken token, LocalDateTime timestamp, boolean checkDependencies) {
        try (var processor = new ExtensionProcessor(extensionFile)) {
            var extVersion = publishHandler.createExtensionVersion(processor, token, timestamp, checkDependencies);

            var download = processor.getBinary(extVersion, binaryName);
            extensionFile.setResource(download);
        }
    }

    private TempFile createExtensionFile(InputStream content) {
        long maxContentSize = getMaxContentSize();
        try (var input = ByteStreams.limit(new BufferedInputStream(content), maxContentSize + 1)) {
            long size;
            var extensionFile = new TempFile("extension_", ".vsix");
            try(var out = Files.newOutputStream(extensionFile.getPath())) {
                size = input.transferTo(out);
            }

            if (size > maxContentSize) {
                IOUtils.closeQuietly(extensionFile);
                var maxSize = FileUtils.byteCountToDisplaySize(maxContentSize);
                throw new ErrorResultException(
                    "The extension package exceeds the size limit of " + maxSize + ".", HttpStatus.CONTENT_TOO_LARGE
                );
            }

            return extensionFile;
        } catch (IOException e) {
            throw new ErrorResultException("Failed to read extension file", e);
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

        extension.setLastUpdatedDate(TimeUtil.getCurrentUTC());
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

    /**
     * Delete an extension version published by the given user.
     * <p>
     * If the resolved extension version has not been published by the given user,
     * a {@code ErrorResultException} will be thrown.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteUserExtension(
        UserData user,
        String namespaceName,
        String extensionName,
        TargetPlatformVersion... targetVersions
    ) throws ErrorResultException {
        return deleteExtension(user, true, namespaceName, extensionName, targetVersions);
    }

    /**
     * Deletes the given extension.
     * <p>
     * If {@code restrictedToUser} is {@code true}, the deletion operation is only successful if the user
     * has published the respective extension version.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(
            UserData user,
            boolean restrictedToUser,
            String namespaceName,
            String extensionName,
            TargetPlatformVersion... targetVersions
    ) throws ErrorResultException {
        var extension = lockExtension(namespaceName, extensionName);
        if (repositories.isDeleteAllVersions(restrictedToUser ? user : null, namespaceName, extensionName, targetVersions)) {
            return deleteExtension(user, extension);
        }

        return deleteExtensionVersions(user, Arrays.stream(targetVersions)
                .map(target -> {
                    var extVersion = restrictedToUser ?
                        repositories.findVersionPublishedWithUser(user, target.version(), target.targetPlatform(), extensionName, namespaceName) :
                        repositories.findVersion(target.version(), target.targetPlatform(), extensionName, namespaceName);

                    if (extVersion == null) {
                        throw new ErrorResultException(
                            "Extension not found: " + NamingUtil.toLogFormat(namespaceName, extensionName, target.targetPlatform(), target.version()),
                            HttpStatus.NOT_FOUND);
                    }
                    return extVersion;
                })
                .toList());
    }

    /**
     * Deletes the given pre-resolved extension versions without any ownership check.
     * Callers are responsible for authorisation (e.g. scoping the lookup to the owning user
     * before calling this, or using an admin-level unscoped lookup).
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtensionVersions(UserData user, List<ExtensionVersion> versions) {
        var results = new ArrayList<ResultJson>();
        for (var version : versions) {
            results.add(deleteExtensionVersion(user, version));
        }
        return combineResults(results);
    }

    /**
     * Locks the extension row ({@code SELECT … FOR UPDATE NOWAIT}).
     * this fails fast with {@code 409}
     */
    private Extension lockExtension(String namespaceName, String extensionName) throws ErrorResultException {
        Extension extension;
        try {
            extension = repositories.findExtensionForUpdateNoWait(extensionName, namespaceName);
        } catch (PessimisticLockingFailureException e) {
            throw new ErrorResultException(
                    "Extension " + NamingUtil.toExtensionId(namespaceName, extensionName)
                            + " can not be locked due to concurrent modification. Please try again.",
                    HttpStatus.CONFLICT);
        }
        if (extension == null) {
            throw new ErrorResultException(
                    "Extension not found: " + NamingUtil.toExtensionId(namespaceName, extensionName),
                    HttpStatus.NOT_FOUND);
        }
        return extension;
    }

    /**
     * Merges the per-version delete outcomes into a single result, concatenating any success and
     * error messages.
     */
    private ResultJson combineResults(List<ResultJson> results) {
        var result = new ResultJson();
        result.setError(results.stream().map(ResultJson::getError).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        result.setSuccess(results.stream().map(ResultJson::getSuccess).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(UserData user, Extension extension) throws ErrorResultException {
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

        for (var extVersion : repositories.findVersions(extension)) {
            removeExtensionVersion(extVersion);
        }

        for (var review : repositories.findAllReviews(extension)) {
            entityManager.remove(review);
        }

        var deprecatedExtensions = repositories.findDeprecatedExtensions(extension);
        for (var deprecatedExtension : deprecatedExtensions) {
            deprecatedExtension.setReplacement(null);
            cache.evictExtensionJsons(deprecatedExtension);
        }

        entityManager.remove(extension);

        // evict the cache entries only after the changes have been committed
        cache.evictExtensionJsons(extension);
        cache.evictNamespaceDetails(extension);
        cache.evictLatestExtensionVersion(extension);

        search.removeSearchEntry(extension);

        var result = ResultJson.success("Deleted " + NamingUtil.toExtensionId(extension));
        logs.logAction(user, result);
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtensionVersion(UserData user, ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        removeExtensionVersion(extVersion);
        extension.getVersions().remove(extVersion);
        updateExtension(extension);

        var result = ResultJson.success("Deleted " + NamingUtil.toLogFormat(extVersion));
        logs.logAction(user, result);
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public void removeExtensionVersion(ExtensionVersion extVersion) {
        // Clean up any pending scan jobs for this extension version
        // to prevent "file not found" errors after deletion
        scanPersistenceService.deleteScansForExtensionVersion(extVersion.getId());

        repositories.findFiles(extVersion).map(RemoveFileJobRequest::new).forEach(scheduler::enqueue);
        repositories.deleteFiles(extVersion);
        entityManager.remove(extVersion);
    }
}
