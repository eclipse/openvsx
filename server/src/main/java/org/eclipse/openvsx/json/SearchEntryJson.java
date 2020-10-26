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
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "SearchEntry",
    description = "Summary of metadata of an extension"
)
@JsonInclude(Include.NON_NULL)
public class SearchEntryJson {

    @ApiModelProperty("URL to get the full metadata of the extension")
    @NotNull
    public String url;

    @ApiModelProperty("Map of file types (download, manifest, icon, readme, license, changelog) to their respective URLs")
    @NotNull
    public Map<String, String> files;

    @ApiModelProperty("Name of the extension")
    @NotNull
    public String name;

    @ApiModelProperty("Namespace of the extension")
    @NotNull
    public String namespace;

    @ApiModelProperty("The latest published version")
    @NotNull
    public String version;

    @ApiModelProperty("Date and time when this version was published (ISO-8601)")
    @NotNull
    public String timestamp;

    @ApiModelProperty("Essential metadata of all available versions")
    public List<VersionReference> allVersions;

    @ApiModelProperty(value = "Average rating", allowableValues = "range[0,5]")
    public Double averageRating;

    @ApiModelProperty("Number of downloads of the extension package")
    @Min(0)
    public int downloadCount;

    @ApiModelProperty("Name to be displayed in user interfaces")
    public String displayName;

    public String description;

    @ApiModel(
        value = "VersionReference",
        description = "Essential metadata of an extension version"
    )
    public static class VersionReference {

        @ApiModelProperty("URL to get the full metadata of this version")
        public String url;

        @ApiModelProperty("Map of file types (download, manifest, icon, readme, license, changelog) to their respective URLs")
        public Map<String, String> files;

        public String version;

        @ApiModelProperty("Map of engine names to the respective version constraints")
        public Map<String, String> engines;

    }

}