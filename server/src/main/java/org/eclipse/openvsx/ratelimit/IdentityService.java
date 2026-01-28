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
package org.eclipse.openvsx.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class IdentityService {

    private final CustomerService customerService;

    public IdentityService(CustomerService customerService) {
        this.customerService = customerService;
    }

    public ResolvedIdentity resolveIdentity(HttpServletRequest request) {
        String ipAddress = getIPAddress(request);

        String cacheKey = null;

        var token = request.getParameter("token");
        if (token != null) {
            // TODO: check if its a valid access token
            cacheKey = "token_" + token.hashCode();
        }

        var customer = customerService.getCustomerByIpAddress(ipAddress);
        if (customer.isPresent() && cacheKey != null) {
            cacheKey = "customer" + customer.get().getId();
        }

        if (cacheKey == null) {
            var session = request.getSession(false);
            var sessionId = session != null ? session.getId() : null;
            if (sessionId != null) {
                // TODO: check if its an active session
                cacheKey = "session_" + sessionId.hashCode();
            }
        }

        if (cacheKey == null) {
            cacheKey = "ip_" + ipAddress;
        }

        return new ResolvedIdentity(cacheKey, customer.orElse(null));
    }

    private String getIPAddress(HttpServletRequest request) {
        // TODO: make this configurable rather than hardcode,
        //       if the server is run without proxy, someone
        //       could fake the X-Forwarded-For header
        var forwardedFor = request.getHeader("X-Forwarded-For");
        return forwardedFor != null ? forwardedFor : request.getRemoteAddr();
    }
}
