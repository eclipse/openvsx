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
import java.util.List;

@Schema(
    name = "VersionReferences",
    description = "List of version references matching an extension"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VersionReferencesJson extends ResultJson{

    public static VersionReferencesJson error(String message) {
        var result = new VersionReferencesJson();
        result.error = message;
        return result;
    }

    @Schema(description = "Number of skipped entries according to the version references request")
    @NotNull
    @Min(0)
    public int offset;

    @Schema(description = "Total number of version references the extension has")
    @NotNull
    @Min(0)
    public int totalSize;

    @Schema(description = "Essential metadata of all available versions, limited to the size specified in the version references request")
    @NotNull
    public List<VersionReferenceJson> versions;
}
