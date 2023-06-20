/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
public class MirrorExtensionHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    DataMirrorService dataMirror;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var params = request.getRequestURI().equals("/vscode/item")
                ? extractQueryParams(request)
                : extractPathParams(request);
        var namespaceName = (String) params.get("namespaceName");
        var extensionName = (String) params.get("extensionName");
        if (!dataMirror.match(namespaceName, extensionName)) {
            response.reset();
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        return true;
    }

    private Map<String, String> extractQueryParams(HttpServletRequest request) {
        var itemName = request.getParameter("itemName");
        var itemNamePieces = itemName.split("\\.");
        return itemNamePieces.length == 2
                ? Map.of("namespaceName", itemNamePieces[0], "extensionName", itemNamePieces[1])
                : Collections.emptyMap();
    }

    private Map<String, String> extractPathParams(HttpServletRequest request) {
        return new TreeMap<>((Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
    }
}
