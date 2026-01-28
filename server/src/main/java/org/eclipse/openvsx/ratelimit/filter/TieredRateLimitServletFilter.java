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

import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.openvsx.ratelimit.RateLimitService;
import org.eclipse.openvsx.ratelimit.UsageDataService;
import org.eclipse.openvsx.ratelimit.IdentityService;
import org.eclipse.openvsx.ratelimit.config.TieredRateLimitFilterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TieredRateLimitServletFilter extends OncePerRequestFilter implements Ordered {

    private final Logger logger = LoggerFactory.getLogger(TieredRateLimitServletFilter.class);

    private final TieredRateLimitFilterProperties filterProperties;
    private final UsageDataService customerUsageService;
    private final IdentityService identityService;
    private final RateLimitService rateLimitService;

    public TieredRateLimitServletFilter(
        TieredRateLimitFilterProperties filterProperties,
        UsageDataService customerUsageService,
        IdentityService identityService,
        RateLimitService rateLimitService
    ) {
        this.filterProperties = filterProperties;
        this.customerUsageService = customerUsageService;
        this.identityService = identityService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().matches(filterProperties.getUrl());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        logger.debug("rate limit filter: {}: {}", request.getRequestURI(), request.getRemoteAddr());

        var identity = identityService.resolveIdentity(request);

        if (identity.isCustomer()) {
            var customer = identity.getCustomer();
            logger.info("tiered-rate-limit: updating usage status for customer {}", customer.getName());
            customerUsageService.incrementUsage(customer);
        }

        var bucket = rateLimitService.getBucket(identity);

        // TODO: return ratelimit from service for bucket
        // response.setHeader("X-RateLimit-Limit", Long.toString(100L));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        logger.info(">>>>>>>> remainingTokens: {}", probe.getRemainingTokens());
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", Long.toString(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            handleHttpResponseOnRateLimiting(response, probe);
        }
    }

    private void handleHttpResponseOnRateLimiting(HttpServletResponse response, ConsumptionProbe probe) throws IOException {
        response.setStatus(filterProperties.getHttpStatusCode().value());

        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-Rate-Limit-Reset", "" + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForReset()));
        var refillInSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
        response.setHeader("X-Rate-Limit-Retry-After-Seconds", Long.toString(refillInSeconds));
        response.setHeader("Retry-After", Long.toString(refillInSeconds));

        filterProperties.getHttpResponseHeaders().forEach(response::setHeader);
        if (filterProperties.getHttpResponseBody() != null) {
            response.setContentType(filterProperties.getHttpContentType());
            response.getWriter().append(filterProperties.getHttpResponseBody());
        }
    }

    @Override
    public int getOrder() {
        return filterProperties.getFilterOrder();
    }
}
