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

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

@Configuration
@Profile("test")
public class TestSearchConfig extends AbstractElasticsearchConfiguration {

    @Override
    @SuppressWarnings("resource")
    public RestHighLevelClient elasticsearchClient() {
        var container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.9.3");
        container.start();
        var config = ClientConfiguration.create(container.getHttpHostAddress());
        return RestClients.create(config).rest();
    }
    
}