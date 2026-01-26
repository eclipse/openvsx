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

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.openvsx.ratelimit.TieredRateLimitService;
import org.eclipse.openvsx.ratelimit.UsageDataService;
import org.eclipse.openvsx.ratelimit.IdentityService;
import org.eclipse.openvsx.ratelimit.config.TieredRateLimitFilterProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class TieredRateLimitServletFilter extends OncePerRequestFilter implements Ordered {

    private final Logger logger = LoggerFactory.getLogger(TieredRateLimitServletFilter.class);

    private final TieredRateLimitFilterProperties filterProperties;
    private final UsageDataService customerUsageService;
    private final IdentityService identityService;
    private final TieredRateLimitService rateLimitService;

    public TieredRateLimitServletFilter(
        TieredRateLimitFilterProperties filterProperties,
        UsageDataService customerUsageService,
        IdentityService identityService,
        TieredRateLimitService rateLimitService
    ) {
        this.filterProperties = filterProperties;
        this.customerUsageService = customerUsageService;
        this.identityService = identityService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !request.getRequestURI().matches(filterProperties.getUrl());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        logger.debug("rate limit filter: {}: {}", request.getRequestURI(), request.getRemoteAddr());

        var identity = identityService.resolveIdentity(request);

        if (identity.isCustomer()) {
            var customer = identity.getCustomer();
            logger.info("updating usage status for customer {}", customer.getName());
            customerUsageService.incrementUsage(customer);
        }

        var bucketConfiguration = BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(200L, Duration.ofMinutes(1L)))
                .build();

        var bucket = rateLimitService.getBucket(identity.cacheKey(), bucketConfiguration);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        logger.info(">>>>>>>> remainingTokens: {}", probe.getRemainingTokens());
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setContentType("text/plain");
            httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            httpResponse.setStatus(429);
            httpResponse.getWriter().append("Too many requests");
        }

//        boolean allConsumed = true;
//        Long remainingLimit = null;
//        for (var rl : filterConfig.getRateLimitChecks()) {
//            var wrapper = rl.rateLimit(new ExpressionParams<>(request), null);
//            if (wrapper != null && wrapper.getRateLimitResult() != null) {
//                var rateLimitResult = wrapper.getRateLimitResult();
//                if (rateLimitResult.isConsumed()) {
//                    remainingLimit = RateLimitService.getRemainingLimit(remainingLimit, rateLimitResult);
//                } else {
//                    allConsumed = false;
//                    handleHttpResponseOnRateLimiting(response, rateLimitResult);
//                    break;
//                }
//                if (filterConfig.getStrategy().equals(RateLimitConditionMatchingStrategy.FIRST)) {
//                    break;
//                }
//            }
//        }
//
//        if (allConsumed) {
//            if (remainingLimit != null && Boolean.FALSE.equals(filterConfig.getHideHttpResponseHeaders())) {
//                logger.debug("add-x-rate-limit-remaining-header;limit:{}", remainingLimit);
//                response.setHeader("X-Rate-Limit-Remaining", "" + remainingLimit);
//            }
//            chain.doFilter(request, response);
//            filterConfig.getPostRateLimitChecks()
//                    .forEach(rlc -> {
//                        var result = rlc.rateLimit(request, response);
//                        if (result != null) {
//                            logger.debug("post-rate-limit;remaining-tokens:{}", result.getRateLimitResult().getRemainingTokens());
//                        }
//                    });
//        }
    }

//    private void handleHttpResponseOnRateLimiting(HttpServletResponse httpResponse, RateLimitResult rateLimitResult) throws IOException {
//        httpResponse.setStatus(filterConfig.getHttpStatusCode().value());
//        if (Boolean.FALSE.equals(filterConfig.getHideHttpResponseHeaders())) {
//            httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(rateLimitResult.getNanosToWaitForRefill()));
//            filterConfig.getHttpResponseHeaders().forEach(httpResponse::setHeader);
//        }
//        if (filterConfig.getHttpResponseBody() != null) {
//            httpResponse.setContentType(filterConfig.getHttpContentType());
//            httpResponse.getWriter().append(filterConfig.getHttpResponseBody());
//        }
//    }

    @Override
    public int getOrder() {
        return filterProperties.getFilterOrder();
    }
}
