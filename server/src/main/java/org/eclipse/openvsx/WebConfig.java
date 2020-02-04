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

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ovsx.webui.url}")
    String webuiUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!Strings.isNullOrEmpty(webuiUrl)) {
            var authorizedEndpoints = new String[] {
                "/user",
                "/logout",
                "/api/*/*/review"
            };
            for (var endpoint : authorizedEndpoints) {
                registry.addMapping(endpoint)
                        .allowedOrigins(webuiUrl)
                        .allowCredentials(true);
            }
        }
    }

}