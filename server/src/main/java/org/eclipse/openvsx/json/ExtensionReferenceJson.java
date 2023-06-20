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

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Objects;

@Schema(
    name = "ExtensionReference",
    description = "A reference to another extension in the registry"
)
@JsonInclude(Include.NON_NULL)
public class ExtensionReferenceJson implements Serializable {

    @Schema(description = "URL to get metadata of the referenced extension")
    @NotNull
    public String url;

    @Schema(description = "Namespace of the referenced extension")
    @NotNull
    public String namespace;

    @Schema(description = "Name of the referenced extension")
    @NotNull
    public String extension;

    @Schema(hidden = true)
    public String version;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionReferenceJson that = (ExtensionReferenceJson) o;
        return Objects.equals(url, that.url)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(extension, that.extension)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, namespace, extension, version);
    }
}
