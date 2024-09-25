/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
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

import java.io.Serializable;

@Schema(
        name = "ExtensionReplacement",
        description = "Metadata of an extension replacement"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExtensionReplacementJson implements Serializable {

    @Schema(description = "URL of the extension replacement")
    public String url;

    @Schema(description = "Name to be displayed in user interfaces")
    public String displayName;
}
