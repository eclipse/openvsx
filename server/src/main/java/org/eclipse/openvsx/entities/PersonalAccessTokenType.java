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
package org.eclipse.openvsx.entities;

/**
 * The type of the personal access token.
 */
public enum PersonalAccessTokenType {
    /**
     * Long-lived personal access token (classic).
     */
    LLT(false, true),
    /**
     * One time usable general personal access token.
     * Legacy: was used until 1.2.0; but is not anymore.
     */
    @Deprecated
    OTT(true, false),
    /**
     * One time usable, trusted publishing issued access token.
     */
    TPT(true, false);

    private final boolean oneTime;
    private final boolean notify;

    PersonalAccessTokenType(boolean oneTime, boolean notify) {
        this.oneTime = oneTime;
        this.notify = notify;
    }

    public boolean isOneTime() {
        return oneTime;
    }

    public boolean isNotify() {
        return notify;
    }
}
