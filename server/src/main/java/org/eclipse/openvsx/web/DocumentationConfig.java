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
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Stream;

@Configuration
public class DocumentationConfig {

    @Bean
    public GroupedOpenApi registry(OpenApiCustomizer sortSchemasAlphabetically, OpenApiCustomizer addRateLimitResponse) {
        var description = "This API provides metadata of VS Code extensions in the Open VSX Registry as well as means to publish extensions.";
        return GroupedOpenApi.builder()
                .group("registry")
                .displayName("Registry API")
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(openApi -> openApi.getInfo().title("Open VSX Registry API").description(description))
                .addOpenApiCustomizer(sortSchemasAlphabetically)
                .addOpenApiCustomizer(addRateLimitResponse)
                .build();
    }

    @Bean
    public GroupedOpenApi vscode(OpenApiCustomizer sortSchemasAlphabetically, OpenApiCustomizer addRateLimitResponse) {
        var description = "Provides a compatibility layer between VS Code based editors and the Open VSX Registry.";
        return GroupedOpenApi.builder()
                .group("vscode-adapter")
                .displayName("VSCode Adapter")
                .pathsToMatch("/vscode/**")
                .addOpenApiCustomizer(openApi -> openApi.getInfo().title("Open VSX VSCode Adapter").description(description))
                .addOpenApiCustomizer(sortSchemasAlphabetically)
                .addOpenApiCustomizer(addRateLimitResponse)
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
    public OpenApiCustomizer addRateLimitResponse() {
        var retryAfterHeader = new Header()
                .description("Number of seconds to wait after receiving a 429 response")
                .schema(new Schema<>().type("integer").format("int32"));
        var limitRemainingHeader = new Header()
                .description("Remaining number of requests left")
                .schema(new Schema<>().type("integer").format("int32"));

        var response = new ApiResponse()
                .description("A client has sent too many requests in a given amount of time")
                .headers(Map.of(
                        "X-Rate-Limit-Retry-After-Seconds", retryAfterHeader,
                        "X-Rate-Limit-Remaining", limitRemainingHeader
                ));

        return openApi -> {
            openApi.getPaths().forEach((path, item) -> {
                Stream.of(
                    item.getGet(),
                    item.getHead(),
                    item.getPost(),
                    item.getPut(),
                    item.getDelete(),
                    item.getOptions(),
                    item.getTrace(),
                    item.getPatch()
                )
                .filter(Objects::nonNull)
                .forEach(operation -> {
                    var responses = operation.getResponses();
                    if(responses == null) {
                        responses = new ApiResponses();
                    }

                    responses.addApiResponse("429", response);
                    operation.setResponses(responses);
                });
            });
        };
    }

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
            .info(new Info()
                .termsOfService("https://www.eclipse.org/legal/termsofuse.php")
                .version("0.1")
                .license(new License()
                    .name("Eclipse Public License 2.0")
                    .url("https://www.eclipse.org/legal/epl-2.0/")));
    }
}