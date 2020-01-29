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

import javax.annotation.Nullable;

public class SearchEntryJson {

    public String url;

    public String downloadUrl;

    @Nullable
    public String iconUrl;

    public String name;

    public String publisher;

    public String version;

    public String timestamp;

    @Nullable
    public Double averageRating;

    @Nullable
    public String displayName;

    @Nullable
    public String description;

}