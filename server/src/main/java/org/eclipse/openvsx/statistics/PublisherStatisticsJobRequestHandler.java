/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.statistics;

import org.eclipse.openvsx.entities.PublisherStatistics;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PublisherStatisticsJobRequestHandler implements JobRequestHandler<StatisticsJobRequest> {

    private final RepositoryService repositories;
    private final StatisticsService service;

    public PublisherStatisticsJobRequestHandler(RepositoryService repositories, StatisticsService service) {
        this.repositories = repositories;
        this.service = service;
    }

    @Override
    public void run(StatisticsJobRequest jobRequest) throws Exception {
        var year = jobRequest.getYear();
        var month = jobRequest.getMonth();
        var prevDate = LocalDateTime.of(year, month, 1, 0, 0).minusMonths(1);

        var users = repositories.findUsersByProvider("github");
        for(var user : users) {
            var extensions = repositories.findExtensions(user);
            var totalDownloads = extensions.stream()
                    .map(e -> Map.entry(NamingUtil.toExtensionId(e), (long) e.getDownloadCount()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            var prevStatistics = repositories.findPublisherStatisticsByYearAndMonthAndUser(prevDate.getYear(), prevDate.getMonthValue(), user);
            var prevTotalDownloads = prevStatistics != null ? prevStatistics.getExtensionTotalDownloads() : Collections.<String, Long>emptyMap();
            var downloads = totalDownloads.entrySet().stream()
                    .map(e -> {
                        var prevDownloads = prevTotalDownloads.getOrDefault(e.getKey(), 0L);
                        return Map.entry(e.getKey(), e.getValue() - prevDownloads);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            var statistics = new PublisherStatistics();
            statistics.setYear(year);
            statistics.setMonth(month);
            statistics.setExtensionDownloads(downloads);
            statistics.setExtensionTotalDownloads(totalDownloads);
            service.savePublisherStatistics(statistics);
        }
    }
}