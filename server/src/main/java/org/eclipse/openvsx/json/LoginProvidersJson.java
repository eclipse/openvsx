/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginProvidersJson extends ResultJson {
    private Map<String,String> loginProviders;

    public Map<String, String> getLoginProviders() {
        return loginProviders;
    }

    public void setLoginProviders(Map<String, String> loginProviders) {
        this.loginProviders = loginProviders;
    }
}
