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

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The time-series database behind download analytics: its own PostgreSQL instance (with the
 * timescaledb extension), its own connection pool, its own Flyway migration set and its own
 * jOOQ context, all configured from {@code ovsx.analytics.datasource.*}. Nothing is created
 * unless {@code ovsx.analytics.enabled=true}.
 */
@Configuration
@ConditionalOnProperty(name = "ovsx.analytics.enabled", havingValue = "true")
class TimeseriesDatabaseConfiguration {

    private static final int DEFAULT_POOL_SIZE = 5;

    // defaultCandidate = false keeps these beans invisible to @ConditionalOnMissingBean and to
    // plain by-type injection, so Boot still auto-configures the primary DataSource, the main
    // Flyway chain and the primary DSLContext; only an explicit @Qualifier reaches them.
    @Bean(destroyMethod = "close", defaultCandidate = false)
    DataSource timeseriesDataSource(Environment environment) {
        var config = new HikariConfig();
        config.setPoolName("timeseries");
        config.setJdbcUrl(environment.getRequiredProperty("ovsx.analytics.datasource.url"));
        config.setUsername(environment.getProperty("ovsx.analytics.datasource.username"));
        config.setPassword(environment.getProperty("ovsx.analytics.datasource.password"));
        config.setMaximumPoolSize(
                environment.getProperty(
                        "ovsx.analytics.datasource.maximum-pool-size",
                        Integer.class,
                        DEFAULT_POOL_SIZE));
        return new HikariDataSource(config);
    }

    /**
     * Migrates the time-series schema. Configured programmatically rather than through
     * {@code spring.flyway.*} so that the main and the time-series migration settings cannot leak
     * into each other; in particular there is no baseline, because this database starts empty and
     * an unexpected schema must fail the startup.
     */
    @Bean(defaultCandidate = false)
    Flyway timeseriesFlyway(@Qualifier("timeseriesDataSource") DataSource dataSource) {
        var flyway = Flyway.configure()
                .dataSource(dataSource)
                // a sibling of db/migration, never a child: Flyway scans locations recursively,
                // so a child would be swept into the registry's migration chain as well
                .locations("classpath:db/migration-timeseries")
                .load();
        flyway.migrate();
        return flyway;
    }

    /**
     * Standalone jOOQ context on the time-series pool. Being outside Spring's transaction and
     * exception-translation infrastructure, queries throw jOOQ's {@code DataAccessException}
     * rather than Spring's, and never join a caller's registry transaction.
     */
    @Bean(defaultCandidate = false)
    DSLContext timeseriesDsl(
            @Qualifier("timeseriesDataSource") DataSource dataSource,
            // depended upon so the schema exists before the first query
            @Qualifier("timeseriesFlyway") Flyway flyway
    ) {
        return DSL.using(dataSource, SQLDialect.POSTGRES);
    }
}
