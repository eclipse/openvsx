/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BlockHostFilter extends OncePerRequestFilter {

    private final List<Pattern> blockedHosts;

    public BlockHostFilter(String[] blockedHosts) {
        this.blockedHosts = Stream.of(blockedHosts).map(Pattern::compile).toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var hostAndPort = UrlUtil.getBaseUrlHostAndPort(request);
        var host = hostAndPort.getFirst();
        var allow = true;
        for(var blockedHost : blockedHosts) {
            if(blockedHost.matcher(host).matches()) {
                allow = false;
                break;
            }
        }
        if(allow) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }
}
