/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.migration;

import java.util.List;
import java.util.Optional;

import org.jobrunr.server.BackgroundJobServer;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registers {@link MigrationItemCleanupFilter} with JobRunr's {@link BackgroundJobServer}.
 * <p>
 * JobRunr's Spring Boot starter ({@code JobRunrAutoConfiguration}) does not pick up
 * {@link org.jobrunr.jobs.filters.JobFilter} beans automatically -- confirmed by inspecting its
 * bean definitions, none of which take a {@code List<JobFilter>} or similar. Registration has to
 * be done explicitly against the server's {@code JobDefaultFilters}, and via
 * {@code addAll(...)} rather than {@code setJobFilters(...)} so JobRunr's own built-in filters
 * (e.g. {@code RetryFilter}) are appended to, not replaced.
 * <p>
 * {@link BackgroundJobServer} is injected as {@link Optional} because it's only present when
 * {@code jobrunr.background-job-server.enabled=true} (e.g. it's absent on nodes that only
 * schedule jobs, not process them); there's nothing to register the filter on in that case.
 */
@Component
public class MigrationItemCleanupFilterRegistrar {

    private final Optional<BackgroundJobServer> backgroundJobServer;
    private final MigrationItemCleanupFilter migrationItemCleanupFilter;

    public MigrationItemCleanupFilterRegistrar(
            Optional<BackgroundJobServer> backgroundJobServer,
            MigrationItemCleanupFilter migrationItemCleanupFilter
    ) {
        this.backgroundJobServer = backgroundJobServer;
        this.migrationItemCleanupFilter = migrationItemCleanupFilter;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        backgroundJobServer.ifPresent(server -> server.getJobFilters().addAll(List.of(migrationItemCleanupFilter)));
    }
}
