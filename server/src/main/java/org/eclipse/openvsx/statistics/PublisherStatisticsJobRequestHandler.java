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

import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.entities.PublisherStatistics;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PublisherStatisticsJobRequestHandler implements JobRequestHandler<StatisticsJobRequest> {

    private final RepositoryService repositories;
    private final EntityManager entityManager;
    private final StatisticsService service;

    public PublisherStatisticsJobRequestHandler(RepositoryService repositories, EntityManager entityManager, StatisticsService service) {
        this.repositories = repositories;
        this.entityManager = entityManager;
        this.service = service;
    }

    @Override
    public void run(StatisticsJobRequest jobRequest) throws Exception {
        var year = jobRequest.getYear();
        var month = jobRequest.getMonth();
        var prevDate = LocalDateTime.of(year, month, 1, 0, 0).minusMonths(1);

        var offset = 0;
        var limit = 10000;
        var userId = -1L;
        var userDownloads = new ArrayList<MembershipDownloadCount>();
        var membershipDownloads = Collections.<MembershipDownloadCount>emptyList();
        do {
            for(var membershipDownload : membershipDownloads) {
                if(membershipDownload.userId() != userId) {
                    if(userId != -1L) {
                        var totalDownloads = userDownloads.stream()
                                .map(i -> Map.entry(NamingUtil.toExtensionId(i.namespace(), i.extension()), i.downloadCount()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        var prevStatistics = repositories.findPublisherStatisticsByYearAndMonthAndUserId(prevDate.getYear(), prevDate.getMonthValue(), userId);
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
                        statistics.setUser(entityManager.find(UserData.class, userId));
                        statistics.setExtensionDownloads(downloads);
                        statistics.setExtensionTotalDownloads(totalDownloads);
                        service.savePublisherStatistics(statistics);
                    }

                    userId = membershipDownload.userId();
                    userDownloads.clear();
                }

                userDownloads.add(membershipDownload);
            }

            membershipDownloads = repositories.findMembershipDownloads(offset, limit);
            offset += limit;
        } while(!membershipDownloads.isEmpty());
    }
}