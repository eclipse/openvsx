/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.NamespaceMembership;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.SelectHavingStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.jooq.Tables.*;

@Component
public class AdminStatisticCalculationsRepository {

    private static final String ALIAS_LAST_CHANGE = "last_change";
    private static final String ENTITY_TYPE_EXTENSION = "extension";
    private static final String ENTITY_TYPE_EXTENSION_REVIEW = "extension_review";
    private static final String ENTITY_TYPE_EXTENSION_VERSION = "extension_version";

    @Autowired
    DSLContext dsl;

    public long downloadsSumByTimestampLessThan(LocalDateTime endExclusive) {
        var sum = DSL.coalesce(DSL.sum(DOWNLOAD.AMOUNT),new BigDecimal(0));
        return dsl.select(sum)
                .from(DOWNLOAD)
                .where(DOWNLOAD.TIMESTAMP.lessThan(endExclusive))
                .fetchOne(sum)
                .longValue();
    }

    public long downloadsSumByTimestampGreaterThanEqualAndTimestampLessThan(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        var sum = DSL.coalesce(DSL.sum(DOWNLOAD.AMOUNT),new BigDecimal(0));
        return dsl.select(sum)
                .from(DOWNLOAD)
                .where(DOWNLOAD.TIMESTAMP.greaterOrEqual(startInclusive))
                .and(DOWNLOAD.TIMESTAMP.lessThan(endExclusive))
                .fetchOne(sum)
                .longValue();
    }

