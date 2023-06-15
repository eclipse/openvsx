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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Autowired
    OAuth2UserServices userServices;

    @Value("${ovsx.webui.frontendRoutes:/extension/**,/namespace/**,/user-settings/**,/admin-dashboard/**}")
    String[] frontendRoutes;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        var redirectUrl = StringUtils.isEmpty(webuiUrl) ? "/" : webuiUrl;
        return http.authorizeHttpRequests(
                registry -> registry
                        .requestMatchers(antMatchers("/*", "/login/**", "/oauth2/**", "/user", "/user/auth-error", "/logout", "/actuator/health/**", "/actuator/metrics", "/actuator/metrics/**", "/actuator/prometheus", "/v3/api-docs/**", "/swagger-resources/**", "/swagger-ui/**", "/webjars/**"))
                            .permitAll()
                        .requestMatchers(antMatchers("/api/*/*/review", "/api/*/*/review/delete", "/api/user/publish", "/api/user/namespace/create"))
                            .authenticated()
                        .requestMatchers(antMatchers("/api/**", "/vscode/**", "/documents/**", "/admin/report", "/admin/reports", "/admin/report/schedule"))
                            .permitAll()
                        .requestMatchers(antMatchers("/admin/**"))
                            .hasAuthority("ROLE_ADMIN")
                        .requestMatchers(antMatchers(frontendRoutes))
                            .permitAll()
                        .anyRequest()
                            .authenticated()
                )
                .cors(configurer -> configurer.configure(http))
                .csrf(configurer -> {
                    configurer.ignoringRequestMatchers(antMatchers("/api/-/publish", "/api/-/namespace/create", "/api/-/query", "/admin/report/schedule", "/vscode/**"));
                })
                .exceptionHandling(configurer -> configurer.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .oauth2Login(configurer -> {
                    configurer.defaultSuccessUrl(redirectUrl);
                    configurer.successHandler(new CustomAuthenticationSuccessHandler(redirectUrl));
                    configurer.failureUrl(redirectUrl + "?auth-error");
                    configurer.userInfoEndpoint(customizer -> customizer.oidcUserService(userServices.getOidc()).userService(userServices.getOauth2()));
                })
                .logout(configurer -> configurer.logoutSuccessUrl(redirectUrl))
                .build();
    }

    private RequestMatcher[] antMatchers(String... patterns)
    {
        var antMatchers = new RequestMatcher[patterns.length];
        for(var i = 0; i < patterns.length; i++) {
            antMatchers[i] = AntPathRequestMatcher.antMatcher(patterns[i]);
        }

        return antMatchers;
    }
}