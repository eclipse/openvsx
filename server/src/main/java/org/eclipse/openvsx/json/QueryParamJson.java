/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
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

import static org.eclipse.openvsx.util.TargetPlatform.*;

@Schema(
    name = "QueryParam",
    description = "Metadata query parameter"
)
@JsonInclude(Include.NON_NULL)
public class QueryParamJson {

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

    @Schema(description = "Whether to include all versions of an extension, ignored if extensionVersion is specified")
    public boolean includeAllVersions;

    @Schema(description = "Name of the target platform", allowableValues = {
        NAME_WIN32_X64, NAME_WIN32_IA32, NAME_WIN32_ARM64,
        NAME_LINUX_X64, NAME_LINUX_ARM64, NAME_LINUX_ARMHF,
        NAME_ALPINE_X64, NAME_ALPINE_ARM64,
        NAME_DARWIN_X64, NAME_DARWIN_ARM64,
        NAME_WEB, NAME_UNIVERSAL
    })
    public String targetPlatform;
}