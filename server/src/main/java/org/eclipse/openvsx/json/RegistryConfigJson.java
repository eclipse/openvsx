/********************************************************************************
 * Copyright (c) 2024 STMicroelectronics and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "RegistryConfig",
    description = "Configuration of the registry service"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistryConfigJson extends ResultJson {
    public static RegistryConfigJson error(String message) {
        var result = new RegistryConfigJson();
        result.setError(message);
        return result;
    }

    @Schema(description = "Registry version")
    @NotNull
    private String version;

    @Schema(description = "Maximum allowed extension package size in bytes")
    private long maxExtensionSize;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getMaxExtensionSize() {
        return maxExtensionSize;
    }

    public void setMaxExtensionSize(long maxExtensionSize) {
        this.maxExtensionSize = maxExtensionSize;
    }
}
