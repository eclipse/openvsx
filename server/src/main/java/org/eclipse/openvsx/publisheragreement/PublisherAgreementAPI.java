/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.publisheragreement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the agreement text from {@code ovsx.publisher-agreement.text}, any Spring resource location
 * (e.g. {@code classpath:agreement.md}, {@code file:/etc/agreement.md}). Point
 * {@code ovsx.webui.urls.publisher-agreement} at {@code /api/publisher-agreement}.
 */
@RestController
public class PublisherAgreementAPI {

    private final ResourceLoader resourceLoader;
    private final String location;

    public PublisherAgreementAPI(
            ResourceLoader resourceLoader,
            @Value("${ovsx.publisher-agreement.text:}") String location
    ) {
        this.resourceLoader = resourceLoader;
        this.location = location;
    }

    @GetMapping(path = "/api/publisher-agreement", produces = MediaType.TEXT_MARKDOWN_VALUE + ";charset=UTF-8")
    public ResponseEntity<Resource> getText() {
        if (location.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        var text = resourceLoader.getResource(location);
        if (!text.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noCache()).body(text);
    }
}
