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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

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
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    VersionService versions;

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

    @Transactional
    public ExtensionSearch toSearchEntryTrxn(Extension extension, SearchStats stats) {
        extension = entityManager.merge(extension);
        return toSearchEntry(extension, stats);
    }

    public ExtensionSearch toSearchEntry(Extension extension, SearchStats stats) {
        var latest = versions.getLatest(extension, null, false, true);
        var entry = extension.toSearch(latest);
        entry.rating = calculateRating(extension, stats);
        entry.relevance = calculateRelevance(extension, latest, stats, entry);

        return entry;
    }

    private double calculateRating(Extension extension, SearchStats stats) {
        // IMDB rating formula, source: https://stackoverflow.com/a/1411268
        var padding = 100;
        var reviews = Optional.ofNullable(extension.getReviewCount()).orElse(0L);
        var averageRating = Optional.ofNullable(extension.getAverageRating()).orElse(0.0);
        // The amount of "smoothing" applied to the rating is based on reviews in relation to padding.
        return (averageRating * reviews + stats.averageReviewRating * padding) / (reviews + padding);
    }

    private double calculateRelevance(Extension extension, ExtensionVersion latest, SearchStats stats, ExtensionSearch entry) {
        var ratingValue = 0.0;
        if (extension.getAverageRating() != null) {
            var reviewCount = extension.getReviewCount();
            // Reduce the rating relevance if there are only few reviews
            var countRelevance = saturate(reviewCount, 0.25);
            ratingValue = (extension.getAverageRating() / 5.0) * countRelevance;
        }
        var downloadsValue = entry.downloadCount / stats.downloadRef;
        var timestamp = latest.getTimestamp();
        var timestampValue = Duration.between(stats.oldest, timestamp).toSeconds() / stats.timestampRef;
        var relevance = ratingRelevance * limit(ratingValue) + downloadsRelevance * limit(downloadsValue)
                + timestampRelevance * limit(timestampValue);

        // Reduce the relevance value of unverified extensions
        if (!isVerified(latest)) {
            relevance *= unverifiedRelevance;
        }

        if (Double.isNaN(entry.relevance) || Double.isInfinite(entry.relevance)) {
            var message = "Invalid relevance for entry " + NamingUtil.toExtensionId(entry);
            try {
                message += " " + new ObjectMapper().writeValueAsString(stats);
            } catch (JsonProcessingException exc) {
                // Ignore exception
            }
            logger.error(message);
            relevance = 0.0;
        }

        return relevance;
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
        return repositories.isVerified(namespace, user);
    }

    public static class SearchStats {
        protected final double downloadRef;
        protected final double timestampRef;
        protected final LocalDateTime oldest;
        protected final double averageReviewRating;

        public SearchStats(RepositoryService repositories) {
            var now = TimeUtil.getCurrentUTC();
            var maxDownloads = repositories.getMaxExtensionDownloadCount();
            var oldestTimestamp = repositories.getOldestExtensionTimestamp();
            this.downloadRef = maxDownloads * 1.5 + 100;
            this.oldest = oldestTimestamp == null ? now : oldestTimestamp;
            this.timestampRef = Duration.between(this.oldest, now).toSeconds() + 60;
            this.averageReviewRating = repositories.getAverageReviewRating();
        }
    }
}
