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
package org.eclipse.openvsx.analytics.timescale;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.google.common.collect.Lists;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;

import org.eclipse.openvsx.analytics.DownloadAnalyticsRepository;
import org.eclipse.openvsx.analytics.DownloadEvent;
import org.eclipse.openvsx.analytics.DownloadSeriesGroupBy;
import org.eclipse.openvsx.analytics.DownloadSeriesInterval;
import org.eclipse.openvsx.analytics.DownloadSeriesRequest;
import org.eclipse.openvsx.analytics.DownloadSeriesRow;

import static org.eclipse.openvsx.jooq.Tables.DOWNLOAD_EVENT;
import static org.eclipse.openvsx.jooq.Tables.DOWNLOAD_STATS_DAILY;

/**
 * {@link DownloadAnalyticsRepository} backed by TimescaleDB: writes to the download_event
 * hypertable and reads from the download_stats_daily continuous aggregate. Queries run through
 * the application's transaction-aware {@link DSLContext}, so writes commit atomically with the
 * download counter and the ingestion entry.
 */
public class TimescaleDownloadAnalyticsRepository implements DownloadAnalyticsRepository {

    private static final int BATCH_SIZE = 500;

    private final DSLContext dsl;

    public TimescaleDownloadAnalyticsRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void save(List<DownloadEvent> events) {
        for (var batch : Lists.partition(events, BATCH_SIZE)) {
            var insert = dsl.insertInto(
                    DOWNLOAD_EVENT,
                    DOWNLOAD_EVENT.TIME,
                    DOWNLOAD_EVENT.EXTENSION_ID,
                    DOWNLOAD_EVENT.EXTENSION_VERSION_ID,
                    DOWNLOAD_EVENT.NAMESPACE,
                    DOWNLOAD_EVENT.EXTENSION_NAME,
                    DOWNLOAD_EVENT.VERSION,
                    DOWNLOAD_EVENT.TARGET_PLATFORM,
                    DOWNLOAD_EVENT.COUNTRY,
                    DOWNLOAD_EVENT.IP,
                    DOWNLOAD_EVENT.USER_AGENT,
                    DOWNLOAD_EVENT.COUNT);
            for (var event : batch) {
                insert = insert.values(
                        OffsetDateTime.ofInstant(event.time(), ZoneOffset.UTC),
                        event.extensionId(),
                        event.extensionVersionId(),
                        event.namespace(),
                        event.extensionName(),
                        event.version(),
                        event.targetPlatform(),
                        event.country(),
                        event.ip(),
                        event.userAgent(),
                        event.count());
            }
            insert.execute();
        }
    }

    @Override
    public List<DownloadSeriesRow> findSeries(DownloadSeriesRequest request) {
        var bucket = bucketField(request.interval());
        var group = groupField(request.groupBy());
        var total = DSL.sum(DOWNLOAD_STATS_DAILY.DOWNLOADS).cast(Long.class);

        List<Field<?>> groupByFields = request.groupBy() == DownloadSeriesGroupBy.NONE
                ? List.<Field<?>>of(bucket)
                : List.<Field<?>>of(bucket, group);
        return dsl.select(bucket, group, total)
                .from(DOWNLOAD_STATS_DAILY)
                .where(
                        DOWNLOAD_STATS_DAILY.EXTENSION_ID.in(request.extensionIds()),
                        DOWNLOAD_STATS_DAILY.DAY
                                .greaterOrEqual(OffsetDateTime.ofInstant(request.from(), ZoneOffset.UTC)),
                        DOWNLOAD_STATS_DAILY.DAY.lessThan(OffsetDateTime.ofInstant(request.to(), ZoneOffset.UTC)))
                .groupBy(groupByFields)
                .orderBy(groupByFields)
                .fetch(record -> new DownloadSeriesRow(record.value1().toInstant(), record.value2(), record.value3()));
    }

    private Field<OffsetDateTime> bucketField(DownloadSeriesInterval interval) {
        // `day` holds UTC-aligned buckets; date_trunc must not depend on the session time zone,
        // hence the AT TIME ZONE round-trip
        return switch (interval) {
            case DAY -> DOWNLOAD_STATS_DAILY.DAY;
            case WEEK -> DSL.field(
                    "(date_trunc('week', {0} AT TIME ZONE 'UTC') AT TIME ZONE 'UTC')",
                    OffsetDateTime.class,
                    DOWNLOAD_STATS_DAILY.DAY);
            case MONTH -> DSL.field(
                    "(date_trunc('month', {0} AT TIME ZONE 'UTC') AT TIME ZONE 'UTC')",
                    OffsetDateTime.class,
                    DOWNLOAD_STATS_DAILY.DAY);
        };
    }

    private Field<String> groupField(DownloadSeriesGroupBy groupBy) {
        return switch (groupBy) {
            case NONE -> DSL.inline(null, String.class);
            case VERSION -> DOWNLOAD_STATS_DAILY.VERSION;
            case TARGET_PLATFORM -> DOWNLOAD_STATS_DAILY.TARGET_PLATFORM;
            case COUNTRY -> DOWNLOAD_STATS_DAILY.COUNTRY;
        };
    }
}
