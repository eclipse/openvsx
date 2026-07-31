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

import java.time.Clock;
import java.time.Duration;

import org.jooq.DSLContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import org.eclipse.openvsx.analytics.timescale.TimescaleDownloadAnalyticsRepository;

/**
 * Wires download analytics when {@code ovsx.analytics.enabled=true}. The download_event schema
 * is part of the main migration chain, so the database image must provide the timescaledb
 * extension.
 */
@Configuration
@ConditionalOnProperty(name = "ovsx.analytics.enabled", havingValue = "true")
class DownloadAnalyticsConfiguration {

    @Bean
    DownloadAnalyticsRepository downloadAnalyticsRepository(DSLContext dsl) {
        return new TimescaleDownloadAnalyticsRepository(dsl);
    }

    @Bean
    DownloadAnalyticsService downloadAnalyticsService(
            DownloadAnalyticsRepository repository,
            Environment environment
    ) {
        var settlingMargin = environment
                .getProperty("ovsx.analytics.settling-margin", Duration.class, Duration.ofHours(2));
        return new DownloadAnalyticsService(repository, settlingMargin, Clock.systemUTC());
    }
}
