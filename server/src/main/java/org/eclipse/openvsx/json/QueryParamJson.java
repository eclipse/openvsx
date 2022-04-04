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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
    value = "QueryParam",
    description = "Metadata query parameter"
)
@JsonInclude(Include.NON_NULL)
public class QueryParamJson {

    @ApiModelProperty("Name of a namespace")
    public String namespaceName;

    @ApiModelProperty("Name of an extension")
    public String extensionName;

    @ApiModelProperty("Version of an extension")
    public String extensionVersion;

    @ApiModelProperty("Identifier in the form {namespace}.{extension}")
    public String extensionId;

    @ApiModelProperty("Universally unique identifier of an extension")
    public String extensionUuid;

    @ApiModelProperty("Universally unique identifier of a namespace")
    public String namespaceUuid;

    @ApiModelProperty("Whether to include all versions of an extension, ignored if extensionVersion is specified")
    public boolean includeAllVersions;

    @ApiModelProperty("Name of the target platform")
    public String targetPlatform;
}