    public int countActiveExtensions(LocalDateTime endExclusive) {
        var count = DSL.count(ENTITY_ACTIVE_STATE.ENTITY_ID);
        var lastChangedEntityStates = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION).asTable("m");
        return dsl.select(count)
                .from(ENTITY_ACTIVE_STATE)
                .join(lastChangedEntityStates)
                .on(lastChangedEntityStates.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(ENTITY_ACTIVE_STATE.ENTITY_ID).and(lastChangedEntityStates.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(ENTITY_ACTIVE_STATE.TIMESTAMP)))
                .where(ENTITY_ACTIVE_STATE.ACTIVE.eq(true)).fetchOne(count);
    }

    public int countActiveExtensionPublishers(LocalDateTime endExclusive) {
        var extensionState = ENTITY_ACTIVE_STATE.as("es");
        var lastChangedExtensionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION).asTable("ae");
        var extensionVersionState = ENTITY_ACTIVE_STATE.as("evs");
        var lastChangedExtensionVersionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION_VERSION).asTable("aev");
        var publishers = DSL.countDistinct(PERSONAL_ACCESS_TOKEN.USER_DATA);
        return dsl.select(publishers)
                .from(EXTENSION)
                .join(extensionState).on(extensionState.ENTITY_ID.eq(EXTENSION.ID).and(extensionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION)))
                .join(lastChangedExtensionState).on(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionState.ENTITY_ID).and(lastChangedExtensionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionState.TIMESTAMP)))
                .join(EXTENSION_VERSION).on(EXTENSION_VERSION.EXTENSION_ID.eq(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID)))
                .join(extensionVersionState).on(extensionVersionState.ENTITY_ID.eq(EXTENSION_VERSION.ID).and(extensionVersionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION_VERSION)))
                .join(lastChangedExtensionVersionState).on(lastChangedExtensionVersionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionVersionState.ENTITY_ID).and(lastChangedExtensionVersionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionVersionState.TIMESTAMP)))
                .join(PERSONAL_ACCESS_TOKEN).on(PERSONAL_ACCESS_TOKEN.ID.eq(EXTENSION_VERSION.PUBLISHED_WITH_ID))
                .where(extensionState.ACTIVE.eq(true))
                .and(extensionVersionState.ACTIVE.eq(true))
                .fetchOne(publishers);
    }

    public Map<Integer, Integer> countActiveExtensionPublishersGroupedByExtensionsPublished(LocalDateTime endExclusive) {
        var aliasPublisher = "publisher";
        var aliasExtensionCount = "extension_count";
        var extensionState = ENTITY_ACTIVE_STATE.as("es");
        var lastChangedExtensionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION).asTable("ae");
        var extensionVersionState = ENTITY_ACTIVE_STATE.as("evs");
        var lastChangedExtensionVersionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION_VERSION).asTable("aev");
        var extensionCountsByPublisher = dsl.select(
                    PERSONAL_ACCESS_TOKEN.USER_DATA.as(aliasPublisher),
                    DSL.countDistinct(EXTENSION.ID).as(aliasExtensionCount)
                )
                .from(EXTENSION)
                .join(extensionState).on(extensionState.ENTITY_ID.eq(EXTENSION.ID).and(extensionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION)))
                .join(lastChangedExtensionState).on(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionState.ENTITY_ID).and(lastChangedExtensionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionState.TIMESTAMP)))
                .join(EXTENSION_VERSION).on(EXTENSION_VERSION.EXTENSION_ID.eq(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID)))
                .join(extensionVersionState).on(extensionVersionState.ENTITY_ID.eq(EXTENSION_VERSION.ID).and(extensionVersionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION_VERSION)))
                .join(lastChangedExtensionVersionState).on(lastChangedExtensionVersionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionVersionState.ENTITY_ID).and(lastChangedExtensionVersionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionVersionState.TIMESTAMP)))
                .join(PERSONAL_ACCESS_TOKEN).on(PERSONAL_ACCESS_TOKEN.ID.eq(EXTENSION_VERSION.PUBLISHED_WITH_ID))
                .where(extensionState.ACTIVE.eq(true))
                .and(extensionVersionState.ACTIVE.eq(true))
                .groupBy(PERSONAL_ACCESS_TOKEN.USER_DATA)
                .asTable("aep");

        return dsl.select(
                    extensionCountsByPublisher.field(aliasExtensionCount, Integer.class),
                    DSL.count(extensionCountsByPublisher.field(aliasPublisher))
                )
                .from(extensionCountsByPublisher)
                .groupBy(extensionCountsByPublisher.field(aliasExtensionCount, Integer.class))
                .fetch()
                .stream()
                .map(l -> new AbstractMap.SimpleEntry<>(l.value1(), l.value2()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<Integer,Integer> countActiveExtensionsGroupedByExtensionReviewRating(LocalDateTime endExclusive) {
        var aliasExtensionId = "extension_id";
        var aliasAverageRating = "average_rating";
        var extensionState = ENTITY_ACTIVE_STATE.as("es");
        var lastChangedExtensionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION).asTable("ae");
        var extensionReviewState = ENTITY_ACTIVE_STATE.as("ers");
        var lastChangedExtensionReviewState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION_REVIEW).asTable("aer");

        var averageRatingByExtension = dsl.select(
                    EXTENSION.ID.as(aliasExtensionId),
                    DSL.round(DSL.avg(EXTENSION_REVIEW.RATING)).as(aliasAverageRating)
                )
                .from(EXTENSION)
                .join(extensionState).on(extensionState.ENTITY_ID.eq(EXTENSION.ID).and(extensionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION)))
                .join(lastChangedExtensionState).on(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionState.ENTITY_ID).and(lastChangedExtensionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionState.TIMESTAMP)))
                .join(EXTENSION_REVIEW).on(EXTENSION_REVIEW.EXTENSION_ID.eq(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID)))
                .join(extensionReviewState).on(extensionReviewState.ENTITY_ID.eq(EXTENSION_REVIEW.ID).and(extensionReviewState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION_REVIEW)))
                .join(lastChangedExtensionReviewState).on(lastChangedExtensionReviewState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionReviewState.ENTITY_ID).and(lastChangedExtensionReviewState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionReviewState.TIMESTAMP)))
                .where(extensionState.ACTIVE.eq(true))
                .and(extensionReviewState.ACTIVE.eq(true))
                .groupBy(EXTENSION.ID)
                .asTable("aer");

        return dsl.select(
                    averageRatingByExtension.field(aliasAverageRating, Integer.class),
                    DSL.count(averageRatingByExtension.field(aliasExtensionId))
                )
                .from(averageRatingByExtension)
                .groupBy(averageRatingByExtension.field(aliasAverageRating, Integer.class))
                .fetch()
                .stream()
                .map(l -> new AbstractMap.SimpleEntry<>(l.value1(), l.value2()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public double averageNumberOfActiveReviewsPerActiveExtension(LocalDateTime endExclusive) {
        var extensionState = ENTITY_ACTIVE_STATE.as("es");
        var lastChangedExtensionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION).asTable("ae");
        var extensionReviewState = ENTITY_ACTIVE_STATE.as("ers");
        var lastChangedExtensionReviewState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION_REVIEW).asTable("aer");

        var averageReviewsPerExtension = DSL.case_()
                .when(DSL.count(EXTENSION.ID).greaterThan(0), DSL.count(EXTENSION_REVIEW.ID).divide(DSL.countDistinct(EXTENSION.ID).times(1.0)))
                .otherwise(0)
                .coerce(double.class)
                .as("avg_reviews_per_extension");

        return dsl.select(averageReviewsPerExtension)
                .from(EXTENSION)
                .join(extensionState).on(extensionState.ENTITY_ID.eq(EXTENSION.ID).and(extensionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION)))
                .join(lastChangedExtensionState).on(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionState.ENTITY_ID).and(lastChangedExtensionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionState.TIMESTAMP)))
                .join(EXTENSION_REVIEW).on(EXTENSION_REVIEW.EXTENSION_ID.eq(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID)))
                .join(extensionReviewState).on(extensionReviewState.ENTITY_ID.eq(EXTENSION_REVIEW.ID).and(extensionReviewState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION_REVIEW)))
                .join(lastChangedExtensionReviewState).on(lastChangedExtensionReviewState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionReviewState.ENTITY_ID).and(lastChangedExtensionReviewState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionReviewState.TIMESTAMP)))
                .where(extensionState.ACTIVE.eq(true))
                .and(extensionReviewState.ACTIVE.eq(true))
                .fetchOne(averageReviewsPerExtension);
    }

    public int countPublishersThatClaimedNamespaceOwnership(LocalDateTime endExclusive) {
        var count = DSL.countDistinct(NAMESPACE_MEMBERSHIP.USER_DATA);
        var extensionState = ENTITY_ACTIVE_STATE.as("es");
        var lastChangedExtensionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION).asTable("ae");
        var extensionVersionState = ENTITY_ACTIVE_STATE.as("evs");
        var lastChangedExtensionVersionState = findLastChangedEntityStates(endExclusive, ENTITY_TYPE_EXTENSION_VERSION).asTable("aev");
        return dsl.select(count)
                .from(EXTENSION)
                .join(extensionState).on(extensionState.ENTITY_ID.eq(EXTENSION.ID).and(extensionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION)))
                .join(lastChangedExtensionState).on(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionState.ENTITY_ID).and(lastChangedExtensionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionState.TIMESTAMP)))
                .join(EXTENSION_VERSION).on(EXTENSION_VERSION.EXTENSION_ID.eq(lastChangedExtensionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID)))
                .join(extensionVersionState).on(extensionVersionState.ENTITY_ID.eq(EXTENSION_VERSION.ID).and(extensionVersionState.ENTITY_TYPE.eq(ENTITY_TYPE_EXTENSION_VERSION)))
                .join(lastChangedExtensionVersionState).on(lastChangedExtensionVersionState.field(ENTITY_ACTIVE_STATE.ENTITY_ID).eq(extensionVersionState.ENTITY_ID).and(lastChangedExtensionVersionState.field(ALIAS_LAST_CHANGE, LocalDateTime.class).eq(extensionVersionState.TIMESTAMP)))
                .join(PERSONAL_ACCESS_TOKEN).on(PERSONAL_ACCESS_TOKEN.ID.eq(EXTENSION_VERSION.PUBLISHED_WITH_ID))
                .join(NAMESPACE_MEMBERSHIP).on(NAMESPACE_MEMBERSHIP.USER_DATA.eq(PERSONAL_ACCESS_TOKEN.USER_DATA).and(NAMESPACE_MEMBERSHIP.NAMESPACE.eq(EXTENSION.NAMESPACE_ID)))
                .where(extensionState.ACTIVE.eq(true))
                .and(extensionVersionState.ACTIVE.eq(true))
                .and(NAMESPACE_MEMBERSHIP.ROLE.eq(NamespaceMembership.ROLE_OWNER))
                .fetchOne(count);
    }

    private SelectHavingStep<Record2<Long, LocalDateTime>> findLastChangedEntityStates(LocalDateTime endExclusive, String entityType) {
        return dsl.select(ENTITY_ACTIVE_STATE.ENTITY_ID, DSL.max(ENTITY_ACTIVE_STATE.TIMESTAMP).as(ALIAS_LAST_CHANGE))
                .from(ENTITY_ACTIVE_STATE)
                .where(ENTITY_ACTIVE_STATE.TIMESTAMP.lessThan(endExclusive))
                .and(ENTITY_ACTIVE_STATE.ENTITY_TYPE.eq(entityType))
                .groupBy(ENTITY_ACTIVE_STATE.ENTITY_ID);
    }
}
