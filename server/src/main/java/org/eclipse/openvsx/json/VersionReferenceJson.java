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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(
    name = "VersionReference",
    description = "Essential metadata of an extension version"
)
public class VersionReferenceJson {

    @Schema(description = "URL to get the full metadata of this version")
    public String url;

    @Schema(description = "Map of file types (download, manifest, icon, readme, license, changelog) to their respective URLs")
    public Map<String, String> files;

    public String version;

    @Schema(description = "Name of the target platform")
    public String targetPlatform;

    @Schema(description = "Map of engine names to the respective version constraints")
    public Map<String, String> engines;
}
