/********************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.dto;

import java.util.Objects;

public class NamespaceMembershipDTO {
    private final long id;
    private final String role;
    private final long namespaceId;
    private final long userId;

    public NamespaceMembershipDTO(long id, String role, long namespaceId, long userId) {
        this.id = id;
        this.role = role;
        this.namespaceId = namespaceId;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public long getNamespaceId() {
        return namespaceId;
    }

    public long getUserId() {
        return userId;
    }
}
