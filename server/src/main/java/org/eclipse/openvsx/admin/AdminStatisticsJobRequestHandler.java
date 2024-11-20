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
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AdminStatisticsJobRequestHandler implements JobRequestHandler<AdminStatisticsJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminStatisticsJobRequestHandler.class);

    private final RepositoryService repositories;
    private final AdminStatisticsService service;

    public AdminStatisticsJobRequestHandler(RepositoryService repositories, AdminStatisticsService service) {
        this.repositories = repositories;
        this.service = service;
    }

    @Override
    public void run(AdminStatisticsJobRequest jobRequest) throws Exception {
        var year = jobRequest.getYear();
        var month = jobRequest.getMonth();

        var extensions = repositories.countActiveExtensions();
        var downloadsTotal = repositories.downloadsTotal();

        var lastDate = LocalDateTime.of(year, month, 1, 0, 0).minusMonths(1);
        var lastAdminStatistics = repositories.findAdminStatisticsByYearAndMonth(lastDate.getYear(), lastDate.getMonthValue());
        var lastDownloadsTotal = lastAdminStatistics != null ? lastAdminStatistics.getDownloadsTotal() : 0;
        var downloads = downloadsTotal - lastDownloadsTotal;
        var publishers = repositories.countActiveExtensionPublishers();
        var averageReviewsPerExtension = repositories.averageNumberOfActiveReviewsPerActiveExtension();
        var namespaceOwners = repositories.countPublishersThatClaimedNamespaceOwnership();
        var extensionsByRating = repositories.countActiveExtensionsGroupedByExtensionReviewRating();
        var publishersByExtensionsPublished = repositories.countActiveExtensionPublishersGroupedByExtensionsPublished();

        var limit = 10;
        var topMostActivePublishingUsers = repositories.topMostActivePublishingUsers(limit);
        var topNamespaceExtensions = repositories.topNamespaceExtensions(limit);
        var topNamespaceExtensionVersions = repositories.topNamespaceExtensionVersions(limit);
        var topMostDownloadedExtensions = repositories.topMostDownloadedExtensions(limit);

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