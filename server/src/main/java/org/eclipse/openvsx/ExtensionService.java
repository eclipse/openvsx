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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.io.ByteStreams;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

import static java.util.Objects.requireNonNull;

@Service
public class ExtensionService {

    private static final Logger logger = LoggerFactory.getLogger(ExtensionService.class);

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
    public ExtensionVersion mirrorVersion(
            TempFile extensionFile,
            String signatureName,
            UserData user,
            String binaryName,
            String timestamp
    ) {
        try (var processor = new ExtensionProcessor(extensionFile)) {
            doPublish(processor, binaryName, user, TimeUtil.fromUTCString(timestamp), false);
        }
        publishHandler.mirror(extensionFile, signatureName);
        return extensionFile.getResource().getExtension();
    }

    public TempFile createExtensionFile(InputStream content) {
        requireNonNull(content);
        long maxContentSize = getMaxContentSize();
        try (var input = ByteStreams.limit(new BufferedInputStream(content), maxContentSize + 1)) {
            long size;
            var extensionFile = new TempFile("extension_", ".vsix");
            try (var out = Files.newOutputStream(extensionFile.getPath())) {
                size = input.transferTo(out);
            }

            if (size > maxContentSize) {
                IOUtils.closeQuietly(extensionFile);
                var maxSize = FileUtils.byteCountToDisplaySize(maxContentSize);
                throw new ErrorResultException(
                        "The extension package exceeds the size limit of " + maxSize + ".",
                        HttpStatus.CONTENT_TOO_LARGE);
            }

            return extensionFile;
        } catch (IOException e) {
            throw new ErrorResultException("Failed to read extension file", e);
        }
    }

    public ExtensionVersion publishVersion(InputStream inputStream, UserData user)
            throws ErrorResultException {
        try (
                TempFile tempFile = createExtensionFile(inputStream);
                ExtensionProcessor processor = new ExtensionProcessor(tempFile)
        ) {
            return publishVersion(processor, user);
        } catch (IOException e) {
            throw new ErrorResultException("Failed to read extension file", e);
        }
    }

    public ExtensionVersion publishVersion(ExtensionProcessor processor, UserData user)
            throws ErrorResultException {
        requireNonNull(processor);
        requireNonNull(user);
        var content = processor.getExtensionFile();
        if (scanService.isEnabled()) {
            return publishVersionWithScan(processor, user);
        } else {
            try {
                doPublish(processor, null, user, TimeUtil.getCurrentUTC(), true);
            } catch (ErrorResultException exc) {
                // In case publication fails early on we need to
                // delete the temporary extension file, otherwise
                // it's deleted within the publishAsync method.
                IOUtils.closeQuietly(content);
                throw exc;
            }
            publishHandler.publishAsync(content, this);
            var download = content.getResource();
            publishHandler.schedulePublicIdJob(download);
            return download.getExtension();
        }
    }

    private ExtensionVersion publishVersionWithScan(ExtensionProcessor processor, UserData user)
            throws ErrorResultException {
        var extensionFile = processor.getExtensionFile();
        ExtensionScan scan = null;

        try {
            // Fail before any validation or scanning happens (and before a scan record is stored) if the
            // extension version can not be published anyway, e.g. because the publisher lacks the access
            // rights for the namespace or the version is published already.
            publishHandler.checkPublishPreconditions(processor, user);

            scan = scanService.initializeScan(processor, user);

            scanService.runValidation(scan, extensionFile, user);

            doPublish(processor, null, user, TimeUtil.getCurrentUTC(), true);

            // Publish async handles requesting the long-running scans
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
        } catch (Exception e) {
            if (scan != null && !scan.isCompleted()) {
                scanService.markScanAsErrored(scan, "Unexpected error: " + e.getMessage());
            }
            throw e;
        }
    }

