/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "QueryParamV2",
    description = "Metadata query parameter version 2"
)
@JsonInclude(Include.NON_NULL)
public class QueryParamJsonV2 {

    @Schema(description = "Name of a namespace")
    public String namespaceName;

    @Schema(description = "Name of an extension")
    public String extensionName;

    @Schema(description = "Version of an extension")
    public String extensionVersion;

    @Schema(description = "Identifier in the form {namespace}.{extension}")
    public String extensionId;

    @Schema(description = "Universally unique identifier of an extension")
    public String extensionUuid;

    @Schema(description = "Universally unique identifier of a namespace")
    public String namespaceUuid;

    @Schema(description = "Whether to include all versions of an extension", allowableValues = { "true", "false", "links" })
    public String includeAllVersions;

    @Schema(description = "Name of the target platform")
    public String targetPlatform;
}