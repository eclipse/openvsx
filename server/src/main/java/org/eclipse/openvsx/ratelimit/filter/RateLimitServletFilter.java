/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.openvsx.ratelimit.filter;

import com.giffing.bucket4j.spring.boot.starter.context.ExpressionParams;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimitConditionMatchingStrategy;
import com.giffing.bucket4j.spring.boot.starter.context.RateLimitResult;
import com.giffing.bucket4j.spring.boot.starter.context.properties.FilterConfiguration;
import com.giffing.bucket4j.spring.boot.starter.filter.servlet.ServletRateLimitFilter;
import com.giffing.bucket4j.spring.boot.starter.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.openvsx.cache.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class RateLimitServletFilter extends OncePerRequestFilter implements ServletRateLimitFilter {

    private final Logger logger = LoggerFactory.getLogger(RateLimitServletFilter.class);

    private CacheService cacheService;
    private FilterConfiguration<HttpServletRequest, HttpServletResponse> filterConfig;

    public RateLimitServletFilter(
        CacheService cacheService,
        FilterConfiguration<HttpServletRequest, HttpServletResponse> filterConfig
    ) {
        this.cacheService = cacheService;
        this.filterConfig = filterConfig;
    }

    @Override
    public void setFilterConfig(FilterConfiguration<HttpServletRequest, HttpServletResponse> filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getRequestURI().matches(filterConfig.getUrl());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        logger.debug("rate limit filter: {}: {}", request.getRequestURI(), request.getRemoteAddr());

        var instant = Instant.now();
        var epochMinute = instant.getEpochSecond() / 60;
        var window = epochMinute / 5 * 5;
        var old = cacheService.incrementCustomerUsage(request.getRemoteAddr() + ":" + window, 1);
        System.out.println(old);

        boolean allConsumed = true;
        Long remainingLimit = null;
        for (var rl : filterConfig.getRateLimitChecks()) {
            var wrapper = rl.rateLimit(new ExpressionParams<>(request), null);
            if (wrapper != null && wrapper.getRateLimitResult() != null) {
                var rateLimitResult = wrapper.getRateLimitResult();
                if (rateLimitResult.isConsumed()) {
                    remainingLimit = RateLimitService.getRemainingLimit(remainingLimit, rateLimitResult);
                } else {
                    allConsumed = false;
                    handleHttpResponseOnRateLimiting(response, rateLimitResult);
                    break;
                }
                if (filterConfig.getStrategy().equals(RateLimitConditionMatchingStrategy.FIRST)) {
                    break;
                }
            }
        }

        if (allConsumed) {
            if (remainingLimit != null && Boolean.FALSE.equals(filterConfig.getHideHttpResponseHeaders())) {
                logger.debug("add-x-rate-limit-remaining-header;limit:{}", remainingLimit);
                response.setHeader("X-Rate-Limit-Remaining", "" + remainingLimit);
            }
            chain.doFilter(request, response);
            filterConfig.getPostRateLimitChecks()
                    .forEach(rlc -> {
                        var result = rlc.rateLimit(request, response);
                        if (result != null) {
                            logger.debug("post-rate-limit;remaining-tokens:{}", result.getRateLimitResult().getRemainingTokens());
                        }
                    });
        }
    }

    private void handleHttpResponseOnRateLimiting(HttpServletResponse httpResponse, RateLimitResult rateLimitResult) throws IOException {
        httpResponse.setStatus(filterConfig.getHttpStatusCode().value());
        if (Boolean.FALSE.equals(filterConfig.getHideHttpResponseHeaders())) {
            httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(rateLimitResult.getNanosToWaitForRefill()));
            filterConfig.getHttpResponseHeaders().forEach(httpResponse::setHeader);
        }
        if (filterConfig.getHttpResponseBody() != null) {
            httpResponse.setContentType(filterConfig.getHttpContentType());
            httpResponse.getWriter().append(filterConfig.getHttpResponseBody());
        }
    }

    @Override
    public int getOrder() {
        return filterConfig.getOrder();
    }
}
