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

import org.eclipse.openvsx.ratelimit.RateLimitService;
import org.eclipse.openvsx.ratelimit.UsageDataService;
import org.eclipse.openvsx.ratelimit.IdentityService;
import org.eclipse.openvsx.ratelimit.config.TieredRateLimitConfig;
import org.eclipse.openvsx.ratelimit.config.TieredRateLimitFilterProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(TieredRateLimitConfig.class)
public class TieredRateLimitServletFilterFactory {
    private final UsageDataService usageService;
    private final IdentityService identityService;
    private final RateLimitService rateLimitService;

    public TieredRateLimitServletFilterFactory(
        UsageDataService usageService,
        IdentityService identityService,
        RateLimitService rateLimitService
    ) {
        this.usageService = usageService;
        this.identityService = identityService;
        this.rateLimitService = rateLimitService;
    }

    public TieredRateLimitServletFilter create(TieredRateLimitFilterProperties filterProperties) {
        return new TieredRateLimitServletFilter(filterProperties, usageService, identityService, rateLimitService);
    }
}
