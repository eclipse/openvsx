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

import static org.eclipse.openvsx.entities.FileResource.*;
import static org.eclipse.openvsx.entities.FileResource.CHANGELOG;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;

import net.javacrumbs.shedlock.core.LockProvider;
import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.security.OAuth2UserServices;
import org.eclipse.openvsx.security.TokenService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.AzureDownloadCountService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(RegistryAPI.class)
@AutoConfigureWebClient
@MockBean({
    ClientRegistrationRepository.class, UpstreamRegistryService.class, GoogleCloudStorageService.class,
    AzureBlobStorageService.class, VSCodeIdService.class, AzureDownloadCountService.class, LockProvider.class
})
public class RegistryAPITest {

    @SpyBean
    UserService users;

    @MockBean
    RepositoryService repositories;

    @MockBean
    SearchUtilService search;

    @MockBean
    EntityManager entityManager;

    @MockBean
    EclipseService eclipse;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExtensionService extensionService;

    @Test
    public void testPublicNamespace() throws Exception {
        var namespace = mockNamespace();
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(0l);
        
        mockMvc.perform(get("/api/{namespace}", "foobar"))
                .andExpect(status().isOk())
                .andExpect(content().json(namespaceJson(n -> {
                    n.name = "foobar";
                    n.verified = false;
                })));
    }

    @Test
    public void testVerifiedNamespace() throws Exception {
        var namespace = mockNamespace();
        var user = new UserData();
        user.setLoginName("test_user");
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(1l);

        mockMvc.perform(get("/api/{namespace}", "foobar"))
                .andExpect(status().isOk())
                .andExpect(content().json(namespaceJson(n -> {
                    n.name = "foobar";
                    n.verified = true;
                })));
    }

