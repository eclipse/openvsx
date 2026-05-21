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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Used to revoke publishers in bulk
 */
@Schema(
        name = "BulkPublisherRevokeRequest",
        description = "List of publishers to revoke contributions for"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BulkPublisherRevokeRequestJson(
        List<PublisherRevocationTargetJson> publishers,
        String reason
) {}
