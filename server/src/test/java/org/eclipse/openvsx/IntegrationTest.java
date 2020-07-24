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

import com.google.common.io.ByteStreams;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class IntegrationTest {

    @LocalServerPort
	int port;

	@Autowired
    TestRestTemplate restTemplate;

    @Autowired
    RepositoryService repositories;

    @Autowired
    TestService testService;

	@Test
	public void testEmptySearch() throws Exception {
        var response = restTemplate.getForEntity("http://localhost:" + port + "/api/-/search", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("{\"offset\":0,\"totalSize\":0,\"extensions\":[]}");
    }
    
    @Test
    public void testPublishExtension() throws Exception {
        testService.createUser();

        // Create the namespace
        var requestBody = new NamespaceJson();
        requestBody.name = "Equinusocio";
        var response1 = restTemplate.postForEntity("http://localhost:" + port + "/api/-/namespace/create?token={token}",
                    requestBody, ResultJson.class, "test_token");
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response1.getBody().error).isNull();
        assertThat(response1.getBody().success).isEqualTo("Created namespace " + requestBody.name);

        try (
            var stream = getClass().getResourceAsStream("vsc-material-theme.vsix");
        ) {
            // Publish the extension
            var bytes = ByteStreams.toByteArray(stream);
            var response2 = restTemplate.postForEntity("http://localhost:" + port + "/api/-/publish?token={token}",
                    bytes, ExtensionJson.class, "test_token");
            assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response2.getBody().error).isNull();
            assertThat(response2.getBody().name).isEqualTo("vsc-material-theme");
        }

        // Query the metadata of the published extension
        var response3 = restTemplate.getForEntity("http://localhost:" + port + "/api/Equinusocio/vsc-material-theme",
                ExtensionJson.class);
        assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response3.getBody().description).isEqualTo("The most epic theme now for Visual Studio Code");
    }
    
}