/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Schema(
    name = "Versions",
    description = "Map of versions matching an extension"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionsJson extends ResultJson {

    public static VersionsJson error(String message) {
        var result = new VersionsJson();
        result.error = message;
        return result;
    }

    @Schema(description = "Number of skipped entries according to the versions request")
    @NotNull
    @Min(0)
    public int offset;

    @Schema(description = "Total number of versions the extension has")
    @NotNull
    @Min(0)
    public int totalSize;

    @Schema(description = "Map of versions, limited to the size specified in the versions request")
    @NotNull
    public Map<String, String> versions;
}