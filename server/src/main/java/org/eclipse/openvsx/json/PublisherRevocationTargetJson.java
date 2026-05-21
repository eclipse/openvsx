/** ******************************************************************************
 * Copyright (c) 2026 Eclipse Foundation AISBL.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "PublisherRevocationTarget",
        description = "Coordinate for a publisher that should be have its' contributions revoked"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PublisherRevocationTargetJson(
        String loginName,
        String provider
) {}