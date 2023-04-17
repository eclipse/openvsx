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

import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MigrationRunner implements JobRequestHandler<HandlerJobRequest<?>> {

    @Autowired
    OrphanNamespaceMigration orphanNamespaceMigration;

    @Autowired
    RepositoryService repositories;

    @Autowired
    MigrationService migrations;

    @Autowired
    JobRequestScheduler scheduler;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean mirrorEnabled;

    @Override
    @Job(name = "Run migrations", retries = 0)
    public void run(HandlerJobRequest<?> jobRequest) throws Exception {
        orphanNamespaceMigration.fixOrphanNamespaces();
        extractResourcesMigration();
        setPreReleaseMigration();
        renameDownloadsMigration();
        extractVsixManifestMigration();
        fixTargetPlatformMigration();
        generateSha256ChecksumMigration();
        extensionVersionSignatureMigration();
    }

    private void extractResourcesMigration() {
        var jobName = "ExtractResourcesMigration";
        var handler = ExtractResourcesJobRequestHandler.class;
        repositories.findNotMigratedResources().forEach(item -> migrations.enqueueMigration(jobName, handler, item));
    }

    private void setPreReleaseMigration() {
        var jobName = "SetPreReleaseMigration";
        var handler = SetPreReleaseJobRequestHandler.class;
        repositories.findNotMigratedPreReleases().forEach(item -> migrations.enqueueMigration(jobName, handler, item));
    }

    private void renameDownloadsMigration() {
        var jobName = "RenameDownloadsMigration";
        var handler = RenameDownloadsJobRequestHandler.class;
        repositories.findNotMigratedRenamedDownloads().forEach(item -> migrations.enqueueMigration(jobName, handler, item));
    }

    private void extractVsixManifestMigration() {
        var jobName = "ExtractVsixManifestMigration";
        var handler = ExtractVsixManifestsJobRequestHandler.class;
        repositories.findNotMigratedVsixManifests().forEach(item -> migrations.enqueueMigration(jobName, handler, item));
    }

    private void fixTargetPlatformMigration() {
        var jobName = "FixTargetPlatformMigration";
        var handler = FixTargetPlatformsJobRequestHandler.class;
        repositories.findNotMigratedTargetPlatforms().forEach(item -> migrations.enqueueMigration(jobName, handler, item));
    }

    private void generateSha256ChecksumMigration() {
        var jobName = "GenerateSha256ChecksumMigration";
        var handler = GenerateSha256ChecksumJobRequestHandler.class;
        repositories.findNotMigratedSha256Checksums().forEach(item -> migrations.enqueueMigration(jobName, handler, item));
    }

    private void extensionVersionSignatureMigration() {
        if(!mirrorEnabled) {
            scheduler.enqueue(new HandlerJobRequest<>(GenerateKeyPairJobRequestHandler.class));
        }
    }
}
