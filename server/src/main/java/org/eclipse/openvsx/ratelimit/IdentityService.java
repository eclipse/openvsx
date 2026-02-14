/******************************************************************************
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
 *****************************************************************************/
package org.eclipse.openvsx.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.openvsx.ratelimit.config.RateLimitConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBean(RateLimitConfig.class)
public class IdentityService {

    private final TierService tierService;
    private final CustomerService customerService;

    public IdentityService(TierService tierService, CustomerService customerService) {
        this.tierService = tierService;
        this.customerService = customerService;
    }

    public ResolvedIdentity resolveIdentity(HttpServletRequest request) {
        String ipAddress = getIPAddress(request);
        String cacheKey = null;

        var token = request.getParameter("token");
        if (token != null) {
            // we use the token as a cache key
            // if the token is invalid, request will fail anyway
            cacheKey = "token_" + token.hashCode();
        }

        var customer = customerService.getCustomerByIpAddress(ipAddress);
        if (customer.isPresent() && cacheKey == null) {
            cacheKey = "customer_" + customer.get().getName();
        }

        if (cacheKey == null) {
            var session = request.getSession(false);
            var sessionId = session != null ? session.getId() : null;
            if (sessionId != null) {
                // we use the session id as a cache key
                // if the session is invalid, request will fail anyway
                cacheKey = "session_" + sessionId.hashCode();
            }
        }

        if (cacheKey == null) {
            cacheKey = "ip_" + ipAddress;
        }

        return new ResolvedIdentity(
                ipAddress,
                cacheKey,
                customer.orElse(null),
                tierService.getFreeTier().orElse(null),
                tierService.getSafetyTier().orElse(null)
        );
    }

    private String getIPAddress(HttpServletRequest request) {
        // TODO: make this configurable rather than hardcode,
        //       if the server is run without proxy, someone
        //       could fake the X-Forwarded-For header
        var forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor : request.getRemoteAddr();
    }
}
