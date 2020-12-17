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

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "Namespace",
    description = "Metadata of a namespace"
)
@JsonInclude(Include.NON_NULL)
public class NamespaceJson extends ResultJson {

    public static NamespaceJson error(String message) {
        var result = new NamespaceJson();
        result.error = message;
        return result;
    }

    @ApiModelProperty("Name of the namespace")
    @NotNull
    public String name;

    @ApiModelProperty("Map of extension names to their metadata URLs (not required for creating)")
    public Map<String, String> extensions;

    @ApiModelProperty("Indicates whether the namespace has an owner (not required for creating)")
    @NotNull
    public Boolean verified;

    @ApiModelProperty(value = "Access level of the namespace. Deprecated: namespaces are now always restricted", allowableValues = "public,restricted")
    @Deprecated
    public String access;

    @ApiModelProperty(hidden = true)
    public String membersUrl;

    @ApiModelProperty(hidden = true)
    public String roleUrl;

}