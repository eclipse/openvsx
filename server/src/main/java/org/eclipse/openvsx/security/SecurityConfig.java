/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

import java.net.URI;

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@EnableWebSecurity(debug = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    private ExtendedOAuth2UserService extendedOAuth2UserService;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        var isAbsoluteWebUi = !Strings.isNullOrEmpty(webuiUrl) && URI.create(webuiUrl).isAbsolute();
        
        // @formatter:off
         // TODO add a single "/login" route to directly use GH auth
        http.authorizeRequests()
                .antMatchers(getAuthenticatedPaths(isAbsoluteWebUi))
                    .authenticated()
                .antMatchers(getPermittedPaths(isAbsoluteWebUi))
                    .permitAll()
                .and()
            // .cors()
            //     .disable()
            // .sessionManagement()
            //     .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            //     .and()
            .exceptionHandling()
                // Respond with 403 status when the user is not logged in
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .and()
            .csrf()
                .ignoringAntMatchers("/api/-/publish", "/api/-/namespace/create", "/api/-/query", "/admin/**", "/vscode/**")
                .and()
            .oauth2Login(configurer -> {
                configurer.addObjectPostProcessor(new ObjectPostProcessor<OidcAuthorizationCodeAuthenticationProvider>() {
                    @Override
                    public <O extends OidcAuthorizationCodeAuthenticationProvider> O postProcess(O object) {
                        object.setJwtDecoderFactory(new NoVerifyJwtDecoderFactory());
                        return object;
                    }
                });
                configurer.userInfoEndpoint()
                    .userService(extendedOAuth2UserService)
                    .customUserType(GitHubOAuth2User.class, "github")
                    .customUserType(EclipseOAuth2User.class, "eclipse");
                
                // configurer.successHandler(successHandler); // TODO add redirect hndlr
            });
        // @formatter:on
        
    }

    private String[] getAuthenticatedPaths(boolean isAbsoluteWebUi) {
        if (isAbsoluteWebUi) {
            return new String[0];
        } else {
            return new String[] {
                "/user/tokens", "/user/token/**", "/user/namespaces", "/user/namespace/**", "/user/search/**",
                "/api/*/*/review/**"
            };
        }
    }

    private String[] getPermittedPaths(boolean isAbsoluteWebUi) {
        if (isAbsoluteWebUi) {
            // All endpoints are marked as permitted for CORS to work correctly.
            // User authentication is checked within the endpoints that require it.
            // TODO check whether this can be solved in another way
            return new String[] {
                "/user/**", "/login/**", "/logout", "/api/**", "/admin/**", "/vscode/**"
            };
        } else {
            return new String[] {
                "/user", "/login/**", "/logout", "/api/**", "/admin/**", "/vscode/**"
            };
        }
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/v2/api-docs", "/swagger-resources/**", "/swagger-ui/**", "/webjars/**");
    }

}