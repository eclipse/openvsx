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

        // @formatter:off
        http.authorizeRequests()
                .antMatchers("/logout") // TODO add a single "/login" route to directly use GH auth
                    .permitAll()
                // .antMatchers("/user/**", "/login/**", "/logout", "/api/**", "/admin/**", "/vscode/**")
                .anyRequest()
                    .authenticated()
                .and()
            .cors()
                .disable()
            // .sessionManagement()
            //     .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            //     .and()
            // .exceptionHandling()
            //     .authenticationEntryPoint(new RestAuthenticationEntryPoint()) 
            //     .and()
            .csrf()
                .ignoringAntMatchers("/api/-/publish", "/api/-/namespace/create", "/admin/**", "/vscode/**")
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

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/v2/api-docs", "/swagger-resources/**", "/swagger-ui/**", "/webjars/**");
    }

}