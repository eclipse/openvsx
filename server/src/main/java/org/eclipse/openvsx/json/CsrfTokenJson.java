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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;;

/**
 * A badge to be shown in the sidebar of the extension page in the marketplace.
 */
@JsonInclude(Include.NON_NULL)
public class CsrfTokenJson extends ResultJson {

    public static CsrfTokenJson error(String message) {
        var info = new CsrfTokenJson();
        info.error = message;
        return info;
    }

    public String value;

    public String header;

}