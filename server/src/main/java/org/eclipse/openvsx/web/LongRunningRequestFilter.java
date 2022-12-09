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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LongRunningRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LongRunningRequestFilter.class);

    private final long threshold;
    private final StringBuilder builder;

    public LongRunningRequestFilter(long threshold) {
        this.threshold = threshold;
        this.builder = new StringBuilder();
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
        builder.append("\n\t")
                .append(request.getMethod())
                .append(" | ")
                .append(request.getRequestURI())
                .append(" took ")
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

        builder.append("Headers:");
        var headerNames = request.getHeaderNames().asIterator();
        while(headerNames.hasNext()) {
            var headerName = headerNames.next();
            var headerValues = new ArrayList<String>();
            var headers = request.getHeaders(headerName).asIterator();
            while(headers.hasNext()) {
                headerValues.add(headers.next());
            }

            builder.append("\n\t\t")
                    .append(headerName)
                    .append(": ")
                    .append(String.join(", ", headerValues));
        }

        builder.append("\n\tResponse: ")
                .append(response.getStatus());
        
        LOGGER.warn(builder.toString());
        builder.setLength(0);
    }
}
