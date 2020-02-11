/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.search;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class SearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${ovsx.elasticsearch.host:}")
    String searchHost;

    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration config;
        if (Strings.isNullOrEmpty(searchHost)) {
            config = ClientConfiguration.localhost();
        } else {
            config = ClientConfiguration.create(searchHost);
        }
        return RestClients.create(config).rest();
    }

}