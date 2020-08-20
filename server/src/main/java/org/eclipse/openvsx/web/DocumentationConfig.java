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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class DocumentationConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .useDefaultResponseMessages(false)
            .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.ant("/api/**"))
            .build();
    }

    @Bean
    public UiConfiguration uiConfig() {
        return UiConfigurationBuilder.builder() 
            .docExpansion(DocExpansion.LIST)
            .supportedSubmitMethods(new String[] { "get" })
            .build();
    }

    protected ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Open VSX Registry API")
            .description(
                "This API provides metadata of VS Code extensions in the Open VSX Registry " +
                "as well as means to publish extensions."
            )
            .termsOfServiceUrl("https://www.eclipse.org/legal/termsofuse.php")
            .license("Eclipse Public License 2.0")
            .licenseUrl("https://www.eclipse.org/legal/epl-2.0/")
            .version("0.1")
            .build();
    }
    
}