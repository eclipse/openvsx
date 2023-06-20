/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LongRunningRequestFilter extends OncePerRequestFilter {

    private final long threshold;

    public LongRunningRequestFilter(long threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var hasJsonBody = MediaType.APPLICATION_JSON_VALUE.equals(request.getHeader(HttpHeaders.CONTENT_TYPE));
        var maxBytes = 1024;
        var contentLength = request.getHeader(HttpHeaders.CONTENT_LENGTH);
        boolean jsonBodyTooLong;
        try {
            jsonBodyTooLong = contentLength == null || Integer.parseInt(contentLength) > maxBytes;
        } catch (NumberFormatException e) {
            jsonBodyTooLong = true;
        }
        if(hasJsonBody && !jsonBodyTooLong) {
            request = new CachedBodyHttpServletRequest(request);
        }

        var stopWatch = new StopWatch();
        stopWatch.start();
        filterChain.doFilter(request, response);

        stopWatch.stop();
        if(stopWatch.getLastTaskTimeMillis() > threshold) {
            logWarning(request, response, stopWatch.getLastTaskTimeMillis(), maxBytes, hasJsonBody, jsonBodyTooLong);
        }
    }

    private void logWarning(HttpServletRequest request, HttpServletResponse response, long millis, long maxBytes, boolean hasJsonBody, boolean jsonBodyTooLong) throws IOException {
        var builder = new StringBuilder();
        builder.append("\n\t")
                .append(request.getMethod())
                .append(" | ")
                .append(request.getRequestURI());

        if(request.getQueryString() != null) {
            builder.append('?').append(request.getQueryString());
        }

        builder.append(" took ")
                .append(millis)
                .append(" ms.\n\t");

        if(hasJsonBody) {
            builder.append("Body: ");
            if(!jsonBodyTooLong) {
                builder.append(StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8)).append("\n\t");
            } else {
                builder.append("body exceeds ")
                        .append(maxBytes)
                        .append(" bytes\n\t");
            }
        }

        builder.append("\n\tResponse: ")
                .append(response.getStatus());
        
        logger.warn(builder.toString());
    }
}
