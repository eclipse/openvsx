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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "Result",
    description = "Generic result indicator",
    subTypes = {
        ExtensionJson.class, NamespaceJson.class, ReviewListJson.class, SearchResultJson.class, UserJson.class
    }
)
@JsonInclude(Include.NON_NULL)
public class ResultJson {

    public static ResultJson success(String message) {
        var result = new ResultJson();
        result.success = message;
        return result;
    }

    public static ResultJson warning(String message) {
        var result = new ResultJson();
        result.warning = message;
        return result;
    }

    public static ResultJson error(String message) {
        var result = new ResultJson();
        result.error = message;
        return result;
    }

    @ApiModelProperty("Indicates success of the operation (omitted if a more specific result type is returned)")
    public String success;

    @ApiModelProperty("Indicates a warning; when this is present, other properties can still be used")
    public String warning;

    @ApiModelProperty("Indicates an error; when this is present, all other properties should be ignored")
    public String error;

}