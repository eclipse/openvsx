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

import java.util.Map;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
    name = "Namespace",
    description = "Metadata of a namespace"
)
@JsonInclude(Include.NON_NULL)
public class NamespaceJson extends ResultJson {

    public static NamespaceJson error(String message) {
        var result = new NamespaceJson();
        result.setError(message);
        return result;
    }

    @Schema(description = "Name of the namespace")
    @NotNull
    private String name;

    @Schema(description = "Map of extension names to their metadata URLs (not required for creating)")
    private Map<String, String> extensions;

    @Schema(description = "Indicates whether the namespace has an owner (not required for creating)")
    @NotNull
    private Boolean verified;

    @Schema(
        description = "Access level of the namespace. Deprecated: namespaces are now always restricted",
        allowableValues = {"public", "restricted"}
    )
    @Deprecated
    private String access;

    @Schema(hidden = true)
    private String membersUrl;

    @Schema(hidden = true)
    private String roleUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, String> extensions) {
        this.extensions = extensions;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getAccess() {
        return access;
    }

    public void setAccess(String access) {
        this.access = access;
    }

    public String getMembersUrl() {
        return membersUrl;
    }

    public void setMembersUrl(String membersUrl) {
        this.membersUrl = membersUrl;
    }

    public String getRoleUrl() {
        return roleUrl;
    }

    public void setRoleUrl(String roleUrl) {
        this.roleUrl = roleUrl;
    }
}