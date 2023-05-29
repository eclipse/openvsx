/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.admin;

import org.eclipse.openvsx.entities.AdminStatistics;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;

@Component
public class AdminStatisticsJobRequestHandler implements JobRequestHandler<AdminStatisticsJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminStatisticsJobRequestHandler.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    AdminStatisticsService service;

    @Override
    public void run(AdminStatisticsJobRequest jobRequest) throws Exception {
        var year = jobRequest.getYear();
        var month = jobRequest.getMonth();
        var startInclusive = LocalDateTime.of(year, month, 1, 0, 0);
        var endExclusive = startInclusive.plusMonths(1);

        LOGGER.info(">> ADMIN REPORT STATS {} {}", year, month);
        var stopwatch = new StopWatch();
        stopwatch.start("repositories.countActiveExtensions");
        var extensions = repositories.countActiveExtensions(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.downloadsBetween");
        var downloads = repositories.downloadsBetween(startInclusive, endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.downloadsUntil");
        var downloadsTotal = repositories.downloadsUntil(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.countActiveExtensionPublishers");
        var publishers = repositories.countActiveExtensionPublishers(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.averageNumberOfActiveReviewsPerActiveExtension");
        var averageReviewsPerExtension = repositories.averageNumberOfActiveReviewsPerActiveExtension(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.countPublishersThatClaimedNamespaceOwnership");
        var namespaceOwners = repositories.countPublishersThatClaimedNamespaceOwnership(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.countActiveExtensionsGroupedByExtensionReviewRating");
        var extensionsByRating = repositories.countActiveExtensionsGroupedByExtensionReviewRating(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.countActiveExtensionPublishersGroupedByExtensionsPublished");
        var publishersByExtensionsPublished = repositories.countActiveExtensionPublishersGroupedByExtensionsPublished(endExclusive);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        var limit = 10;

        stopwatch.start("repositories.topMostActivePublishingUsers");
        var topMostActivePublishingUsers = repositories.topMostActivePublishingUsers(endExclusive, limit);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.topNamespaceExtensions");
        var topNamespaceExtensions = repositories.topNamespaceExtensions(endExclusive, limit);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.topNamespaceExtensionVersions");
        var topNamespaceExtensionVersions = repositories.topNamespaceExtensionVersions(endExclusive, limit);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());

        stopwatch.start("repositories.topMostDownloadedExtensions");
        var topMostDownloadedExtensions = repositories.topMostDownloadedExtensions(endExclusive, limit);
        stopwatch.stop();
        LOGGER.info("{} took {} ms", stopwatch.getLastTaskName(), stopwatch.getLastTaskTimeMillis());
        LOGGER.info("<< ADMIN REPORT STATS {} {}", year, month);

        var statistics = new AdminStatistics();
        statistics.setYear(year);
        statistics.setMonth(month);
        statistics.setExtensions(extensions);
        statistics.setDownloads(downloads);
        statistics.setDownloadsTotal(downloadsTotal);
        statistics.setPublishers(publishers);
        statistics.setAverageReviewsPerExtension(averageReviewsPerExtension);
        statistics.setNamespaceOwners(namespaceOwners);
        statistics.setExtensionsByRating(extensionsByRating);
        statistics.setPublishersByExtensionsPublished(publishersByExtensionsPublished);
        statistics.setTopMostActivePublishingUsers(topMostActivePublishingUsers);
        statistics.setTopNamespaceExtensions(topNamespaceExtensions);
        statistics.setTopNamespaceExtensionVersions(topNamespaceExtensionVersions);
        statistics.setTopMostDownloadedExtensions(topMostDownloadedExtensions);
        service.saveAdminStatistics(statistics);
    }
}