    @Test
    public void testUnknownNamespace() throws Exception {
        mockNamespace();
        mockMvc.perform(get("/api/{namespace}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Namespace not found: unknown")));
    }

    @Test
    public void testExtension() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}", "foo", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testInactiveExtension() throws Exception {
        var extVersion = mockExtension();
        extVersion.setActive(false);
        extVersion.getExtension().setActive(false);

        mockMvc.perform(get("/api/{namespace}/{extension}", "foo", "bar"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar")));
    }

    @Test
    public void testUnknownExtension() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}", "foo", "baz"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.baz")));
    }

    @Test
    public void testExtensionVersion() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testLatestExtensionVersion() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "latest"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testInactiveExtensionVersion() throws Exception {
        var extVersion = mockExtension();
        extVersion.setActive(false);

        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar version 1")));
    }

    @Test
    public void testUnknownExtensionVersion() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "2"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar version 2")));
    }

    @Test
    public void testReadme() throws Exception {
        mockReadme();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1", "README"))
                .andExpect(status().isOk())
                .andExpect(content().string("Please read me"));
    }

    @Test
    public void testChangelog() throws Exception {
        mockChangelog();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1", "CHANGELOG"))
                .andExpect(status().isOk())
                .andExpect(content().string("All notable changes is documented here"));
    }

    @Test
    public void testLicense() throws Exception {
        mockLicense();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1", "LICENSE"))
                .andExpect(status().isOk())
                .andExpect(content().string("I never broke the Law! I am the law!"));
    }

    @Test
    public void testInactiveFile() throws Exception {
        var extVersion = mockExtension();
        extVersion.setActive(false);

        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1", "README"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUnknownFile() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1", "unknown.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testReviews() throws Exception {
        mockReviews();
        mockMvc.perform(get("/api/{namespace}/{extension}/reviews", "foo", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(reviewsJson(rs -> {
                    var u1 = new UserJson();
                    u1.loginName = "user1";
                    var r1 = new ReviewJson();
                    r1.user = u1;
                    r1.rating = 3;
                    r1.comment = "Somewhat ok";
                    r1.timestamp = "2000-01-01T10:00Z";
                    rs.reviews.add(r1);
                    var u2 = new UserJson();
                    u2.loginName = "user2";
                    var r2 = new ReviewJson();
                    r2.user = u2;
                    r2.rating = 4;
                    r2.comment = "Quite good";
                    r2.timestamp = "2000-01-01T10:00Z";
                    rs.reviews.add(r2);
                })));
    }

    @Test
    public void testSearch() throws Exception {
        mockSearch();
        mockMvc.perform(get("/api/-/search?query={query}&size={size}&offset={offset}", "foo", "10", "0"))
                .andExpect(status().isOk())
                .andExpect(content().json(searchJson(s -> {
                    s.offset = 0;
                    s.totalSize = 1;
                    var e1 = new SearchEntryJson();
                    e1.namespace = "foo";
                    e1.name = "bar";
                    e1.version = "1";
                    e1.timestamp = "2000-01-01T10:00Z";
                    e1.displayName = "Foo Bar";
                    s.extensions.add(e1);
                })));
    }

    @Test
    public void testSearchInactive() throws Exception {
        var extensions = mockSearch();
        extensions.forEach(extension -> {
            extension.setActive(false);
            extension.getLatest().setActive(false);
        });

        mockMvc.perform(get("/api/-/search?query={query}&size={size}&offset={offset}", "foo", "10", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"offset\":0,\"totalSize\":1,\"extensions\":[]}"));
    }

    @Test
    public void testGetQueryExtensionName() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(get("/api/-/query?extensionName={extensionName}", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryNamespace() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(get("/api/-/query?namespaceName={namespaceName}", "foo"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryUnknownExtension() throws Exception {
        mockExtensionVersionDTO();
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionName("baz"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/-/query?extensionName={extensionName}", "baz"))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testGetQueryInactiveExtension() throws Exception {
        var namespaceName = "foo";
        var extensionName = "bar";

        mockInactiveExtensionVersionDTO(namespaceName, extensionName);
        mockMvc.perform(get("/api/-/query?extensionId={namespaceName}.{extensionName}", namespaceName, extensionName))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testGetQueryExtensionId() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(get("/api/-/query?extensionId={extensionId}", "foo.bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryExtensionUuid() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(get("/api/-/query?extensionUuid={extensionUuid}", "5678"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryNamespaceUuid() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(get("/api/-/query?namespaceUuid={namespaceUuid}", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testPostQueryExtensionName() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionName\": \"bar\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testPostQueryNamespace() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"namespaceName\": \"foo\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testPostQueryUnknownExtension() throws Exception {
        mockExtensionVersionDTO();
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionName("baz"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionName\": \"baz\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testPostQueryInactiveExtension() throws Exception {
        mockInactiveExtensionVersionDTO("foo", "bar");
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionId\": \"foo.bar\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testPostQueryExtensionId() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionId\": \"foo.bar\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testPostQueryExtensionUuid() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionUuid\": \"5678\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testPostQueryNamespaceUuid() throws Exception {
        mockExtensionVersionDTO();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"namespaceUuid\": \"1234\" }"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testCreateNamespace() throws Exception {
        mockAccessToken();
        mockMvc.perform(post("/api/-/namespace/create?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(namespaceJson(n -> { n.name = "foobar"; })))
                .andExpect(status().isCreated())
                .andExpect(redirectedUrl("http://localhost/api/foobar"))
                .andExpect(content().json(successJson("Created namespace foobar")));
    }
    
    @Test
    public void testCreateNamespaceNoName() throws Exception {
        mockAccessToken();
        mockMvc.perform(post("/api/-/namespace/create?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(namespaceJson(n -> {})))
                .andExpect(status().isOk())
                .andExpect(content().json(errorJson("Missing required property 'name'.")));
    }
    
    @Test
    public void testCreateNamespaceInvalidName() throws Exception {
        mockAccessToken();
        mockMvc.perform(post("/api/-/namespace/create?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(namespaceJson(n -> { n.name = "foo.bar"; })))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Invalid namespace name: foo.bar")));
    }
    
    @Test
    public void testCreateNamespaceInactiveToken() throws Exception {
        var token = mockAccessToken();
        token.setActive(false);
        mockMvc.perform(post("/api/-/namespace/create?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(namespaceJson(n -> { n.name = "foobar"; })))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Invalid access token.")));
    }
    
    @Test
    public void testCreateExistingNamespace() throws Exception {
        mockAccessToken();
        var namespace = new Namespace();
        namespace.setName("foobar");
        Mockito.when(repositories.findNamespace("foobar"))
                .thenReturn(namespace);
 
        mockMvc.perform(post("/api/-/namespace/create?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(namespaceJson(n -> { n.name = "foobar"; })))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Namespace already exists: foobar")));
    }
    
    @Test
    public void testPublishOrphan() throws Exception {
        mockForPublish("orphan");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Insufficient access rights for publisher: foo")));
    }
    
    @Test
    public void testPublishRequireLicenseNone() throws Exception {
        var previousRequireLicense = extensionService.requireLicense;
        try {
            extensionService.requireLicense = true;
            mockForPublish("contributor");
            var bytes = createExtensionPackage("bar", "1", null);
            mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .content(bytes))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json(errorJson("This extension cannot be accepted because it has no license.")));
        } finally {
            extensionService.requireLicense = previousRequireLicense;
        }
    }
    
    @Test
    public void testPublishRequireLicenseOk() throws Exception {
        var previousRequireLicense = extensionService.requireLicense;
        try {
            extensionService.requireLicense = true;
            mockForPublish("contributor");
            var bytes = createExtensionPackage("bar", "1", "MIT");
            mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .content(bytes))
                    .andExpect(status().isCreated())
                    .andExpect(content().json(extensionJson(e -> {
                        e.namespace = "foo";
                        e.name = "bar";
                        e.version = "1";
                        var u = new UserJson();
                        u.loginName = "test_user";
                        e.publishedBy = u;
                        e.verified = true;
                    })));
        } finally {
            extensionService.requireLicense = previousRequireLicense;
        }
    }
    
    @Test
    public void testPublishInactiveToken() throws Exception {
        mockForPublish("invalid");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Invalid access token.")));
    }
    
    @Test
    public void testPublishUnknownNamespace() throws Exception {
        mockAccessToken();
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Unknown publisher: foo"
                        + "\nUse the 'create-namespace' command to create a namespace corresponding to your publisher name.")));
    }
    
    @Test
    public void testPublishVerifiedOwner() throws Exception {
        mockForPublish("owner");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = true;
                })));
    }
    
    @Test
    public void testPublishVerifiedContributor() throws Exception {
        mockForPublish("contributor");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = true;
                })));
    }
    
    @Test
    public void testPublishSoleContributor() throws Exception {
        mockForPublish("sole-contributor");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = false;
                })));
    }
    
    @Test
    public void testPublishRestrictedPrivileged() throws Exception {
        mockForPublish("privileged");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = false;
                })));
    }
    
    @Test
    public void testPublishRestrictedUnrelated() throws Exception {
        mockForPublish("unrelated");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Insufficient access rights for publisher: foo")));
    }
    
    @Test
    public void testPublishExistingExtension() throws Exception {
        mockForPublish("existing");
        var bytes = createExtensionPackage("bar", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Extension foo.bar version 1 is already published.")));
    }
    
    @Test
    public void testPublishInvalidName() throws Exception {
        mockForPublish("contributor");
        var bytes = createExtensionPackage("b.a.r", "1", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Invalid extension name: b.a.r")));
    }
    
    @Test
    public void testPublishInvalidVersion() throws Exception {
        mockForPublish("contributor");
        var bytes = createExtensionPackage("bar", "latest", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("The version string 'latest' is reserved.")));
    }
    
    @Test
    public void testPostReview() throws Exception {
        var user = mockUserData();
        var extVersion = mockExtension();
        var extension = extVersion.getExtension();
        Mockito.when(repositories.findExtension("bar", "foo"))
                .thenReturn(extension);
        Mockito.when(repositories.findActiveReviews(extension, user))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.findActiveReviews(extension))
                .thenReturn(Streamable.empty());

        mockMvc.perform(post("/api/{namespace}/{extension}/review", "foo", "bar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(r -> {
                    r.rating = 3;
                }))
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isCreated())
                .andExpect(content().json(successJson("Added review for foo.bar")));
    }
    
    @Test
    public void testPostReviewNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/{namespace}/{extension}/review", "foo", "bar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(r -> {
                    r.rating = 3;
                })).with(csrf().asHeader()))
                .andExpect(status().isForbidden());
    }
    
    @Test
    public void testPostReviewInvalidRating() throws Exception {
        mockMvc.perform(post("/api/{namespace}/{extension}/review", "foo", "bar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(r -> {
                    r.rating = 100;
                }))
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("The rating must be an integer number between 0 and 5.")));
    }
    
    @Test
    public void testPostReviewUnknownExtension() throws Exception {
        mockUserData();
        mockMvc.perform(post("/api/{namespace}/{extension}/review", "foo", "bar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(r -> {
                    r.rating = 3;
                }))
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Extension not found: foo.bar")));
    }
    
    @Test
    public void testPostExistingReview() throws Exception {
        var user = mockUserData();
        var extVersion = mockExtension();
        var extension = extVersion.getExtension();
        Mockito.when(repositories.findExtension("bar", "foo"))
                .thenReturn(extension);
        var review = new ExtensionReview();
        review.setExtension(extension);
        review.setUser(user);
        review.setActive(true);
        Mockito.when(repositories.findActiveReviews(extension, user))
                .thenReturn(Streamable.of(review));

        mockMvc.perform(post("/api/{namespace}/{extension}/review", "foo", "bar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewJson(r -> {
                    r.rating = 3;
                }))
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("You must not submit more than one review for an extension.")));
    }
    
    @Test
    public void testDeleteReview() throws Exception {
        var user = mockUserData();
        var extVersion = mockExtension();
        var extension = extVersion.getExtension();
        Mockito.when(repositories.findExtension("bar", "foo"))
                .thenReturn(extension);
        var review = new ExtensionReview();
        review.setExtension(extension);
        review.setUser(user);
        review.setActive(true);
        Mockito.when(repositories.findActiveReviews(extension, user))
                .thenReturn(Streamable.of(review));
        Mockito.when(repositories.findActiveReviews(extension))
                .thenReturn(Streamable.empty());

        mockMvc.perform(post("/api/{namespace}/{extension}/review/delete", "foo", "bar")
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andExpect(content().json(successJson("Deleted review for foo.bar")));
    }
    
    @Test
    public void testDeleteReviewNotLoggedIn() throws Exception {
        mockMvc.perform(post("/api/{namespace}/{extension}/review/delete", "foo", "bar").with(csrf()))
                .andExpect(status().isForbidden());
    }
    
    @Test
    public void testDeleteReviewUnknownExtension() throws Exception {
        mockUserData();
        mockMvc.perform(post("/api/{namespace}/{extension}/review/delete", "foo", "bar")
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Extension not found: foo.bar")));
    }
    
    @Test
    public void testDeleteNonExistingReview() throws Exception {
        var user = mockUserData();
        var extVersion = mockExtension();
        var extension = extVersion.getExtension();
        Mockito.when(repositories.findExtension("bar", "foo"))
                .thenReturn(extension);
        Mockito.when(repositories.findActiveReviews(extension, user))
                .thenReturn(Streamable.empty());

        mockMvc.perform(post("/api/{namespace}/{extension}/review/delete", "foo", "bar")
                .with(user("test_user"))
                .with(csrf().asHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("You have not submitted any review yet.")));
    }


    //---------- UTILITY ----------//

    private Namespace mockNamespace() {
        var namespace = new Namespace();
        namespace.setName("foobar");
        Mockito.when(repositories.findNamespace("foobar"))
                .thenReturn(namespace);
        Mockito.when(repositories.findActiveExtensions(namespace))
                .thenReturn(Streamable.empty());
        return namespace;
    }

    private String namespaceJson(Consumer<NamespaceJson> content) throws JsonProcessingException {
        var json = new NamespaceJson();
        content.accept(json);
        return new ObjectMapper().writeValueAsString(json);
    }

    private void mockInactiveExtensionVersionDTO(String namespaceName, String extensionName) {
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionName(extensionName, namespaceName))
                .thenReturn(Collections.emptyList());
        Mockito.when(repositories.findActiveExtensionVersionDTOsByNamespaceName(namespaceName))
                .thenReturn(Collections.emptyList());
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionName(extensionName))
                .thenReturn(Collections.emptyList());
    }

    private ExtensionVersionDTO mockExtensionVersionDTO() {
        var namespaceId = 1L;
        var namespacePublicId = "1234";
        var namespaceName = "foo";
        var extensionId = 2L;
        var extensionName = "bar";
        var id = 3L;
        var extensionLatestId = id;
        var version = "1";
        var timestamp = LocalDateTime.parse("2000-01-01T10:00");
        var displayName = "Foo Bar";

        var extVersion = new ExtensionVersionDTO(
                namespaceId, namespacePublicId, namespaceName, extensionId, null, extensionName, false, extensionLatestId,
                null, null, 0, null, null, null,
                null, null, null, id, version, false, timestamp, displayName, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null
        );

        var extensionPublicId = "5678";
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionPublicId(extensionPublicId))
                .thenReturn(List.of(extVersion));
        Mockito.when(repositories.findActiveExtensionVersionDTOsByNamespacePublicId(namespacePublicId))
                .thenReturn(List.of(extVersion));
        Mockito.when(repositories.findActiveExtensionVersionDTOByVersion(version, extensionName, namespaceName))
                .thenReturn(extVersion);
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionName(extensionName, namespaceName))
                .thenReturn(List.of(extVersion));
        Mockito.when(repositories.findActiveExtensionVersionDTOsByNamespaceName(namespaceName))
                .thenReturn(List.of(extVersion));
        Mockito.when(repositories.findActiveExtensionVersionDTOsByExtensionName(extensionName))
                .thenReturn(List.of(extVersion));

        Mockito.when(repositories.findAllActiveReviewCountsByExtensionId(Set.of(extensionId)))
                .thenReturn(Collections.emptyList());
        var fileTypes = List.of(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG);
        Mockito.when(repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(List.of(id), fileTypes))
                .thenReturn(Collections.emptyList());
        Mockito.when(repositories.findAllNamespaceMembershipDTOs(List.of(namespaceId)))
                .thenReturn(Collections.emptyList());

        return extVersion;
    }
    
    private ExtensionVersion mockExtension() {
        var namespace = new Namespace();
        namespace.setName("foo");
        namespace.setPublicId("1234");
        var extension = new Extension();
        extension.setName("bar");
        extension.setNamespace(namespace);
        extension.setPublicId("5678");
        extension.setActive(true);
        var extVersion = new ExtensionVersion();
        extension.setLatest(extVersion);
        extVersion.setExtension(extension);
        extVersion.setVersion("1");
        extVersion.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        extVersion.setActive(true);
        extVersion.setDisplayName("Foo Bar");
        Mockito.when(repositories.findExtension("bar", "foo"))
                .thenReturn(extension);
        Mockito.when(repositories.findVersion("1", "bar", "foo"))
                .thenReturn(extVersion);
        Mockito.when(repositories.findVersions(extension))
                .thenReturn(Streamable.of(extVersion));
        Mockito.when(repositories.findActiveExtensions(namespace))
                .thenReturn(Streamable.of(extension));
        Mockito.when(repositories.getVersionStrings(extension))
                .thenReturn(Streamable.of(extVersion.getVersion()));
        Mockito.when(repositories.getActiveVersionStrings(extension))
                .thenReturn(Streamable.of(extVersion.getVersion()));
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(0l);
        Mockito.when(repositories.countActiveReviews(extension))
                .thenReturn(0l);
        Mockito.when(repositories.findFilesByType(eq(extVersion), anyCollection()))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.findNamespace("foo"))
                .thenReturn(namespace);
        Mockito.when(repositories.findExtensions("bar"))
                .thenReturn(Streamable.of(extension));
        Mockito.when(repositories.findNamespaceByPublicId("1234"))
                .thenReturn(namespace);
        Mockito.when(repositories.findExtensionByPublicId("5678"))
                .thenReturn(extension);
        return extVersion;
    }

    private String extensionJson(Consumer<ExtensionJson> content) throws JsonProcessingException {
        var json = new ExtensionJson();
        content.accept(json);
        return new ObjectMapper().writeValueAsString(json);
    }

    private String queryResultJson(Consumer<ExtensionJson> content) throws JsonProcessingException {
        return "{\"extensions\":[" + extensionJson(content) + "]}";
    }

    private FileResource mockReadme() {
        var extVersion = mockExtension();
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName("README");
        resource.setType(FileResource.README);
        resource.setContent("Please read me".getBytes());
        resource.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByName(extVersion, "README"))
                .thenReturn(resource);
        return resource;
    }

    private FileResource mockChangelog() {
        var extVersion = mockExtension();
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName("CHANGELOG");
        resource.setType(FileResource.CHANGELOG);
        resource.setContent("All notable changes is documented here".getBytes());
        resource.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByName(extVersion, "CHANGELOG"))
                .thenReturn(resource);
        return resource;
    }

    private FileResource mockLicense() {
        var extVersion = mockExtension();
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName("LICENSE");
        resource.setType(FileResource.LICENSE);
        resource.setContent("I never broke the Law! I am the law!".getBytes());
        resource.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByName(extVersion, "LICENSE"))
                .thenReturn(resource);
        return resource;
    }

    private void mockReviews() {
        var extVersion = mockExtension();
        var extension = extVersion.getExtension();
        var user1 = new UserData();
        user1.setLoginName("user1");
        var review1 = new ExtensionReview();
        review1.setExtension(extension);
        review1.setUser(user1);
        review1.setRating(3);
        review1.setComment("Somewhat ok");
        review1.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        review1.setActive(true);
        var user2 = new UserData();
        user2.setLoginName("user2");
        var review2 = new ExtensionReview();
        review2.setExtension(extension);
        review2.setUser(user2);
        review2.setRating(4);
        review2.setComment("Quite good");
        review2.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        review2.setActive(true);
        Mockito.when(repositories.findActiveReviews(extension))
                .thenReturn(Streamable.of(review1, review2));
    }

    private String reviewsJson(Consumer<ReviewListJson> content) throws JsonProcessingException {
        var json = new ReviewListJson();
        json.reviews = new ArrayList<>();
        content.accept(json);
        return new ObjectMapper().writeValueAsString(json);
    }

    private List<Extension> mockSearch() {
        var extVersion = mockExtension();
        var extension = extVersion.getExtension();
        extension.setId(1l);
        var entry1 = new ExtensionSearch();
        entry1.id = 1;
        var searchHit = new SearchHit<ExtensionSearch>("0", "1", 1.0f, null, null, entry1);
        var searchHits = new SearchHitsImpl<ExtensionSearch>(1, TotalHitsRelation.EQUAL_TO, 1.0f, "1",
                Arrays.asList(searchHit), new Aggregations(Collections.emptyList()));
        Mockito.when(search.isEnabled())
                .thenReturn(true);
        var searchOptions = new ISearchService.Options("foo", null, 10, 0, "desc", "relevance", false);
        Mockito.when(search.search(searchOptions, PageRequest.of(0, 10)))
                .thenReturn(searchHits);
        Mockito.when(entityManager.find(Extension.class, 1l))
                .thenReturn(extension);
        return Arrays.asList(extension);
    }

    private String searchJson(Consumer<SearchResultJson> content) throws JsonProcessingException {
        var json = new SearchResultJson();
        json.extensions = new ArrayList<>();
        content.accept(json);
        return new ObjectMapper().writeValueAsString(json);
    }

    private PersonalAccessToken mockAccessToken() {
        var userData = new UserData();
        userData.setLoginName("test_user");
        var token = new PersonalAccessToken();
        token.setUser(userData);
        token.setCreatedTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        token.setValue("my_token");
        token.setActive(true);
        Mockito.when(repositories.findAccessToken("my_token"))
                .thenReturn(token);
        return token;
    }
    
    private void mockForPublish(String mode) {
        var token = mockAccessToken();
        if (mode.equals("invalid")) {
            token.setActive(false);
        }
        var namespace = new Namespace();
        namespace.setName("foo");
        Mockito.when(repositories.findNamespace("foo"))
                .thenReturn(namespace);
        if (mode.equals("existing")) {
            var extension = new Extension();
            extension.setName("bar");
            var extVersion = new ExtensionVersion();
            extVersion.setExtension(extension);
            extVersion.setVersion("1");
            extVersion.setActive(true);
            Mockito.when(repositories.findExtension("bar", namespace))
                    .thenReturn(extension);
            Mockito.when(repositories.findVersion("1", extension))
                    .thenReturn(extVersion);
        }
        Mockito.when(repositories.countActiveReviews(any(Extension.class)))
                .thenReturn(0l);
        Mockito.when(repositories.findVersions(any(Extension.class)))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.findActiveVersions(any(Extension.class)))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.getVersionStrings(any(Extension.class)))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.getActiveVersionStrings(any(Extension.class)))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.findFilesByType(any(ExtensionVersion.class), anyCollection()))
                .thenReturn(Streamable.empty());
        if (mode.equals("owner")) {
            var ownerMem = new NamespaceMembership();
            ownerMem.setUser(token.getUser());
            ownerMem.setNamespace(namespace);
            ownerMem.setRole(NamespaceMembership.ROLE_OWNER);
            Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.of(ownerMem));
            Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(1l);
            Mockito.when(repositories.findMembership(token.getUser(), namespace))
                    .thenReturn(ownerMem);
            Mockito.when(repositories.countMemberships(token.getUser(), namespace))
                    .thenReturn(1l);
        } else if (mode.equals("contributor") || mode.equals("sole-contributor") || mode.equals("existing")) {
            var contribMem = new NamespaceMembership();
            contribMem.setUser(token.getUser());
            contribMem.setNamespace(namespace);
            contribMem.setRole(NamespaceMembership.ROLE_CONTRIBUTOR);
            Mockito.when(repositories.findMembership(token.getUser(), namespace))
                    .thenReturn(contribMem);
            Mockito.when(repositories.countMemberships(token.getUser(), namespace))
                    .thenReturn(1l);
            if (mode.equals("contributor")) {
                var otherUser = new UserData();
                otherUser.setLoginName("other_user");
                var ownerMem = new NamespaceMembership();
                ownerMem.setUser(otherUser);
                ownerMem.setNamespace(namespace);
                ownerMem.setRole(NamespaceMembership.ROLE_OWNER);
                Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                        .thenReturn(Streamable.of(ownerMem));
                Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                        .thenReturn(1l);
            } else {
                Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.empty());
                Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                        .thenReturn(0l);
            }
        } else if (mode.equals("privileged") || mode.equals("unrelated")) {
            var otherUser = new UserData();
            otherUser.setLoginName("other_user");
            var ownerMem = new NamespaceMembership();
            ownerMem.setUser(otherUser);
            ownerMem.setNamespace(namespace);
            ownerMem.setRole(NamespaceMembership.ROLE_OWNER);
            Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.of(ownerMem));
            Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(1l);
            if (mode.equals("privileged")) {
                token.getUser().setRole(UserData.ROLE_PRIVILEGED);
            }
        } else {
            Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.empty());
            Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(0l);
        }
    }

    private String reviewJson(Consumer<ReviewJson> content) throws JsonProcessingException {
        var json = new ReviewJson();
        content.accept(json);
        return new ObjectMapper().writeValueAsString(json);
    }

    private UserData mockUserData() {
        var userData = new UserData();
        userData.setLoginName("test_user");
        userData.setFullName("Test User");
        userData.setProviderUrl("http://example.com/test");
        Mockito.doReturn(userData).when(users).findLoggedInUser();
        return userData;
    }

    private String successJson(String message) throws JsonProcessingException {
        var json = ResultJson.success(message);
        return new ObjectMapper().writeValueAsString(json);
    }

    private String errorJson(String message) throws JsonProcessingException {
        var json = ResultJson.error(message);
        return new ObjectMapper().writeValueAsString(json);
    }
    
    private byte[] createExtensionPackage(String name, String version, String license) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var archive = new ZipOutputStream(bytes);
        archive.putNextEntry(new ZipEntry("extension.vsixmanifest"));
        var vsixmanifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<PackageManifest Version=\"2.0.0\" xmlns=\"http://schemas.microsoft.com/developer/vsx-schema/2011\" xmlns:d=\"http://schemas.microsoft.com/developer/vsx-schema-design/2011\">" +
            "<Metadata>" +
            "<Identity Language=\"en-US\" Id=\""+ name +"\" Version=\"" + version + "\" Publisher=\"foo\"  />" +
            "<DisplayName>foo</DisplayName>" +
            "<Description xml:space=\"preserve\"></Description>" +
            "<Tags></Tags>" +
            "<Categories>Other</Categories>" +
            "<GalleryFlags>Public</GalleryFlags>" +
            "<Badges></Badges>" +
            "<Properties>" +
            "<Property Id=\"Microsoft.VisualStudio.Code.Engine\" Value=\"^1.57.0\" />" +
            "<Property Id=\"Microsoft.VisualStudio.Code.ExtensionDependencies\" Value=\"\" />" +
            "<Property Id=\"Microsoft.VisualStudio.Code.ExtensionPack\" Value=\"\" />" +
            "<Property Id=\"Microsoft.VisualStudio.Code.ExtensionKind\" Value=\"ui,web,workspace\" />" +
            "<Property Id=\"Microsoft.VisualStudio.Code.LocalizedLanguages\" Value=\"\" />" +
            "<Property Id=\"Microsoft.VisualStudio.Services.GitHubFlavoredMarkdown\" Value=\"true\" />" +
            "</Properties>" +
            "</Metadata>" +
            "<Installation>" +
            "<InstallationTarget Id=\"Microsoft.VisualStudio.Code\"/>" +
            "</Installation>" +
            "<Dependencies/>" +
            "<Assets>" +
            "<Asset Type=\"Microsoft.VisualStudio.Code.Manifest\" Path=\"extension/package.json\" Addressable=\"true\" />" +
            "</Assets>" +
            "</PackageManifest>";
        archive.write(vsixmanifest.getBytes());
        archive.closeEntry();
        archive.putNextEntry(new ZipEntry("extension/package.json"));
        var packageJson = "{" +
                "\"publisher\": \"foo\"," +
                "\"name\": \"" + name + "\"," +
                "\"version\": \"" + version + "\"" +
                (license == null ? "" : ",\"license\": \"" + license + "\"" ) +
            "}";
        archive.write(packageJson.getBytes());
        archive.closeEntry();
        archive.finish();
        return bytes.toByteArray();
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        TransactionTemplate transactionTemplate() {
            return new MockTransactionTemplate();
        }

        @Bean
        OAuth2UserServices oauth2UserServices() {
            return new OAuth2UserServices();
        }

        @Bean
        TokenService tokenService() {
            return new TokenService();
        }

        @Bean
        LocalRegistryService localRegistryService() {
            return new LocalRegistryService();
        }

        @Bean
        ExtensionService extensionService() {
            return new ExtensionService();
        }

        @Bean
        ExtensionValidator extensionValidator() {
            return new ExtensionValidator();
        }

        @Bean
        StorageUtilService storageUtilService() {
            return new StorageUtilService();
        }
    }
    
}