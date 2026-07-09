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
package org.eclipse.openvsx.trustedpublishing;

/**
 * Exception thrown when provider cannot resolve trustee.
 */
public class UnresolvableTrusteeException extends RuntimeException {
    public UnresolvableTrusteeException(String message) {
        super(message);
    }

    public UnresolvableTrusteeException(String message, Throwable cause) {
        super(message, cause);
    }
}
