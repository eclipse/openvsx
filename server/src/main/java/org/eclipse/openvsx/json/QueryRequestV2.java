/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

public class QueryRequestV2 {

    public String namespaceName;

    public String extensionName;

    public String extensionVersion;

    public String extensionId;

    public String extensionUuid;

    public String namespaceUuid;

    public String includeAllVersions;

    public String targetPlatform;

    public int size;

    public int offset;
}