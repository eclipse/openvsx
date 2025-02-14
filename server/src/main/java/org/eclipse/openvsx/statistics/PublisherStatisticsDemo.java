/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
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
import org.eclipse.openvsx.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class PublisherStatisticsDemo {

    protected final Logger logger = LoggerFactory.getLogger(PublisherStatisticsDemo.class);

    private final RepositoryService repositories;
    private final StatisticsService service;

    public PublisherStatisticsDemo(RepositoryService repositories, StatisticsService service) {
        this.repositories = repositories;
        this.service = service;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        var loginName = "amvanbaren";
        var user = repositories.findUserByLoginName("github", loginName);
        if(user == null || !repositories.findPublisherStatisticsByUser(user).isEmpty()) {
            return;
        }

        var userDownloads = repositories.findMembershipDownloads(loginName);
        userDownloads.forEach(i -> logger.info("{}.{}: {}", i.namespace(), i.extension(), i.downloadCount()));
        var totals = userDownloads.stream()
                .map((i) -> Map.entry(NamingUtil.toExtensionId(i.namespace(), i.extension()), 0L))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, HashMap::new));

        var random = new Random();
        var now = TimeUtil.getCurrentUTC();
        for(var i = 6; i > 0; i--) {
            var date = now.minusMonths(i);
            var downloads = new HashMap<String, Long>();
            var totalDownloads = new HashMap<String, Long>();
            totals.keySet().forEach((key) -> {
                var value = random.nextLong(1000L);
                downloads.put(key, value);
                var total = totals.get(key);
                totalDownloads.put(key, total + value);
                totals.put(key, total + value);
            });

            var statistics = new PublisherStatistics();
            statistics.setYear(date.getYear());
            statistics.setMonth(date.getMonthValue());
            statistics.setUser(user);
            statistics.setExtensionDownloads(downloads);
            statistics.setExtensionTotalDownloads(totalDownloads);
            service.savePublisherStatistics(statistics);
        }
    }
}
