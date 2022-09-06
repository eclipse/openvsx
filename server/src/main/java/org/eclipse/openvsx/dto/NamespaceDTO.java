/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.dto;

import java.io.Serializable;
import java.util.Objects;

public class NamespaceDTO implements Serializable {

    private final long id;
    private final String publicId;
    private final String name;

    public NamespaceDTO(long id, String publicId, String name) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NamespaceDTO that = (NamespaceDTO) o;
        return id == that.id
                && Objects.equals(publicId, that.publicId)
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publicId, name);
    }
}
