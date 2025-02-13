/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.migration;

import org.eclipse.openvsx.entities.MigrationItem;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class MigrationRunner implements JobRequestHandler<HandlerJobRequest<?>> {

    private final OrphanNamespaceMigration orphanNamespaceMigration;
    private final RepositoryService repositories;
    private final MigrationService migrations;
    private final JobRequestScheduler scheduler;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorEnabled;

    public MigrationRunner(
            OrphanNamespaceMigration orphanNamespaceMigration,
            RepositoryService repositories,
            MigrationService migrations,
            JobRequestScheduler scheduler
    ) {
        this.orphanNamespaceMigration = orphanNamespaceMigration;
        this.repositories = repositories;
        this.migrations = migrations;
        this.scheduler = scheduler;
    }

    @Override
    @Job(name = "Run migrations", retries = 0)
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        orphanNamespaceMigration.fixOrphanNamespaces();
        setPreReleaseMigration();
        renameDownloadsMigration();
        extractVsixManifestMigration();
        fixTargetPlatformMigration();
        generateSha256ChecksumMigration();
        extensionVersionSignatureMigration();
        checkPotentiallyMaliciousExtensionVersions();
        migrateLocalNamespaceLogos();
        migrateLocalFileResourceContent();
        removeFileResourceTypeResource();
    }

    private void setPreReleaseMigration() {
        var jobName = "SetPreReleaseMigration";
        var handler = SetPreReleaseJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedPreReleases);
    }

    private void renameDownloadsMigration() {
        var jobName = "RenameDownloadsMigration";
        var handler = RenameDownloadsJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedRenamedDownloads);
    }

    private void extractVsixManifestMigration() {
        var jobName = "ExtractVsixManifestMigration";
        var handler = ExtractVsixManifestsJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedVsixManifests);
    }

    private void fixTargetPlatformMigration() {
        var jobName = "FixTargetPlatformMigration";
        var handler = FixTargetPlatformsJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedTargetPlatforms);
    }

    private void generateSha256ChecksumMigration() {
        var jobName = "GenerateSha256ChecksumMigration";
        var handler = GenerateSha256ChecksumJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedSha256Checksums);
    }

    private void extensionVersionSignatureMigration() {
        if(!mirrorEnabled) {
            scheduler.enqueue(new HandlerJobRequest<>(GenerateKeyPairJobRequestHandler.class));
        }
    }

    private void checkPotentiallyMaliciousExtensionVersions() {
        var jobName = "CheckPotentiallyMaliciousExtensionVersions";
        var handler = PotentiallyMaliciousJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedPotentiallyMalicious);
    }

    private void migrateLocalNamespaceLogos() {
        var jobName = "LocalNamespaceLogoMigration";
        var handler = NamespaceLogoFileResourceJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedLocalNamespaceLogos);
    }

    private void migrateLocalFileResourceContent() {
        var jobName = "LocalFileResourceContentMigration";
        var handler = FileResourceContentJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedLocalFileResourceContent);
    }

    private void removeFileResourceTypeResource() {
        var jobName = "RemoveFileResourceTypeResourceMigration";
        var handler = RemoveFileResourceTypeResourceJobRequestHandler.class;
        scheduleMigrations(jobName, handler, repositories::findNotMigratedFileResourceTypeResource);
    }

    private void scheduleMigrations(String jobName, Class<? extends JobRequestHandler<MigrationJobRequest>> handler, Function<PageRequest, Slice<MigrationItem>> query) {
        var pageRequest = PageRequest.ofSize(10000);
        var next = true;
        while(next) {
            var items = query.apply(pageRequest);
            items.forEach(item -> migrations.enqueueMigration(jobName, handler, item));
            next = items.hasNext();
            pageRequest = pageRequest.next();
        }
    }
}
