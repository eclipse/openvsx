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
package org.eclipse.openvsx.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import org.eclipse.openvsx.AbstractPostgresContainerTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ovsx.analytics.enabled defaults to false: no analytics beans exist and the endpoint is not
 * mapped, byte-for-byte current behavior. The property is pinned so this holds even in the
 * analytics-on test matrix run.
 */
@SpringBootTest(properties = "ovsx.analytics.enabled=false")
@AutoConfigureMockMvc
class DownloadAnalyticsDisabledTest extends AbstractPostgresContainerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    void testEndpointIsNotFoundWhenAnalyticsIsDisabled() throws Exception {
        mockMvc.perform(get("/api/foo/bar/analytics/downloads")).andExpect(status().isNotFound());
    }

    @Test
    void testNoAnalyticsBeansWhenDisabled() {
        assertTrue(context.getBeanNamesForType(DownloadAnalyticsRepository.class).length == 0);
        assertTrue(context.getBeanNamesForType(DownloadAnalyticsService.class).length == 0);
        assertTrue(context.getBeanNamesForType(DownloadAnalyticsAPI.class).length == 0);
    }
}
