/********************************************************************************
 * Copyright (c) 2020-2021 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.openvsx.search;

import java.time.Duration;
import java.time.LocalDateTime;
import javax.annotation.PostConstruct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * Provides relevance for a given extension
 */
@Component
public class RelevanceService {

    protected final Logger logger = LoggerFactory.getLogger(RelevanceService.class);

    @Value("${ovsx.search.relevance.rating:1.0}")
    double ratingRelevance;
    @Value("${ovsx.search.relevance.downloads:1.0}")
    double downloadsRelevance;
    @Value("${ovsx.search.relevance.timestamp:1.0}")
    double timestampRelevance;
    @Value("${ovsx.search.relevance.unverified:0.5}")
    double unverifiedRelevance;

    @Value("${ovsx.elasticsearch.relevance.rating:-1.0}")
    double deprecatedElasticSearchRatingRelevance;
    @Value("${ovsx.elasticsearch.relevance.downloads:-1.0}")
    double deprecatedElasticSearchDownloadsRelevance;
    @Value("${ovsx.elasticsearch.relevance.timestamp:-1.0}")
    double deprecatedElasticSearchTimestampRelevance;
    @Value("${ovsx.elasticsearch.relevance.unverified:-1.0}")
    double deprecatedElasticSearchUnverifiedRelevance;

    @Autowired
    RepositoryService repositories;

    @PostConstruct
    void init() {
        if (deprecatedElasticSearchRatingRelevance != -1.0) {
            logger.warn("Using deprecated ovsx.elasticsearch.relevance.rating property. It has been renamed to ovsx.search.relevance.rating.");
            this.ratingRelevance = deprecatedElasticSearchRatingRelevance;
        }
        if (deprecatedElasticSearchDownloadsRelevance != -1.0) {
            logger.warn("Using deprecated ovsx.elasticsearch.relevance.downloads property. It has been renamed to ovsx.search.relevance.rating.");
            this.downloadsRelevance = deprecatedElasticSearchDownloadsRelevance;
        }
        if (deprecatedElasticSearchTimestampRelevance != -1.0) {
            logger.warn("Using deprecated ovsx.elasticsearch.relevance.timestamp property. It has been renamed to ovsx.search.relevance.rating.");
            this.timestampRelevance = deprecatedElasticSearchTimestampRelevance;
        }
        if (deprecatedElasticSearchUnverifiedRelevance != -1.0) {
            logger.warn("Using deprecated ovsx.elasticsearch.relevance.unverified property. It has been renamed to ovsx.search.relevance.rating.");
            this.unverifiedRelevance = deprecatedElasticSearchUnverifiedRelevance;
        }
    }

    protected ExtensionSearch toSearchEntry(Extension extension, SearchStats stats) {
        var entry = extension.toSearch();
        var ratingValue = 0.0;
        if (entry.averageRating != null) {
            var reviewCount = repositories.countActiveReviews(extension);
            // Reduce the rating relevance if there are only few reviews
            var countRelevance = saturate(reviewCount, 0.25);
            ratingValue = (entry.averageRating / 5.0) * countRelevance;
        }
        var downloadsValue = entry.downloadCount / stats.downloadRef;
        var timestamp = extension.getLatest().getTimestamp();
        var timestampValue = Duration.between(stats.oldest, timestamp).toSeconds() / stats.timestampRef;
        entry.relevance = ratingRelevance * limit(ratingValue) + downloadsRelevance * limit(downloadsValue)
                + timestampRelevance * limit(timestampValue);

        // Reduce the relevance value of unverified extensions
        if (!isVerified(extension.getLatest())) {
            entry.relevance *= unverifiedRelevance;
        }

        if (Double.isNaN(entry.relevance) || Double.isInfinite(entry.relevance)) {
            var message = "Invalid relevance for entry " + entry.namespace + "." + entry.name;
            try {
                message += " " + new ObjectMapper().writeValueAsString(stats);
            } catch (JsonProcessingException exc) {
                // Ignore exception
            }
            logger.error(message);
            entry.relevance = 0.0;
        }
        return entry;
    }

    private double limit(double value) {
        if (value < 0.0)
            return 0.0;
        else if (value > 1.0)
            return 1.0;
        else
            return value;
    }

    private double saturate(double value, double factor) {
        return 1 - 1.0 / (value * factor + 1);
    }

    private boolean isVerified(ExtensionVersion extVersion) {
        if (extVersion.getPublishedWith() == null)
            return false;
        var user = extVersion.getPublishedWith().getUser();
        var namespace = extVersion.getExtension().getNamespace();
        return repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER) > 0
                && repositories.countMemberships(user, namespace) > 0;
    }

    public static class SearchStats {
        protected final double downloadRef;
        protected final double timestampRef;
        protected final LocalDateTime oldest;

        public SearchStats(RepositoryService repositories) {
            var now = TimeUtil.getCurrentUTC();
            var maxDownloads = repositories.getMaxExtensionDownloadCount();
            var oldestTimestamp = repositories.getOldestExtensionTimestamp();
            this.downloadRef = maxDownloads * 1.5 + 100;
            this.oldest = oldestTimestamp == null ? now : oldestTimestamp;
            this.timestampRef = Duration.between(this.oldest, now).toSeconds() + 60;
        }
    }
}
