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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(
    name = "SearchEntry",
    description = "Summary of metadata of an extension"
)
@JsonInclude(Include.NON_NULL)
public class SearchEntryJson {

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

    @Schema(description = "Essential metadata of all available versions")
    public List<VersionReference> allVersions;

    @Schema(description = "Average rating")
    @Min(0)
    @Max(5)
    public Double averageRating;

    @Schema(description = "Number of downloads of the extension package")
    @Min(0)
    public int downloadCount;

    @Schema(description = "Name to be displayed in user interfaces")
    public String displayName;

    public String description;

    @Schema(
        name = "VersionReference",
        description = "Essential metadata of an extension version"
    )
    public static class VersionReference {

        @Schema(description = "URL to get the full metadata of this version")
        public String url;

        @Schema(description = "Map of file types (download, manifest, icon, readme, license, changelog) to their respective URLs")
        public Map<String, String> files;

        public String version;

        @Schema(description = "Map of engine names to the respective version constraints")
        public Map<String, String> engines;

    }

}