    private void doPublish(
            ExtensionProcessor processor,
            String binaryName,
            UserData user,
            LocalDateTime timestamp,
            boolean checkDependencies
    ) {
        var extVersion = publishHandler
                .createExtensionVersion(processor, user, timestamp, checkDependencies);
        var download = processor.getBinary(extVersion, binaryName);
        processor.getExtensionFile().setResource(download);
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
        var now = TimeUtil.getCurrentUTC();
        for (var version : versions) {
            if (canBeReactivated(version)) {
                version.setActive(true);
                // the version becomes publicly visible again, which the changes feed reports as a
                // further entry for it, after the one for its deactivation
                repositories.recordExtensionVersionChange(version, ExtensionVersionState.ACTIVE, now);
                affectedExtensions.add(version.getExtension());
            } else {
                logger.warn(
                        "User {} tried to reactivate extension '{}' that has failed scans or was blocked by an admin.",
                        user.getLoginName(),
                        NamingUtil.toFileFormat(version));
            }
        }
        for (var extension : affectedExtensions) {
            updateExtension(extension);
        }
    }

    private boolean canBeReactivated(ExtensionVersion extVersion) {
        // A soft-deleted version is a permanent tombstone and must never be reactivated.
        if (extVersion.isRemoved()) {
            return false;
        }

        var scan = repositories.findLatestExtensionScan(extVersion);
        // if no scan could be found, scanning is disabled, so allow reactivation
        if (scan == null) {
            return true;
        }

        // check if the scan has passed
        if (ScanStatus.PASSED.equals(scan.getStatus())) {
            return true;
        }

        // if the extension was quarantined before, check if there is an admin decision
        if (ScanStatus.QUARANTINED.equals(scan.getStatus())) {
            var scanDecision = repositories.findAdminScanDecision(scan);
            return scanDecision != null && scanDecision.isAllowed();
        }

        // otherwise do not allow reactivation
        return false;
    }

    /**
     * Soft-deletes the given extension versions.
     * <p>
     * The extension will be locked for the operation. If the lock can not be acquired, i.e. the extension
     * is updated at the same time, the operation will fail.
     * <p>
     * If {@code restrictedToUser} is {@code true}, the deletion operation is only successful if the user
     * has published the respective extension version.
     * <p>
     * The versions to delete must be named explicitly; an empty {@code targetVersions} deletes nothing.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(
            UserData user,
            boolean restrictedToUser,
            String namespaceName,
            String extensionName,
            TargetPlatformVersion... targetVersions
    ) throws ErrorResultException {
        var extension = lockExtensionNoWait(namespaceName, extensionName);
        var uniqueVersions = distinctVersions(targetVersions);
        var versions = resolveVersions(user, restrictedToUser, namespaceName, extensionName, uniqueVersions);

        // if all active versions of the extension would get deactivated, check for dependencies
        if (extension.isActive() &&
                repositories.isDeleteAllActiveVersions(namespaceName, extensionName, uniqueVersions)) {
            checkNoDependencies(extension);
        }

        return deleteExtensionVersions(user, versions);
    }

    /**
     * Purges (permanently deletes) the given extension versions from the database and storage.
     * <p>
     * Unlike {@link #deleteExtension(UserData, boolean, String, String, TargetPlatformVersion...)}, which
     * soft-deletes versions (keeping the row so the version identity stays reserved), this physically removes
     * the rows and frees the version identity for republishing. It is intended for administrative purge and
     * automated cleanup (mirror, extension control) and performs no user-ownership check.
     * <p>
     * The versions to purge must be named explicitly; an empty {@code targetVersions} purges nothing. Use
     * {@link #purgeExtension(UserData, Extension, boolean)} to purge an extension as a whole.
     * <p>
     * The method will try to lock the extension and fail with an {@code ErrorResultException} if it can't acquire it.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson purgeExtensionNoWait(
            UserData user,
            String namespaceName,
            String extensionName,
            TargetPlatformVersion... targetVersions
    ) throws ErrorResultException {
        var extension = lockExtensionNoWait(namespaceName, extensionName);
        var uniqueVersions = distinctVersions(targetVersions);
        var versions = resolveVersions(user, false, namespaceName, extensionName, uniqueVersions);

        // if every version of the extension is purged, purge the extension as a whole so that its
        // record, reviews and search entry are removed too and nothing is left orphaned
        if (!versions.isEmpty() && versions.size() == repositories.countVersions(namespaceName, extensionName)) {
            return purgeExtension(user, extension, extension.isActive());
        }

        // if all active versions of the extension would get deactivated, check for dependencies
        if (extension.isActive() &&
                repositories.isDeleteAllActiveVersions(namespaceName, extensionName, uniqueVersions)) {
            checkNoDependencies(extension);
        }

        return purgeExtensionVersions(user, versions);
    }

    /**
     * Returns the given target versions with duplicates removed, so that callers can reason about the
     * exact set of versions to delete/purge without a repeated entry inflating any count-based check.
     */
    private static TargetPlatformVersion[] distinctVersions(TargetPlatformVersion... targetVersions) {
        return Arrays.stream(targetVersions).distinct().toArray(TargetPlatformVersion[]::new);
    }

