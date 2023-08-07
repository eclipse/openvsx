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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(
    name = "SearchEntry",
    description = "Summary of metadata of an extension"
)
@JsonInclude(Include.NON_NULL)
public class SearchEntryJson implements Serializable {

    @Schema(description = "URL to get the full metadata of the extension")
    @NotNull
    public String url;

    @Schema(description = "Map of file types (download, manifest, icon, readme, license, changelog) to their respective URLs")
    @NotNull
    public Map<String, String> files;

    @Schema(description = "Name of the extension")
    @NotNull
    public String name;

    @Schema(description = "Namespace of the extension")
    @NotNull
    public String namespace;

    @Schema(description = "The latest published version")
    @NotNull
    public String version;

    @Schema(description = "Date and time when this version was published (ISO-8601)")
    @NotNull
    public String timestamp;

    @Schema(description = "Essential metadata of all available versions. Deprecated: only returns the last 100 versions. Use allVersionsUrl instead.")
    @Deprecated
    public List<VersionReferenceJson> allVersions;

    @Schema(description = "URL to get essential metadata of all available versions.")
    public String allVersionsUrl;

    @Schema(description = "Average rating")
    @Min(0)
    @Max(5)
    public Double averageRating;

    @Schema(description = "Number of reviews")
    @Min(0)
    public Long reviewCount;

    @Schema(description = "Number of downloads of the extension package")
    @Min(0)
    public int downloadCount;

    @Schema(description = "Name to be displayed in user interfaces")
    public String displayName;

    public String description;
}