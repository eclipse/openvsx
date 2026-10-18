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

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class PublisherAgreementAPITest {

    private PublisherAgreementAPI api(String location) {
        return new PublisherAgreementAPI(new DefaultResourceLoader(), location);
    }

    @Test
    void servesTheConfiguredText() throws Exception {
        var response = api("classpath:publisheragreement/agreement.md").getText();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(response.getBody().getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains("Test Publisher Agreement");
    }

    @Test
    void notFoundWhenNotConfigured() {
        assertThat(api("").getText().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void notFoundWhenTheResourceIsMissing() {
        assertThat(api("classpath:publisheragreement/missing.md").getText().getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
