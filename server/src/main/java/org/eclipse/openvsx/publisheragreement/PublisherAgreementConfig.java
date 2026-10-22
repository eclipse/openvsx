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
package org.eclipse.openvsx.publisheragreement;

import java.util.Arrays;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;

import org.eclipse.openvsx.repositories.RepositoryService;

@Configuration(proxyBeanMethods = false)
public class PublisherAgreementConfig {

    // A deployment's own bean is @Primary; this stays a candidate so injection by type has a fallback.
    @Bean
    PublisherAgreementProvider simplePublisherAgreementProvider(
            RepositoryService repositories,
            @Value("${ovsx.publisher-agreement.version:}") String version,
            Environment environment,
            ListableBeanFactory beanFactory
    ) {
        // Counts this bean too: a deployment's own provider, which may use ovsx.eclipse.*, makes it more than one.
        if (beanFactory.getBeanNamesForType(PublisherAgreementProvider.class, true, false).length <= 1) {
            warnAboutEclipseProperties(environment);
        }
        return new SimplePublisherAgreementProvider(repositories, version);
    }

    // The Eclipse agreement is gone and its properties bind to nothing, so enforcement silently stops.
    private static void warnAboutEclipseProperties(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurable
                && configurable.getPropertySources().stream()
                        .filter(EnumerablePropertySource.class::isInstance)
                        .map(EnumerablePropertySource.class::cast)
                        .flatMap(source -> Arrays.stream(source.getPropertyNames()))
                        .anyMatch(name -> name.startsWith("ovsx.eclipse."))) {
            LoggerFactory.getLogger(PublisherAgreementConfig.class).warn(
                    "ovsx.eclipse.* properties are ignored since the Eclipse publisher agreement was removed; "
                            + "provide a PublisherAgreementProvider bean or set ovsx.publisher-agreement.version");
        }
    }
}
