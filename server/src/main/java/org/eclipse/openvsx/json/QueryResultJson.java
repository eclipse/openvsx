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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
    value = "QueryResult",
    description = "Metadata query result"
)
@JsonInclude(Include.NON_NULL)
public class QueryResultJson extends ResultJson {

    public static QueryResultJson error(String message) {
        var result = new QueryResultJson();
        result.error = message;
        return result;
    }

    @ApiModelProperty("Extensions that match the given query (may be empty)")
    public List<ExtensionJson> extensions;
    
}