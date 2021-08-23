/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.web;

import java.net.URI;

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Value("${ovsx.webui.frontendRoutes:/extension/**,/user-settings/**,/admin-dashboard/**}")
    String[] frontendRoutes;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!Strings.isNullOrEmpty(webuiUrl) && URI.create(webuiUrl).isAbsolute()) {
            // The Web UI is given with an absolute URL, so we need to enable CORS with credentials.
            var authorizedEndpoints = new String[] {
                "/user/**",
                "/logout",
                "/api/*/*/review/**",
                "/admin/**"
            };
            for (var endpoint : authorizedEndpoints) {
                registry.addMapping(endpoint)
                        .allowedOrigins(webuiUrl)
                        .allowCredentials(true);
            }
            registry.addMapping("/documents/**")
                    .allowedOrigins("*");
            registry.addMapping("/api/**")
                    .allowedOrigins("*");
            registry.addMapping("/vscode/**")
                    .allowedOrigins("*");
        }
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (var route : frontendRoutes) {
            registry.addViewController(route).setViewName("forward:/");
        }
    }

}
