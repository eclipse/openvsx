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

import jakarta.validation.constraints.NotNull;
import org.eclipse.openvsx.entities.Customer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record ResolvedIdentity(
        @Nonnull String cacheKey,
        @Nullable Customer customer
) {

    public boolean isCustomer() {
        return customer != null;
    }

    public @NotNull Customer getCustomer() {
        if (isCustomer()) {
            return customer;
        } else {
            throw new RuntimeException("no customer associated with identity");
        }
    }
}
