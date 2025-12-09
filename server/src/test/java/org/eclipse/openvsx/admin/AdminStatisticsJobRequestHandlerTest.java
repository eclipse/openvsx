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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

@ExtendWith(SpringExtension.class)
class AdminStatisticsJobRequestHandlerTest {

    @MockitoBean
    RepositoryService repositories;

    @MockitoBean
    AdminStatisticsService service;

    @Autowired
    AdminStatisticsJobRequestHandler handler;

    @Test
    void testAdminStatisticsJobRequestHandler() throws Exception {
        var expectedStatistics = mockAdminStatistics();

        var request = new AdminStatisticsJobRequest(2023, 11);
        handler.run(request);
        Mockito.verify(service).saveAdminStatistics(expectedStatistics);
    }

    @Test
    void testAdminStatisticsJobRequestHandlerWithPreviousStatistics() throws Exception {
        var expectedStatistics = mockAdminStatistics();
        expectedStatistics.setDownloads(678L);

        var prevStatistics = new AdminStatistics();
        prevStatistics.setDownloadsTotal(5000);
        Mockito.when(repositories.findAdminStatisticsByYearAndMonth(2023, 10)).thenReturn(prevStatistics);

        var request = new AdminStatisticsJobRequest(2023, 11);
        handler.run(request);
        Mockito.verify(service).saveAdminStatistics(expectedStatistics);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        AdminStatisticsJobRequestHandler adminStatisticsJobRequestHandler(
                RepositoryService repositories,
                AdminStatisticsService service
        ) {
            return new AdminStatisticsJobRequestHandler(repositories, service);
        }
    }

    private AdminStatistics mockAdminStatistics() {
        var year = 2023;
        var month = 11;
        var extensions = 1234L;
        var downloadsTotal = 5678L;
        var publishers = 579L;
        var averageReviewsPerExtension = 2.5;
        var namespaceOwners = 268L;
        var extensionsByRating = Map.of(
                1, 34,
                2, 100,
                3, 700,
                4, 150,
                5, 250
        );
        var publishersByExtensionsPublished = Map.of(
                1, 500,
                3, 70,
                10, 9
        );
        var topMostActivePublishingUsers = Map.of(
                "foo", 400,
                "bar", 150,
                "baz", 29
        );
        var topNamespaceExtensions = Map.of(
                "lorum", 800,
                "ipsum", 400,
                "dolar", 34
        );
        var topNamespaceExtensionVersions = Map.of(
                "lorum", 8000,
                "ipsum", 2000,
                "dolar", 68
        );
        var topMostDownloadedExtensions = Map.of(
                "lorum.alpha", 1200L,
                "ipsum.beta", 450L,
                "dolar.omega", 300L
        );

        var expectedStatistics = new AdminStatistics();
        expectedStatistics.setYear(year);
        expectedStatistics.setMonth(month);
        expectedStatistics.setExtensions(extensions);
        expectedStatistics.setDownloads(downloadsTotal);
        expectedStatistics.setDownloadsTotal(downloadsTotal);
        expectedStatistics.setPublishers(publishers);
        expectedStatistics.setAverageReviewsPerExtension(averageReviewsPerExtension);
        expectedStatistics.setNamespaceOwners(namespaceOwners);
        expectedStatistics.setExtensionsByRating(extensionsByRating);
        expectedStatistics.setPublishersByExtensionsPublished(publishersByExtensionsPublished);
        expectedStatistics.setTopMostActivePublishingUsers(topMostActivePublishingUsers);
        expectedStatistics.setTopNamespaceExtensions(topNamespaceExtensions);
        expectedStatistics.setTopNamespaceExtensionVersions(topNamespaceExtensionVersions);
        expectedStatistics.setTopMostDownloadedExtensions(topMostDownloadedExtensions);

        Mockito.when(repositories.countActiveExtensions()).thenReturn(extensions);
        Mockito.when(repositories.downloadsTotal()).thenReturn(downloadsTotal);
        Mockito.when(repositories.countActiveExtensionPublishers()).thenReturn(publishers);
        Mockito.when(repositories.averageNumberOfActiveReviewsPerActiveExtension()).thenReturn(averageReviewsPerExtension);
        Mockito.when(repositories.countPublishersThatClaimedNamespaceOwnership()).thenReturn(namespaceOwners);
        Mockito.when(repositories.countActiveExtensionsGroupedByExtensionReviewRating()).thenReturn(extensionsByRating);
        Mockito.when(repositories.countActiveExtensionPublishersGroupedByExtensionsPublished()).thenReturn(publishersByExtensionsPublished);
        var limit = 10;
        Mockito.when(repositories.topMostActivePublishingUsers(limit)).thenReturn(topMostActivePublishingUsers);
        Mockito.when(repositories.topNamespaceExtensions(limit)).thenReturn(topNamespaceExtensions);
        Mockito.when(repositories.topNamespaceExtensionVersions(limit)).thenReturn(topNamespaceExtensionVersions);
        Mockito.when(repositories.topMostDownloadedExtensions(limit)).thenReturn(topMostDownloadedExtensions);

        return expectedStatistics;
    }
}
