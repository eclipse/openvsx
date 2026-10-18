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
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One signing in a user's publisher agreement history; {@code revokedTimestamp} is absent while it stands.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublisherAgreementEntryJson(int version, String signedTimestamp, String revokedTimestamp) {}
