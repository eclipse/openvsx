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
        var forwardedFor = request.getHeader("X-Forwarded-For");
        var ipAddress = forwardedFor != null ? forwardedFor : request.getRemoteAddr();

        var token = request.getParameter("token");
        if (token != null) {

        }

        var session = request.getSession(false);
        var sessionId = session != null ? session.getId() : null;

        var customer = customerService.getCustomerByIpAddress(ipAddress);
        if (customer.isPresent()) {
            return ResolvedIdentity.ofCustomer(customer.get());
        }


        return ResolvedIdentity.anonymous(ipAddress);
    }
}
