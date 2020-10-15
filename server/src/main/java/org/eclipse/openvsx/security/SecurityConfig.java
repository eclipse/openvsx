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

import com.google.common.base.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Autowired
    ExtendedOAuth2UserServices userServices;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        var redirectUrl = Strings.isNullOrEmpty(webuiUrl) ? "/" : webuiUrl;

        http
            .authorizeRequests()
                .antMatchers("/login/**", "/oauth2/**", "/user", "/logout")
                    .permitAll()
                .antMatchers("/api/*/*/review", "/api/*/*/review/delete")
                    .authenticated()
                .antMatchers("/api/**", "/vscode/**")
                    .permitAll()
                .antMatchers("/admin/**")
                    .hasAuthority("ROLE_ADMIN")
                .anyRequest()
                    .authenticated()
                .and()
            .cors()
                .and()
            .csrf()
                .ignoringAntMatchers("/api/-/publish", "/api/-/namespace/create", "/api/-/query", "/vscode/**")
                .and()
            .exceptionHandling()
                // Respond with 403 status when the user is not logged in
                .authenticationEntryPoint(new Http403ForbiddenEntryPoint())
                .and()

            .oauth2Login(configurer -> {
                configurer.defaultSuccessUrl(redirectUrl);
                configurer.addObjectPostProcessor(new ObjectPostProcessor<OidcAuthorizationCodeAuthenticationProvider>() {
                    @Override
                    public <O extends OidcAuthorizationCodeAuthenticationProvider> O postProcess(O object) {
                        object.setJwtDecoderFactory(new NoVerifyJwtDecoderFactory());
                        return object;
                    }
                });
                configurer.successHandler(new ExtendedAuthenticationSuccessHandler(redirectUrl));
                configurer.userInfoEndpoint()
                    .oidcUserService(userServices.getOidc())
                    .userService(userServices.getOauth2());
            })

            .logout()
                .logoutSuccessUrl(redirectUrl);
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        // Ignore resources required by Swagger API documentation
        web.ignoring().antMatchers("/v2/api-docs", "/swagger-resources/**", "/swagger-ui/**", "/webjars/**");
    }

}