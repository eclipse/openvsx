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

import javax.annotation.Nullable;

public class SearchEntryJson {

    public String url;

    // key: file type (see constants in FileResource), value: url
    public Map<String, String> files;

    public String name;

    public String namespace;

    public String version;

    public String timestamp;

    @Nullable
    public Double averageRating;

    public int downloadCount;

    @Nullable
    public String displayName;

    @Nullable
    public String description;

}