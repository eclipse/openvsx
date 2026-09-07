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
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.eclipse.openvsx.mirror.MirrorExtensionHandlerInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private MirrorExtensionHandlerInterceptor mirrorInterceptor;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Value(
        "${ovsx.webui.frontendRoutes:/extension/**,/namespace/**,/search,/user-settings/**,/publish,/admin-dashboard/**}"
    )
    String[] frontendRoutes;

    public WebConfig(Optional<MirrorExtensionHandlerInterceptor> mirrorExtensionHandlerInterceptor) {
        mirrorExtensionHandlerInterceptor.ifPresent(service -> this.mirrorInterceptor = service);
    }

    /**
     * Registration order is load-bearing. {@code UrlBasedCorsConfigurationSource} walks its mappings in the
     * order they were added and returns the configuration of the <em>first</em> pattern that matches - it
     * neither combines them nor prefers the more specific one. Every credentialed mapping below is also
     * matched by the {@code /api/**} catch-all, so the credentialed ones have to be registered first or
     * they would silently lose their configuration to it.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (!StringUtils.isEmpty(webuiUrl) && URI.create(webuiUrl).isAbsolute()) {
            // The Web UI is given with an absolute URL, so we need to enable CORS with credentials. Where
            // it is served from the same origin as the API these are unnecessary: a same-origin request
            // does not use CORS at all.
            var authorizedEndpoints = new String[] {
                "/user/**",
                "/logout",
                "/api/*/*/review/**",
                "/api/user/publish",
                "/api/user/namespace/create",
                "/api/-/trusted-publishing/status",
                "/admin/**"
            };
            for (var endpoint : authorizedEndpoints) {
                registry.addMapping(endpoint)
                        .allowedOrigins(webuiUrl)
                        .allowedMethods("GET", "HEAD", "POST", "DELETE", "PUT")
                        .allowCredentials(true);
            }
            registry.addMapping("/login-providers")
                    .allowedOrigins(webuiUrl);
        }

        // The public, unauthenticated surface: the registry API, the VS Code gallery adapter and the
        // static documents, readable from any origin and never with credentials.
        //
        // Registered whatever ovsx.webui.url says, because it has nothing to do with them. These used to
        // sit inside the branch above, so a deployment serving the Web UI from the same origin as the API
        // - where that property is empty or relative - emitted no CORS headers at all, and the browser
        // clients that need them (vscode.dev, Gitpod, Theia and anything else querying the gallery from
        // page script) could only be served by putting the headers back on at the CDN. Which is a poor
        // place for them: the edge cannot tell the public surface from the credentialed one, and a rule
        // permissive enough to cover the first is a rule that hands the second to any origin that asks.
        //
        // Clients that are not browsers - VS Code desktop fetches the gallery from its node extension
        // host - never consult these headers at all, and are unaffected either way.
        registry.addMapping("/api/**")
                .allowedOrigins("*");
        registry.addMapping("/vscode/**")
                .allowedOrigins("*");
        registry.addMapping("/documents/**")
                .allowedOrigins("*");
    }

    /**
     * Marks every cookie this application sets {@code SameSite=Lax}.
     *
     * <p>Without the attribute the decision falls to the browser, and browsers do not agree: Chromium
     * treats an unmarked cookie as Lax, others have not always. That left the session cookie's behaviour
     * on a cross-site {@code fetch} - whether it rides along and makes the request an authenticated one -
     * a property of the visitor's browser rather than of this server. It is the last thing standing
     * between a misconfigured CORS layer and a credentialed cross-origin read of {@code /user/**}, which
     * is not a job to leave to a default.
     *
     * <p>Lax and not Strict deliberately. Strict withholds the cookie from cross-site top-level
     * navigations too, which is exactly what the return leg of an OAuth2 login is: the provider redirects
     * the browser back here, and Spring Security needs the session to find the authorization request it
     * saved. Lax sends the cookie on that navigation and withholds it from the subresource requests -
     * {@code fetch}, {@code XMLHttpRequest}, images, frames - that an attacking page would have to use.
     */
    @Bean
    CookieSameSiteSupplier laxCookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofLax();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (var route : frontendRoutes) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (mirrorInterceptor != null) {
            registry.addInterceptor(mirrorInterceptor)
                    .addPathPatterns(
                            "/vscode/asset/**",
                            "/vscode/item",
                            "/vscode/gallery/publishers/**",
                            "/vscode/unpkg/**");
        }
    }
}
