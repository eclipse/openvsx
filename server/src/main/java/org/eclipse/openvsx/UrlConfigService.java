/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A single point of configuration of URLs to upstream.
 */
@Component
public class UrlConfigService {

    @Value("${ovsx.upstream.url:}")
    String upstreamUrl;

    @Value("${ovsx.vscode.upstream.gallery-url:}")
    String upstreamGalleryUrl;

    @Value("${ovsx.data.mirror.server-url:}")
    String mirrorServerUrl;

    public String getUpstreamUrl() {
        return upstreamUrl;
    }

    public String  getUpstreamGalleryUrl() {
        return upstreamGalleryUrl;
    }

    public String getMirrorServerUrl() {
        return mirrorServerUrl;
    }

}
