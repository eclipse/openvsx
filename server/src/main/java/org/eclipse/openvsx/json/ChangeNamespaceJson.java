/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * Used to change a namespace
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeNamespaceJson {
    public String oldNamespace;
    public String newNamespace;
    public boolean removeOldNamespace;
    public boolean mergeIfNewNamespaceAlreadyExists;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeNamespaceJson that = (ChangeNamespaceJson) o;
        return removeOldNamespace == that.removeOldNamespace && mergeIfNewNamespaceAlreadyExists == that.mergeIfNewNamespaceAlreadyExists && Objects.equals(oldNamespace, that.oldNamespace) && Objects.equals(newNamespace, that.newNamespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldNamespace, newNamespace, removeOldNamespace, mergeIfNewNamespaceAlreadyExists);
    }
}
