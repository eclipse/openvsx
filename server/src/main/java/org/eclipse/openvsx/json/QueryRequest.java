/********************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import java.util.Objects;

public class QueryRequest {

    public String namespaceName;

    public String extensionName;

    public String extensionVersion;

    public String extensionId;

    public String extensionUuid;

    public String namespaceUuid;

    public boolean includeAllVersions;

    public String targetPlatform;

    public int size;

    public int offset;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueryRequest that = (QueryRequest) o;
        return includeAllVersions == that.includeAllVersions
                && size == that.size
                && offset == that.offset
                && Objects.equals(namespaceName, that.namespaceName)
                && Objects.equals(extensionName, that.extensionName)
                && Objects.equals(extensionVersion, that.extensionVersion)
                && Objects.equals(extensionId, that.extensionId)
                && Objects.equals(extensionUuid, that.extensionUuid)
                && Objects.equals(namespaceUuid, that.namespaceUuid)
                && Objects.equals(targetPlatform, that.targetPlatform);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespaceName, extensionName, extensionVersion, extensionId, extensionUuid, namespaceUuid,
                includeAllVersions, targetPlatform, size, offset);
    }
}