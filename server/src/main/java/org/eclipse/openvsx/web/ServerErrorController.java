/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.web;

import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@ConditionalOnProperty(value = "server.error.path", havingValue = "/server-error")
public class ServerErrorController extends BasicErrorController {

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    public ServerErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties) {
        super(errorAttributes, serverProperties.getError());
    }

    @RequestMapping(value = "/server-error", produces = {MediaType.TEXT_HTML_VALUE})
    public String handleError() {
        return "redirect:" + UrlUtil.createApiUrl(webuiUrl, "error");
    }
}
