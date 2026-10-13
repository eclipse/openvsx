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
package org.eclipse.openvsx.consistency;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.NamingUtil;

/**
 * Finds and repairs any {@link Extension} whose denormalized {@code averageRating} or
 * {@code reviewCount} differs from its active reviews. Nothing else detects such drift, since both
 * fields are only recomputed when a review is posted or deleted. See issue #2327.
 */
@Component
public class ReviewStatsCheck implements ConsistencyCheck {

    public static final String ID = "extension-review-stats";

    private static final double RATING_TOLERANCE = 0.001;

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final CacheService cache;
    private final SearchUtilService search;

    public ReviewStatsCheck(
            EntityManager entityManager,
            RepositoryService repositories,
            CacheService cache,
            SearchUtilService search
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.cache = cache;
        this.search = search;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Extension review statistics";
    }

    @Override
    public String getDescription() {
        return "Extensions whose stored average rating or review count differs from their active reviews.";
    }

    @Override
    @Transactional
    public List<ConsistencyFinding> check() {
        return repositories.findExtensionsWithInconsistentReviewStats().stream()
                .map(
                        extension -> new ConsistencyFinding(
                                extension.getId(),
                                NamingUtil.toExtensionId(extension),
                                "stored " + extension.getReviewCount() + " reviews with rating "
                                        + extension.getAverageRating() + ", which differs from its active reviews"))
                .toList();
    }

    @Override
    @Transactional
    public void fix(long entityId) {
        // Locked so a concurrent review post/delete can't interleave with the recompute below.
        var extension = entityManager.find(Extension.class, entityId, LockModeType.PESSIMISTIC_WRITE);
        if (extension == null) {
            return;
        }
        // The lock does not reload an entity already managed by the scan in this transaction.
        entityManager.refresh(extension);

        var count = repositories.countActiveReviews(extension);
        var rating = repositories.getAverageReviewRating(extension);
        var countOk = extension.getReviewCount() != null ? extension.getReviewCount() == count : count == 0;
        var ratingOk = Math.abs(
                (extension.getAverageRating() != null ? extension.getAverageRating() : 0)
                        - (rating != null ? rating : 0)) <= RATING_TOLERANCE;
        if (countOk && ratingOk) {
            return;
        }

        extension.setReviewCount(count);
        extension.setAverageRating(rating);
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        cache.evictLatestExtensionVersion(extension);
    }
}
