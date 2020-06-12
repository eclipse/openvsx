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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;;

@JsonInclude(Include.NON_NULL)
public class NamespaceJson extends ResultJson {

    public static final String PUBLIC_ACCESS = "public";
    public static final String RESTRICTED_ACCESS = "restricted";

    public static NamespaceJson error(String message) {
        var result = new NamespaceJson();
        result.error = message;
        return result;
    }

    public String name;

    public Map<String, String> extensions;

    public String access;

    public String membersUrl;

    public String roleUrl;

}