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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.TreeMap;

@Configuration
public class DocumentationConfig {

    @Bean
    public GroupedOpenApi api(OpenApiCustomizer sortSchemasAlphabetically) {
        return GroupedOpenApi.builder()
                .group("default")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(sortSchemasAlphabetically)
                .build();
    }

    @Bean
    public OpenApiCustomizer sortSchemasAlphabetically() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            openApi.getComponents().setSchemas(new TreeMap<>(schemas));
        };
    }

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info().title("Open VSX Registry API")
                .description(
                    "This API provides metadata of VS Code extensions in the Open VSX Registry " +
                    "as well as means to publish extensions."
                )
                .termsOfService("https://www.eclipse.org/legal/termsofuse.php")
                .version("0.1")
                .license(new License()
                    .name("Eclipse Public License 2.0")
                    .url("https://www.eclipse.org/legal/epl-2.0/")));
    }
}