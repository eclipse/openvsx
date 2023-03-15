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

public class QueryRequest {

    public String namespaceName;

    public String extensionName;

    public String extensionVersion;

    public String extensionId;

    public String extensionUuid;

    public String namespaceUuid;

    public boolean includeAllVersions;

    public String targetPlatform;
}