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

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ovsx.webui.url}")
    String webuiUrl;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/user", "/login/**", "/logout", "/api/**")
                .permitAll()
            .anyRequest()
                .authenticated();
        http.csrf()
            .ignoringAntMatchers("/logout", "/api/-/publish");
        if (!Strings.isNullOrEmpty(webuiUrl)) {
            http.oauth2Login()
                .defaultSuccessUrl(webuiUrl, true);
            http.logout()
                .logoutSuccessUrl(webuiUrl);
        }
    }

}