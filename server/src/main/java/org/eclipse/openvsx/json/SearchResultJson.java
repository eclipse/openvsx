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

import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "SearchResult",
    description = "List of extensions matching a search query"
)
@JsonInclude(Include.NON_NULL)
public class SearchResultJson extends ResultJson {

    public static SearchResultJson error(String message) {
        var result = new SearchResultJson();
        result.error = message;
        return result;
    }

    @ApiModelProperty("Number of skipped entries according to the search query")
    @NotNull
    @Min(0)
    public int offset;

    @ApiModelProperty("Total number of entries that match the search query")
    @NotNull
    @Min(0)
    public int totalSize;

    @ApiModelProperty("List of matching entries, limited to the size specified in the search query")
    @NotNull
    public List<SearchEntryJson> extensions;

}