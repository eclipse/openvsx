/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@Configuration
@Profile("test")
public class TestSearchConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        var container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.7.1")
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false");

        container.start();
        return ClientConfiguration.create(container.getHttpHostAddress());
    }
}