    private List<ExtensionVersion> resolveVersions(
            UserData user,
            boolean restrictedToUser,
            String namespaceName,
            String extensionName,
            TargetPlatformVersion... targetVersions
    ) {
        var versions = Arrays.stream(targetVersions)
                .map(target -> {
                    var extVersion = restrictedToUser
                            ? repositories.findVersionPublishedByUser(
                                    user,
                                    target.version(),
                                    target.targetPlatform(),
                                    extensionName,
                                    namespaceName)
                            : repositories.findVersion(
                                    target.version(),
                                    target.targetPlatform(),
                                    extensionName,
                                    namespaceName);

                    if (extVersion == null) {
                        throw new ErrorResultException(
                                "Extension not found: " + NamingUtil.toLogFormat(
                                        namespaceName,
                                        extensionName,
                                        target.targetPlatform(),
                                        target.version()),
                                HttpStatus.NOT_FOUND);
                    }
                    return extVersion;
                })
                .toList();

        // Guard against a mismatch between the requested versions and what was actually resolved: the
        // resolved versions must correspond exactly to the requested (target platform, version) pairs.
        // Otherwise, the count-based "delete/purge all versions" checks could be driven to a wrong
        // conclusion (e.g. by duplicate or unexpectedly-resolving entries).
        var requested = Arrays.stream(targetVersions).collect(Collectors.toSet());
        var resolved = versions.stream()
                .map(v -> new TargetPlatformVersion(v.getTargetPlatform(), v.getVersion()))
                .collect(Collectors.toSet());
        if (!resolved.equals(requested)) {
            throw new ErrorResultException(
                    "The requested versions of " + NamingUtil.toExtensionId(namespaceName, extensionName)
                            + " could not be resolved.",
                    HttpStatus.BAD_REQUEST);
        }

        return versions;
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
     * Purges (permanently deletes) the given pre-resolved extension versions without any ownership check.
     * Callers are responsible for authorisation.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson purgeExtensionVersions(UserData user, List<ExtensionVersion> versions) {
        var results = new ArrayList<ResultJson>();
        for (var version : versions) {
            results.add(purgeExtensionVersion(user, version));
        }
        return combineResults(results);
    }

    /**
     * Locks and return the {@code Extension} identified by {@code namespaceName} and {@code extensionName}.
     *
     * @throws ErrorResultException if no extension exists with the given namespace and extension name
     */
    public @NonNull Extension lockExtension(String namespaceName, String extensionName) throws ErrorResultException {
        var extension = repositories.findExtensionForUpdate(extensionName, namespaceName);
        if (extension == null) {
            var extensionId = NamingUtil.toExtensionId(namespaceName, extensionName);
            throw new ErrorResultException("Extension not found: " + extensionId, HttpStatus.NOT_FOUND);
        }
        return extension;
    }

    /**
     * Locks the extension row ({@code SELECT … FOR UPDATE NOWAIT}) without waiting.
     * <p>
     * If the lock can not be acquired, throw an {@code ErrorResultException} with status code {@code 409}.
     */
    private Extension lockExtensionNoWait(String namespaceName, String extensionName) throws ErrorResultException {
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
        result.setError(
                results.stream().map(ResultJson::getError).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        result.setSuccess(
                results.stream().map(ResultJson::getSuccess).filter(Objects::nonNull)
                        .collect(Collectors.joining("\n")));
        return result;
    }

    /**
     * Soft-delete a single extension version: strip its files from storage and mark it as removed,
     * but keep the row so the version identity stays reserved. Does not touch the parent extension;
     * callers are responsible for calling {@link #updateExtension(Extension)} afterwards.
     */
    private void softDeleteExtensionVersion(UserData user, ExtensionVersion extVersion) {
        deleteFiles(extVersion);
        // Read before the version is marked as removed below, and only reported for a version the feed
        // announced as available -- deleting one it never reported has nothing to withdraw, exactly as
        // purging one does not, see recordPurge.
        var reported = wasReportedAsAvailable(extVersion);
        var now = TimeUtil.getCurrentUTC();
        extVersion.setActive(false);
        extVersion.setRemoved(true);
        extVersion.setRemovedTimestamp(now);
        extVersion.setRemovedBy(user);
        if (reported) {
            repositories.recordExtensionVersionChange(extVersion, ExtensionVersionState.REMOVED, now);
        }
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtensionVersion(UserData user, ExtensionVersion extVersion) {
        if (extVersion.isRemoved()) {
            return ResultJson.success("Already removed " + NamingUtil.toLogFormat(extVersion));
        }

        var extension = extVersion.getExtension();
        softDeleteExtensionVersion(user, extVersion);
        updateExtension(extension);

        var result = ResultJson.success("Deleted " + NamingUtil.toLogFormat(extVersion));
        logs.logAction(user, result);
        return result;
    }

    /**
     * Purge (permanently delete) the given extension and evict caches.
     * <p>
     * This physically removes the extension and all its versions from the database and storage, freeing
     * the extension and version identities for republishing. Intended for administrative purge and automated
     * cleanup (mirror, extension control).
     * <p>
     * If {@code checkDependencies} is {@code true} and another extension declares a dependency on this
     * extension, the operation will fail. Extension packs that bundle this extension are deliberately
     * <em>not</em> checked: bundles are not validated on publication either and IDEs skip bundled
     * extensions that do not exist (see #1956).
     * <p>
     * The given extension must be managed by the current transaction, e.g. locked via
     * {@link #lockExtension(String, String)}. Use {@link #purgeExtension(UserData, String, String)} if only
     * the extension coordinates are at hand.
     *
     * @param user the user that will be used for logging the operation
     * @param extension the extension to purge
     * @param checkDependencies whether to check if this extension is still referenced as a dependency
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson purgeExtension(UserData user, Extension extension, boolean checkDependencies)
            throws ErrorResultException {
        if (checkDependencies) {
            checkNoDependencies(extension);
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

        var result = ResultJson.success("Purged " + NamingUtil.toExtensionId(extension));
        logs.logAction(user, result);
        return result;
    }

    /**
     * Purge (permanently delete) the given extension and evict caches. The extension is locked for the
     * operation, waiting until the lock can be acquired. No dependency check is performed.
     *
     * @throws ErrorResultException if no extension exists with the given namespace and extension name
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson purgeExtension(UserData user, String namespaceName, String extensionName)
            throws ErrorResultException {
        return purgeExtension(user, lockExtension(namespaceName, extensionName), false);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson purgeExtensionVersion(UserData user, ExtensionVersion extVersion) {
        // Callers may hand us a version that was loaded in another transaction (background jobs collect
        // versions before purging them), which would be detached here. Re-read it so that removing it and
        // updating the parent extension below both operate on managed entities.
        var managedVersion = entityManager.find(ExtensionVersion.class, extVersion.getId());
        if (managedVersion == null) {
            return ResultJson.success("Already purged " + NamingUtil.toLogFormat(extVersion));
        }

        var extension = managedVersion.getExtension();
        removeExtensionVersion(managedVersion);
        extension.getVersions().remove(managedVersion);
        updateExtension(extension);

        var result = ResultJson.success("Purged " + NamingUtil.toLogFormat(managedVersion));
        logs.logAction(user, result);
        return result;
    }

    private void checkNoDependencies(Extension extension) throws ErrorResultException {
        var dependRefs = repositories.findDependenciesReference(extension);
        if (!dependRefs.isEmpty()) {
            throw new ErrorResultException(
                    "The following extensions have a dependency on " + NamingUtil.toExtensionId(extension) + ": "
                            + dependRefs.stream()
                                    .map(NamingUtil::toFileFormat)
                                    .collect(Collectors.joining(", ")));
        }
    }

    private void deleteFiles(ExtensionVersion extVersion) {
        // Clean up any pending scan jobs for this extension version
        // to prevent "file not found" errors after deletion
        scanPersistenceService.deleteScansForExtensionVersion(extVersion.getId());

        repositories.findFiles(extVersion).map(RemoveFileJobRequest::new).forEach(scheduler::enqueue);
        repositories.deleteFiles(extVersion);
    }

    /**
     * Physically remove an extension version: strip its files from storage and delete the row.
     * This is the low-level hard-delete primitive used by the purge paths.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public void removeExtensionVersion(ExtensionVersion extVersion) {
        deleteFiles(extVersion);
        recordPurge(extVersion);
        entityManager.remove(extVersion);
    }

    /**
     * Report a purged version as removed, so that consumers following the changes feed learn that it is
     * gone instead of it silently vanishing from the registry.
     * <p>
     * A purge is reported no differently from a deletion: either way the version is no longer available
     * for download, and whether the registry keeps a tombstone for it is not something a consumer of the
     * feed can act on.
     * <p>
     * Has to run before the row is deleted, both to copy the version's coordinates onto the entry and to
     * read the transition last reported for it. Neither the entry it appends nor the ones already there are
     * left referencing the version this transaction goes on to remove -- see
     * {@link RepositoryService#recordPurgedExtensionVersionChange} and
     * {@link RepositoryService#detachExtensionVersionChanges}.
     */
    private void recordPurge(ExtensionVersion extVersion) {
        if (wasReportedAsAvailable(extVersion)) {
            repositories.recordPurgedExtensionVersionChange(
                    extVersion,
                    ExtensionVersionState.REMOVED,
                    TimeUtil.getCurrentUTC());
        }

        // Unconditional, and in particular also on the paths that append nothing above: whatever the log
        // already holds for this version has to stop pointing at it before it is deleted.
        repositories.detachExtensionVersionChanges(extVersion);
    }

    /**
     * Whether the changes feed last reported this version as being available, and so has a transition to
     * withdraw once the version goes away.
     * <p>
     * False for a version the feed never reported -- one whose publication never made it public, for
     * instance because a scan quarantined it, or one that predates the feed and was already hidden when
     * it was seeded. Reporting its removal would withdraw a publication that consumers were never told
     * about, which is the one thing the append-only log is not allowed to say.
     * <p>
     * False as well once the feed has reported the version as gone: a version is deleted before it can be
     * purged, and the purge only drops the tombstone the deletion kept, which is invisible from the
     * outside, so a second entry would report a transition that never happened.
     */
    private boolean wasReportedAsAvailable(ExtensionVersion extVersion) {
        return repositories.findLatestExtensionVersionChange(extVersion)
                .map(latest -> latest.getState() != ExtensionVersionState.REMOVED)
                .orElse(false);
    }
}
