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
package org.eclipse.openvsx.analytics;

import java.util.List;

/**
 * Storage for download analytics: one interface for writing events and reading series.
 */
public interface DownloadAnalyticsRepository {

    /**
     * Persists the given events. Implementations must participate in the caller's transaction,
     * so that events, the extension download counter and the download ingestion entry commit atomically.
     */
    void save(List<DownloadEvent> events);

    /**
     * Returns the (sparse) aggregated download series for the given request. Buckets without
     * downloads are absent; zero-filling is the {@link DownloadAnalyticsService}'s concern.
     */
    List<DownloadSeriesRow> findSeries(DownloadSeriesRequest request);
}
