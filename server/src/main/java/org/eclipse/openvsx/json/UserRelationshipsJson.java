/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.List;

@JsonInclude(Include.NON_NULL)
public class UserRelationshipsJson {

    private UserJson user;
    private List<NamespaceDetailsJson> namespaces;

    public UserJson getUser() {
        return user;
    }

    public List<NamespaceDetailsJson> getNamespaces() {
        return namespaces;
    }
    
    public void setUser(UserJson user) {
        this.user = user;
    }

    public void setNamespaces(List<NamespaceDetailsJson> namespaces) {
        this.namespaces = namespaces;
    }
}
