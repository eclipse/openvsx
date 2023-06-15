/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class IntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TestService testService;

    private String apiCall(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    public void testPublishExtension() throws Exception {
        testService.createUser();
        createNamespace();
        publishExtension();

        // Wait a bit until the publish extension background job has finished
        Thread.sleep(15000);
        getExtensionMetadata();

        // Wait a bit until the new entry has landed in the search index
        Thread.sleep(2000);
        searchExtension();
    }

    private void createNamespace() {
        var requestBody = new NamespaceJson();
        requestBody.name = "Equinusocio";
        var response = restTemplate.postForEntity(apiCall("/api/-/namespace/create?token={token}"), requestBody,
                ResultJson.class, "test_token");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().error).isNull();
        assertThat(response.getBody().success).isEqualTo("Created namespace " + requestBody.name);
    }

    private void publishExtension() throws IOException {
        try (var stream = getClass().getResourceAsStream("vsc-material-theme.vsix")) {
            var bytes = stream.readAllBytes();
            var response = restTemplate.postForEntity(apiCall("/api/-/publish?token={token}"),
                    bytes, ExtensionJson.class, "test_token");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().error).isNull();
            assertThat(response.getBody().name).isEqualTo("vsc-material-theme");
        }
    }

    private void getExtensionMetadata() {
        var response = restTemplate.getForEntity(apiCall("/api/Equinusocio/vsc-material-theme"), ExtensionJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().description).isEqualTo("The most epic theme now for Visual Studio Code");
    }

    private void searchExtension() {
        var response = restTemplate.getForEntity(apiCall("/api/-/search?query=material"), SearchResultJson.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().extensions.size()).isEqualTo(1);
        assertThat(response.getBody().extensions.get(0).description)
                .isEqualTo("The most epic theme now for Visual Studio Code");
    }
    
}