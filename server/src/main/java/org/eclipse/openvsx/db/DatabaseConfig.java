/********************************************************************************
 * Copyright (c) 2025 and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.db;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Database configuration for read/write splitting.
 * 
 * This configuration sets up separate connection pools for:
 * - Primary database: handles all write operations
 * - Replica database: handles read-only operations (when available)
 * 
 * When replica is not configured, all operations go to the primary database.
 * This provides backward compatibility with existing single-database deployments.
 */
@Configuration
public class DatabaseConfig {
    
    protected final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    /**
     * Configuration properties for the primary (write) database.
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSourceProperties primaryDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    /**
     * Configuration properties for the replica (read-only) database.
     * Only loaded when ovsx.datasource.replica.enabled=true
     */
    @Bean
    @ConditionalOnProperty(prefix = "ovsx.datasource.replica", name = "enabled", havingValue = "true")
    @ConfigurationProperties("spring.datasource.replica")
    public DataSourceProperties replicaDataSourceProperties() {
        return new DataSourceProperties();
    }
    
    /**
     * Primary database connection pool using HikariCP.
     * Handles all write operations and reads when replica is not available.
     */
    @Bean
    @ConfigurationProperties("spring.datasource.primary.hikari")
    public DataSource primaryDataSource(@Qualifier("primaryDataSourceProperties") DataSourceProperties properties) {
        logger.info("Configuring primary datasource: {}", properties.getUrl());
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
    
    /**
     * Replica database connection pool using HikariCP.
     * Handles read-only operations for improved horizontal scalability.
     * Only created when replica configuration is enabled.
     */
    @Bean
    @ConditionalOnProperty(prefix = "ovsx.datasource.replica", name = "enabled", havingValue = "true")
    @ConfigurationProperties("spring.datasource.replica.hikari")
    public DataSource replicaDataSource(@Qualifier("replicaDataSourceProperties") DataSourceProperties properties) {
        logger.info("Configuring replica datasource: {}", properties.getUrl());
        return properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }
    
    /**
     * Routing datasource that directs queries to primary or replica based on transaction type.
     * This is the main datasource bean used by the application.
     */
    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Autowired(required = false) @Qualifier("replicaDataSource") DataSource replicaDataSource
    ) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.PRIMARY, primaryDataSource);
        
        // Add replica datasource only if it's configured
        if (replicaDataSource != null) {
            targetDataSources.put(DataSourceType.REPLICA, replicaDataSource);
            logger.info("Read/write splitting enabled - using primary for writes and replica for reads");
        } else {
            // If no replica, route all reads to primary as well
            targetDataSources.put(DataSourceType.REPLICA, primaryDataSource);
            logger.info("Replica not configured - all operations will use primary datasource");
        }
        
        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        
        return routingDataSource;
    }
}
