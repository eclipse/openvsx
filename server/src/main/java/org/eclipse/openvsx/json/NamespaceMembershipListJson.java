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

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;;

@Schema(
    name = "NamespaceMembershipList",
    description = "Metadata of a namespace member list"
)
@JsonInclude(Include.NON_NULL)
public class NamespaceMembershipListJson extends ResultJson {

    public static NamespaceMembershipListJson error(String message) {
        var result = new NamespaceMembershipListJson();
        result.error = message;
        return result;
    }

    @Schema(description = "List of memberships")
    @NotNull
    public List<NamespaceMembershipJson> namespaceMemberships;
}