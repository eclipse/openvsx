/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "ExtensionReference",
    description = "A reference to another extension in the registry"
)
@JsonInclude(Include.NON_NULL)
public class ExtensionReferenceJson {

    @ApiModelProperty("URL to get metadata of the referenced extension")
    @NotNull
    public String url;

    @ApiModelProperty("Namespace of the referenced extension")
    @NotNull
    public String namespace;

    @ApiModelProperty("Name of the referenced extension")
    @NotNull
    public String extension;

    @ApiModelProperty(hidden = true)
    public String version;

}