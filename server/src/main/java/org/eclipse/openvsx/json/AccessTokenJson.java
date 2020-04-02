/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;;

@JsonInclude(Include.NON_NULL)
public class AccessTokenJson extends ResultJson {

    public static AccessTokenJson error(String message) {
        var result = new AccessTokenJson();
        result.error = message;
        return result;
    }

    public Long id;

    @Nullable
    public String value;

    public String createdTimestamp;

    @Nullable
    public String accessedTimestamp;

    public String description;

    public String deleteTokenUrl;

}