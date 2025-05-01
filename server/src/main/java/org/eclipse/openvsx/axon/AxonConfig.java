/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.axon;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.DuplicateCommandHandlerResolver;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.SQLErrorCodesResolver;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.modelling.saga.repository.jpa.JpaSagaStore;
import org.axonframework.serialization.Serializer;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@Profile("!test")
public class AxonConfig {

    @Bean(defaultCandidate = false)
    @Qualifier("axon")
    @ConfigurationProperties(prefix = "ovsx.axon.datasource")
    public DataSource eventsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(defaultCandidate = false)
    @Qualifier("axon")
    @ConfigurationProperties(prefix = "ovsx.axon.jpa")
    public JpaProperties eventsJpaProperties() {
        return new JpaProperties();
    }

    @Qualifier("axon")
    @Bean(defaultCandidate = false)
    public LocalContainerEntityManagerFactoryBean secondEntityManagerFactory(
            @Qualifier("axon") DataSource dataSource,
            @Qualifier("axon") JpaProperties jpaProperties
    ) {
        EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(jpaProperties);
        return builder
                .dataSource(dataSource)
                .packages(
                        "org.axonframework.eventsourcing.eventstore.jpa",
                        "org.axonframework.modelling.saga.repository.jpa",
                        "org.axonframework.eventhandling.tokenstore",
                        "org.axonframework.eventhandling.deadletter.jpa"
                )
                .persistenceUnit("axon")
                .build();
    }

    private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {
        JpaVendorAdapter jpaVendorAdapter = createJpaVendorAdapter(jpaProperties);
        Function<DataSource, Map<String, ?>> jpaPropertiesFactory = (dataSource) -> createJpaProperties(dataSource,
                jpaProperties.getProperties());
        return new EntityManagerFactoryBuilder(jpaVendorAdapter, jpaPropertiesFactory, null);
    }

    private JpaVendorAdapter createJpaVendorAdapter(JpaProperties jpaProperties) {
        var adapter = new HibernateJpaVendorAdapter();
        adapter.setShowSql(jpaProperties.isShowSql());
        adapter.setGenerateDdl(jpaProperties.isGenerateDdl());
        adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        var database = jpaProperties.getDatabase();
        if(database != null) {
            adapter.setDatabase(database);
        }
        return adapter;
    }

    private Map<String, ?> createJpaProperties(DataSource dataSource, Map<String, ?> existingProperties) {
        Map<String, ?> jpaProperties = new LinkedHashMap<>(existingProperties);
        // ... map JPA properties that require the DataSource (e.g. DDL flags)
        return jpaProperties;
    }

    @Bean(defaultCandidate = false)
    @Qualifier("axon")
    public PlatformTransactionManager eventsPlatformTransactionManager(
            @Qualifier("axon") EntityManagerFactory entityManagerFactory
    ) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(defaultCandidate = false)
    @Qualifier("axon")
    public EntityManager eventsSharedEntityManager(@Qualifier("axon") EntityManagerFactory entityManagerFactory ) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    @Bean
    @Primary
    public EntityManagerProvider eventsEntityManagerProvider(@Qualifier("axon") EntityManager entityManager) {
        return new SimpleEntityManagerProvider(entityManager);
    }

    @Bean
    @Primary
    public TransactionManager eventsTransactionManager(@Qualifier("axon") PlatformTransactionManager transactionManager) {
        return new SpringTransactionManager(transactionManager);
    }

    @Bean
    @Primary
    public PersistenceExceptionResolver eventsDataSourcePER(@Qualifier("axon")DataSource dataSource) throws SQLException {
        return new SQLErrorCodesResolver(dataSource);
    }

    @Bean
    @Primary
    public EventStorageEngine eventStorageEngine(
            @Qualifier("eventSerializer") Serializer eventSerializer,
            Serializer snapshotSerializer,
            @Qualifier("axon") DataSource dataSource,
            EntityManagerProvider entityManagerProvider,
            PersistenceExceptionResolver persistenceExceptionResolver,
            TransactionManager transactionManager
    ) throws SQLException {
        return JpaEventStorageEngine.builder()
                .eventSerializer(eventSerializer)
                .snapshotSerializer(snapshotSerializer)
                .dataSource(dataSource)
                .entityManagerProvider(entityManagerProvider)
                .persistenceExceptionResolver(persistenceExceptionResolver)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @Primary
    public JpaSagaStore sagaStore(
            Serializer serializer,
            EntityManagerProvider entityManagerProvider
    ) {
        return JpaSagaStore.builder()
                .entityManagerProvider(entityManagerProvider)
                .serializer(serializer)
                .build();
    }

    @Bean
    @Primary
    public SimpleCommandBus commandBus(
            org.axonframework.config.Configuration axonConfiguration,
            DuplicateCommandHandlerResolver duplicateCommandHandlerResolver,
            TransactionManager txManager
    ) {
        var commandBus = SimpleCommandBus.builder()
                .transactionManager(txManager)
                .duplicateCommandHandlerResolver(duplicateCommandHandlerResolver)
                .messageMonitor(axonConfiguration.messageMonitor(CommandBus.class, "commandBus"))
            .build();

        commandBus.registerHandlerInterceptor(
                new CorrelationDataInterceptor(axonConfiguration.correlationDataProviders())
        );
        return commandBus;
    }
}
