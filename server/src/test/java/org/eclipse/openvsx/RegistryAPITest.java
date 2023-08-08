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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.cache.ExtensionJsonCacheKeyGenerator;
import org.eclipse.openvsx.cache.LatestExtensionVersionCacheKeyGenerator;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.publish.PublishExtensionVersionHandler;
import org.eclipse.openvsx.publish.PublishExtensionVersionService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.security.OAuth2UserServices;
import org.eclipse.openvsx.security.SecurityConfig;
import org.eclipse.openvsx.security.TokenService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.AzureDownloadCountService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.eclipse.openvsx.entities.FileResource.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegistryAPI.class)
@AutoConfigureWebClient
@MockBean({
    ClientRegistrationRepository.class, UpstreamRegistryService.class, GoogleCloudStorageService.class,
    AzureBlobStorageService.class, VSCodeIdService.class, AzureDownloadCountService.class, CacheService.class,
    EclipseService.class, PublishExtensionVersionService.class, SimpleMeterRegistry.class
})
public class RegistryAPITest {

    @SpyBean
    UserService users;

    @MockBean
    RepositoryService repositories;

    @MockBean
    SearchUtilService search;

    @MockBean
    ExtensionVersionIntegrityService integrityService;

    @MockBean
    EntityManager entityManager;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ExtensionService extensions;

    @Autowired
    VersionService versions;

    @Test
    public void testPublicNamespace() throws Exception {
        var namespace = mockNamespace();
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(0L);
        
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
                .thenReturn(1L);

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
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testExtensionWithPublicKey() throws Exception {
        Mockito.when(integrityService.isEnabled()).thenReturn(true);
        var extVersion = mockExtensionWithSignature();
        var keyPair = new SignatureKeyPair();
        keyPair.setPublicId("123-456-7890");
        extVersion.setSignatureKeyPair(keyPair);
        mockMvc.perform(get("/api/{namespace}/{extension}", "foo", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.files = Map.of("publicKey", "http://localhost/api/-/public-key/" + keyPair.getPublicId());
                })));
    }

    @Test
    public void testExtensionNonDefaultTarget() throws Exception {
        var extVersion = mockExtension("alpine-x64");
        extVersion.setDisplayName("Foo Bar (alpine x64)");

        mockMvc.perform(get("/api/{namespace}/{extension}", "foo", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (alpine x64)";
                    e.targetPlatform = "alpine-x64";
                })));
    }

    @Test
    public void testExtensionLinuxTarget() throws Exception {
        var extVersion = mockExtension("linux-x64");
        extVersion.setDisplayName("Foo Bar (linux x64)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}", "foo", "bar", "linux-x64"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.targetPlatform = "linux-x64";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (linux x64)";
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
    public void testUnknownExtensionTarget() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}", "foo", "bar", "win32-ia32"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar (win32-ia32)")));
    }

    @Test
    public void testExtensionVersion() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testExtensionVersionNonDefaultTarget() throws Exception {
        var extVersion = mockExtension("darwin-arm64");
        extVersion.setDisplayName("Foo Bar (darwin arm64)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (darwin arm64)";
                })));
    }

    @Test
    public void testExtensionVersionMacOSXTarget() throws Exception {
        var extVersion = mockExtension("darwin-arm64");
        extVersion.setDisplayName("Foo Bar (darwin arm64)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}/{version}", "foo", "bar", "darwin-arm64", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (darwin arm64)";
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
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.versionAlias = List.of("latest");
                })));
    }

    @Test
    public void testLatestExtensionVersionNonDefaultTarget() throws Exception {
        var extVersion = mockExtension("alpine-arm64");
        extVersion.setDisplayName("Foo Bar (alpine arm64)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "latest"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (alpine arm64)";
                    e.versionAlias = List.of("latest");
                    e.targetPlatform = "alpine-arm64";
                })));
    }

    @Test
    public void testLatestExtensionVersionAlpineLinuxTarget() throws Exception {
        var extVersion = mockExtension("alpine-arm64");
        extVersion.setDisplayName("Foo Bar (alpine arm64)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}/{version}", "foo", "bar", "alpine-arm64", "latest"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (alpine arm64)";
                    e.versionAlias = List.of("latest");
                    e.targetPlatform = "alpine-arm64";
                })));
    }

    @Test
    public void testPreReleaseExtensionVersion() throws Exception {
        var extVersion = mockExtension();
        extVersion.setPreRelease(true);
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "pre-release"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.versionAlias = List.of("pre-release", "latest");
                    e.preRelease = true;
                })));
    }

    @Test
    public void testPreReleaseExtensionVersionNonDefaultTarget() throws Exception {
        var extVersion = mockExtension("web");
        extVersion.setPreRelease(true);
        extVersion.setDisplayName("Foo Bar (web)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "pre-release"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (web)";
                    e.versionAlias = List.of("pre-release", "latest");
                    e.preRelease = true;
                })));
    }

    @Test
    public void testPreReleaseExtensionVersionWebTarget() throws Exception {
        var extVersion = mockExtension("web");
        extVersion.setPreRelease(true);
        extVersion.setDisplayName("Foo Bar (web)");
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}/{version}", "foo", "bar", "web", "pre-release"))
                .andExpect(status().isOk())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar (web)";
                    e.versionAlias = List.of("pre-release", "latest");
                    e.preRelease = true;
                })));
    }

    @Test
    public void testInactiveExtensionVersion() throws Exception {
        var extVersion = mockExtension();
        extVersion.setActive(false);

        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "1.0.0"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar 1.0.0")));
    }

    @Test
    public void testUnknownExtensionVersion() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}", "foo", "bar", "2.0.0"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar 2.0.0")));
    }

    @Test
    public void testUnknownExtensionVersionTarget() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}/{version}", "foo", "bar", "linux-armhf", "1.0.0"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(errorJson("Extension not found: foo.bar 1.0.0 (linux-armhf)")));
    }

    @Test
    public void testReadmeUniversalTarget() throws Exception {
        mockReadme();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1.0.0", "README"))
                .andExpect(status().isOk())
                .andExpect(content().string("Please read me"));
    }

    @Test
    public void testReadmeWindowsTarget() throws Exception {
        mockReadme("win32-x64");
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}/{version}/file/{fileName}", "foo", "bar", "win32-x64", "1.0.0", "README"))
                .andExpect(status().isOk())
                .andExpect(content().string("Please read me"));
    }

    @Test
    public void testReadmeUnknownTarget() throws Exception {
        mockReadme();
        mockMvc.perform(get("/api/{namespace}/{extension}/{target}/{version}/file/{fileName}", "foo", "bar", "darwin-x64", "1.0.0", "README"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testChangelog() throws Exception {
        mockChangelog();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1.0.0", "CHANGELOG"))
                .andExpect(status().isOk())
                .andExpect(content().string("All notable changes is documented here"));
    }

    @Test
    public void testLicense() throws Exception {
        mockLicense();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1.0.0", "LICENSE"))
                .andExpect(status().isOk())
                .andExpect(content().string("I never broke the Law! I am the law!"));
    }

    @Test
    public void testInactiveFile() throws Exception {
        var extVersion = mockExtension();
        extVersion.setActive(false);

        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1.0.0", "README"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUnknownFile() throws Exception {
        mockExtension();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "1.0.0", "unknown.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testLatestFile() throws Exception {
        mockLatest();
        mockMvc.perform(get("/api/{namespace}/{extension}/{version}/file/{fileName}", "foo", "bar", "latest", "DOWNLOAD"))
                .andExpect(status().isOk())
                .andExpect(content().string("latest download"));
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
                    e1.version = "1.0.0";
                    e1.timestamp = "2000-01-01T10:00Z";
                    e1.displayName = "Foo Bar";
                    s.extensions.add(e1);
                })));
    }

    @Test
    public void testSearchInactive() throws Exception {
        var extensionsList = mockSearch();
        extensionsList.forEach(extension -> {
            extension.setActive(false);
            versions.getLatest(extension, null, false, false).setActive(false);
        });

        mockMvc.perform(get("/api/-/search?query={query}&size={size}&offset={offset}", "foo", "10", "0"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"offset\":0,\"totalSize\":1,\"extensions\":[]}"));
    }

    @Test
    public void testGetQueryExtensionName() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/-/query?extensionName={extensionName}", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryNamespace() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/-/query?namespaceName={namespaceName}", "foo"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryUnknownExtension() throws Exception {
        mockExtensionVersion();
        Mockito.when(repositories.findActiveExtensionVersionsByExtensionName(TargetPlatform.NAME_UNIVERSAL, "baz"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/-/query?extensionName={extensionName}", "baz"))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testGetQueryInactiveExtension() throws Exception {
        var namespaceName = "foo";
        var extensionName = "bar";

        mockInactiveExtensionVersion(namespaceName, extensionName);
        mockMvc.perform(get("/api/-/query?extensionId={namespaceName}.{extensionName}", namespaceName, extensionName))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testGetQueryExtensionId() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/-/query?extensionId={extensionId}", "foo.bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryExtensionVersion() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/-/query?extensionId={id}&extensionVersion={version}", "foo.bar", "1.0.0"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryExtensionUuid() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/-/query?extensionUuid={extensionUuid}", "5678"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryNamespaceUuid() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/-/query?namespaceUuid={namespaceUuid}", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryMultipleTargets() throws Exception {
        var versions = mockExtensionVersionTargetPlatforms();
        var query = new QueryRequest();
        query.namespaceUuid = "1234";
        query.offset = 0;
        query.size = 100;
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/-/query?namespaceUuid={namespaceUuid}", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.targetPlatform = "darwin-x64";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.targetPlatform = "linux-x64";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.targetPlatform = "alpine-arm64";
                })));
    }

    @Test
    public void testGetQueryV2ExtensionName() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/v2/-/query?extensionName={extensionName}", "bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryV2Namespace() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/v2/-/query?namespaceName={namespaceName}", "foo"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryV2UnknownExtension() throws Exception {
        mockExtensionVersion();
        Mockito.when(repositories.findActiveExtensionVersionsByExtensionName(TargetPlatform.NAME_UNIVERSAL, "baz"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v2/-/query?extensionName={extensionName}", "baz"))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testGetQueryV2InactiveExtension() throws Exception {
        var namespaceName = "foo";
        var extensionName = "bar";

        mockInactiveExtensionVersion(namespaceName, extensionName);
        mockMvc.perform(get("/api/v2/-/query?extensionId={namespaceName}.{extensionName}", namespaceName, extensionName))
                .andExpect(status().isOk())
                .andExpect(content().json("{ \"extensions\": [] }"));
    }

    @Test
    public void testGetQueryV2ExtensionId() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}", "foo.bar"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryV2IncludeAllVersionsTrue() throws Exception {
        var versions = mockExtensionVersionVersionsTargetPlatforms();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.includeAllVersions = true;
        query.offset = 0;
        query.size = 100;
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&includeAllVersions={includeAllVersions}", "foo.bar", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.targetPlatform = "darwin-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/darwin-x64/1.0.0";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "darwin-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/darwin-x64/2.0.0";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.targetPlatform = "linux-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/linux-x64/1.0.0";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "linux-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/linux-x64/2.0.0";
                })));
    }

    @Test
    public void testGetQueryV2IncludeAllVersionsFalse() throws Exception {
        var versions = mockExtensionVersionVersions();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        versions = versions.stream().filter(ev -> ev.getVersion().equals("3.0.0")).collect(Collectors.toList());
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&includeAllVersions={includeAllVersions}", "foo.bar", "false"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "3.0.0";
                    e.versionAlias = List.of("latest");
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/universal/3.0.0";
                })));
    }

    @Test
    public void testGetQueryV2IncludeAllVersionsLinks() throws Exception {
        var versions = mockExtensionVersionVersions();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        versions = versions.stream().filter(ev -> ev.getVersion().equals("3.0.0")).collect(Collectors.toList());
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&includeAllVersions={includeAllVersions}", "foo.bar", "links"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "3.0.0";
                    e.versionAlias = List.of("latest");
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = Map.of(
                            "latest", "http://localhost/api/foo/bar/latest",
                            "3.0.0", "http://localhost/api/foo/bar/3.0.0",
                            "2.0.0", "http://localhost/api/foo/bar/2.0.0",
                            "1.0.0", "http://localhost/api/foo/bar/1.0.0"
                    );
                    e.url = "http://localhost/api/foo/bar/universal/3.0.0";
                })));
    }

    @Test
    public void testGetQueryV2MultipleTargetsIncludeAllVersionsLinks() throws Exception {
        var versions = mockExtensionVersionVersionsTargetPlatforms();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        versions = versions.stream().filter(ev -> ev.getVersion().equals("2.0.0")).collect(Collectors.toList());
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&includeAllVersions={includeAllVersions}", "foo.bar", "links"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "darwin-x64";
                    e.versionAlias = List.of("latest");
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = Map.of(
                            "latest", "http://localhost/api/foo/bar/latest",
                            "2.0.0", "http://localhost/api/foo/bar/2.0.0",
                            "1.0.0", "http://localhost/api/foo/bar/1.0.0"
                    );
                    e.url = "http://localhost/api/foo/bar/darwin-x64/2.0.0";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "linux-x64";
                    e.versionAlias = List.of("latest");
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = Map.of(
                            "latest", "http://localhost/api/foo/bar/latest",
                            "2.0.0", "http://localhost/api/foo/bar/2.0.0",
                            "1.0.0", "http://localhost/api/foo/bar/1.0.0"
                    );
                    e.url = "http://localhost/api/foo/bar/linux-x64/2.0.0";
                })));
    }

    @Test
    public void testGetQueryV2TargetPlatformIncludeAllVersionsTrue() throws Exception {
        var versions = mockExtensionVersionVersionsTargetPlatforms("linux-x64");
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.targetPlatform = "linux-x64";
        query.includeAllVersions = true;
        query.offset = 0;
        query.size = 100;
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&targetPlatform={targetPlatform}&includeAllVersions={includeAllVersions}", "foo.bar", "linux-x64", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.targetPlatform = "linux-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/linux-x64/1.0.0";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "linux-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/linux-x64/2.0.0";
                })));
    }

    @Test
    public void testGetQueryV2ExtensionVersionIncludeAllVersionsTrue() throws Exception {
        var versions = mockExtensionVersionVersionsTargetPlatforms();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.extensionVersion = "2.0.0";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        versions = versions.stream().filter(ev -> ev.getVersion().equals("2.0.0")).collect(Collectors.toList());
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&extensionVersion={extensionVersion}&includeAllVersions={includeAllVersions}", "foo.bar", "2.0.0", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "darwin-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/darwin-x64/2.0.0";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.targetPlatform = "linux-x64";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/linux-x64/2.0.0";
                })));
    }

    @Test
    public void testGetQueryV2ExtensionVersionIncludeAllVersionsFalse() throws Exception {
        var versions = mockExtensionVersionVersions();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.extensionVersion = "2.0.0";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        versions = versions.stream().filter(ev -> ev.getVersion().equals("2.0.0")).collect(Collectors.toList());
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&extensionVersion={extensionVersion}&includeAllVersions={includeAllVersions}", "foo.bar", "2.0.0", "false"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = null;
                    e.url = "http://localhost/api/foo/bar/universal/2.0.0";
                })));
    }

    @Test
    public void testGetQueryV2ExtensionVersionIncludeAllVersionsLinks() throws Exception {
        var versions = mockExtensionVersionVersions();
        var query = new QueryRequest();
        query.namespaceName = "foo";
        query.extensionName = "bar";
        query.extensionVersion = "2.0.0";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        versions = versions.stream().filter(ev -> ev.getVersion().equals("2.0.0")).collect(Collectors.toList());
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?extensionId={extensionId}&extensionVersion={extensionVersion}&includeAllVersions={includeAllVersions}", "foo.bar", "2.0.0", "links"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "2.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.allVersions = Map.of(
                            "latest", "http://localhost/api/foo/bar/latest",
                            "3.0.0", "http://localhost/api/foo/bar/3.0.0",
                            "2.0.0", "http://localhost/api/foo/bar/2.0.0",
                            "1.0.0", "http://localhost/api/foo/bar/1.0.0"
                    );
                    e.url = "http://localhost/api/foo/bar/universal/2.0.0";
                })));
    }

    @Test
    public void testGetQueryV2ExtensionUuid() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/v2/-/query?extensionUuid={extensionUuid}", "5678"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryV2NamespaceUuid() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/api/v2/-/query?namespaceUuid={namespaceUuid}", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                })));
    }

    @Test
    public void testGetQueryV2MultipleTargets() throws Exception {
        var versions = mockExtensionVersionTargetPlatforms();
        var query = new QueryRequest();
        query.namespaceUuid = "1234";
        query.includeAllVersions = false;
        query.offset = 0;
        query.size = 100;
        Mockito.when(repositories.findActiveVersions(query))
                .thenReturn(new PageImpl<>(versions, Pageable.ofSize(100), versions.size()));

        mockMvc.perform(get("/api/v2/-/query?namespaceUuid={namespaceUuid}", "1234"))
                .andExpect(status().isOk())
                .andExpect(content().json(queryResultJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.targetPlatform = "darwin-x64";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.targetPlatform = "linux-x64";
                },
                e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    e.verified = false;
                    e.timestamp = "2000-01-01T10:00Z";
                    e.displayName = "Foo Bar";
                    e.targetPlatform = "alpine-arm64";
                })));
    }

    @Test
    public void testPostQueryExtensionName() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionName\": \"bar\" }"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/-/query?extensionName=bar&includeAllVersions=false"));
    }

    @Test
    public void testPostQueryNamespace() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"namespaceName\": \"foo\" }"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/-/query?namespaceName=foo&includeAllVersions=false"));
    }

    @Test
    public void testPostQueryExtensionId() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionId\": \"foo.bar\" }"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/-/query?extensionId=foo.bar&includeAllVersions=false"));
    }

    @Test
    public void testPostQueryExtensionUuid() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"extensionUuid\": \"5678\" }"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/-/query?extensionUuid=5678&includeAllVersions=false"));
    }

    @Test
    public void testPostQueryNamespaceUuid() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(post("/api/-/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"namespaceUuid\": \"1234\" }"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "http://localhost/api/-/query?namespaceUuid=1234&includeAllVersions=false"));
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

    @ParameterizedTest
    @ValueSource(strings = {"owner", "contributor", "sole-contributor"})
    public void testVerifyToken(String mode) throws Exception {
        mockForPublish(mode);

        mockMvc.perform(get("/api/{namespace}/verify-pat?token={token}", "foo", "my_token"))
                .andExpect(status().isOk());
    }

    @Test
    public void testVerifyTokenNoNamespace() throws Exception {
        mockAccessToken();

        mockMvc.perform(get("/api/{namespace}/verify-pat?token={token}", "unexistingnamespace", "my_token"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testVerifyTokenInvalid() throws Exception {
        mockForPublish("invalid");

        mockMvc.perform(get("/api/{namespace}/verify-pat?token={token}", "foo", "my_token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testVerifyTokenNoToken() throws Exception {
        mockAccessToken();
        mockNamespace();

        mockMvc.perform(get("/api/{namespace}/verify-pat", "foobar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testVerifyTokenNoPermission() throws Exception {
        mockAccessToken();
        mockNamespace();

        mockMvc.perform(get("/api/{namespace}/verify-pat?token={token}", "foobar", "my_token"))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    public void testPublishOrphan() throws Exception {
        mockForPublish("orphan");
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Insufficient access rights for publisher: foo")));
    }
    
    @Test
    public void testPublishRequireLicenseNone() throws Exception {
        var previousRequireLicense = extensions.requireLicense;
        try {
            extensions.requireLicense = true;
            mockForPublish("contributor");
            var bytes = createExtensionPackage("bar", "1.0.0", null);
            mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .content(bytes))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().json(errorJson("This extension cannot be accepted because it has no license.")));
        } finally {
            extensions.requireLicense = previousRequireLicense;
        }
    }
    
    @Test
    public void testPublishRequireLicenseOk() throws Exception {
        var previousRequireLicense = extensions.requireLicense;
        try {
            extensions.requireLicense = true;
            mockForPublish("contributor");
            mockActiveVersion();
            var bytes = createExtensionPackage("bar", "1.0.0", "MIT");
            mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .content(bytes))
                    .andExpect(status().isCreated())
                    .andExpect(content().json(extensionJson(e -> {
                        e.namespace = "foo";
                        e.name = "bar";
                        e.version = "1.0.0";
                        var u = new UserJson();
                        u.loginName = "test_user";
                        e.publishedBy = u;
                        e.verified = true;
                    })));
        } finally {
            extensions.requireLicense = previousRequireLicense;
        }
    }
    
    @Test
    public void testPublishInactiveToken() throws Exception {
        mockForPublish("invalid");
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Invalid access token.")));
    }
    
    @Test
    public void testPublishUnknownNamespace() throws Exception {
        mockAccessToken();
        var bytes = createExtensionPackage("bar", "1.0.0", null);
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
        mockActiveVersion();
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = true;
                })));
    }

    @Test
    public void testPublishVerifiedContributor() throws Exception {
        mockForPublish("contributor");
        mockActiveVersion();
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = true;
                })));
    }

    @Test
    public void testPublishSoleContributor() throws Exception {
        mockForPublish("sole-contributor");
        mockActiveVersion();
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = false;
                })));
    }

    @Test
    public void testPublishRestrictedPrivileged() throws Exception {
        mockForPublish("privileged");
        mockActiveVersion();
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(extensionJson(e -> {
                    e.namespace = "foo";
                    e.name = "bar";
                    e.version = "1.0.0";
                    var u = new UserJson();
                    u.loginName = "test_user";
                    e.publishedBy = u;
                    e.verified = true;
                })));
    }

    @Test
    public void testPublishRestrictedUnrelated() throws Exception {
        mockForPublish("unrelated");
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Insufficient access rights for publisher: foo")));
    }

    @Test
    public void testPublishExistingExtension() throws Exception {
        mockForPublish("existing");
        var bytes = createExtensionPackage("bar", "1.0.0", null);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(errorJson("Extension foo.bar 1.0.0 is already published.")));
    }

    @Test
    public void testPublishSameVersionDifferentTargetPlatformPreRelease() throws Exception {
        var extVersion = mockExtension(TargetPlatform.NAME_WIN32_X64);
        extVersion.setVersion("1.0.0");
        extVersion.setPreRelease(false);

        mockForPublish("contributor");
        Mockito.when(repositories.findVersions(eq("1.0.0"), any(Extension.class)))
                .thenReturn(Streamable.of(extVersion));

        var bytes = createExtensionPackage("bar", "1.0.0", null, true, TargetPlatform.NAME_LINUX_X64);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(warningJson("A stable release already exists for foo.bar 1.0.0.\n" +
                        "To prevent update conflicts, we recommend that this pre-release uses 1.1.0 as its version instead.")));
    }

    @Test
    public void testPublishSameVersionDifferentTargetPlatformStableRelease() throws Exception {
        var extVersion = mockExtension(TargetPlatform.NAME_DARWIN_ARM64);
        extVersion.setVersion("1.5.0");
        extVersion.setPreRelease(true);

        mockForPublish("contributor");
        Mockito.when(repositories.findVersions(eq("1.5.0"), any(Extension.class)))
                .thenReturn(Streamable.of(extVersion));

        var bytes = createExtensionPackage("bar", "1.5.0", null, false, TargetPlatform.NAME_ALPINE_ARM64);
        mockMvc.perform(post("/api/-/publish?token={token}", "my_token")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(bytes))
                .andExpect(status().isCreated())
                .andExpect(content().json(warningJson("A pre-release already exists for foo.bar 1.5.0.\n" +
                        "To prevent update conflicts, we recommend that this stable release uses 1.6.0 as its version instead.")));
    }

    @Test
    public void testPublishInvalidName() throws Exception {
        mockForPublish("contributor");
        var bytes = createExtensionPackage("b.a.r", "1.0.0", null);
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

    private void mockActiveVersion() {
        var namespace = new Namespace();
        namespace.setName("foo");
        var extension = new Extension();
        extension.setId(1);
        extension.setName("bar");
        extension.setActive(true);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setVersion("1.0.0");
        extVersion.setActive(true);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);
        extension.getVersions().add(extVersion);
    }

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

    private void mockInactiveExtensionVersion(String namespaceName, String extensionName) {
        var request = new QueryRequest();
        request.namespaceName = namespaceName;
        request.extensionName = extensionName;
        request.size = 100;

        Mockito.when(repositories.findActiveVersions(request))
                .thenReturn(new PageImpl<>(Collections.emptyList(), Pageable.ofSize(request.size), 0));
    }

    private List<ExtensionVersion> mockExtensionVersionVersionsTargetPlatforms() {
        var values = List.of(
                "1.0.0@darwin-x64", "2.0.0@darwin-x64",
                "1.0.0@linux-x64", "2.0.0@linux-x64"
        );

        return mockExtensionVersions(null, values, (ev, value) -> {
            var pieces = value.split("@");
            ev.setVersion(pieces[0]);
            ev.setTargetPlatform(pieces[1]);
            ev.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
            ev.setDisplayName("Foo Bar");
            return ev;
        });
    }

    private List<ExtensionVersion> mockExtensionVersionVersionsTargetPlatforms(String targetPlatform) {
        var versions = List.of("1.0.0", "2.0.0");
        var values = versions.stream().map(version -> version + "@" + targetPlatform).collect(Collectors.toList());
        return mockExtensionVersions(targetPlatform, values, (ev, value) -> {
            var pieces = value.split("@");
            ev.setVersion(pieces[0]);
            ev.setTargetPlatform(pieces[1]);
            ev.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
            ev.setDisplayName("Foo Bar");
            return ev;
        });
    }

    private List<ExtensionVersion> mockExtensionVersionVersions() {
        var versions = List.of("1.0.0", "2.0.0", "3.0.0");
        return mockExtensionVersions(null, versions, (ev, version) -> {
            ev.setVersion(version);
            ev.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
            ev.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
            ev.setDisplayName("Foo Bar");
            return ev;
        });
    }

    private List<ExtensionVersion> mockExtensionVersionTargetPlatforms() {
        var targetPlatforms = List.of("darwin-x64", "linux-x64", "alpine-arm64");
        return mockExtensionVersions(null, targetPlatforms, (ev, targetPlatform) -> {
            ev.setVersion("1.0.0");
            ev.setTargetPlatform(targetPlatform);
            ev.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
            ev.setDisplayName("Foo Bar");
            return ev;
        });
    }

    private List<ExtensionVersion> mockExtensionVersions(String targetPlatform, List<String> values, BiFunction<ExtensionVersion, String, ExtensionVersion> setter) {
        var namespace = new Namespace();
        namespace.setId(1L);
        namespace.setPublicId("1234");
        namespace.setName("foo");

        var extension = new Extension();
        extension.setId(2L);
        extension.setName("bar");
        extension.setNamespace(namespace);

        var versions = new ArrayList<ExtensionVersion>();
        for(var i = 0; i < values.size(); i++) {
            var extVersion = new ExtensionVersion();
            extVersion.setId(3 + i);
            extVersion = setter.apply(extVersion, values.get(i));
            extVersion.setExtension(extension);
            versions.add(extVersion);
        }

        Mockito.when(repositories.findActiveExtensionVersions(Set.of(extension.getId()), null))
                .thenReturn(versions);
        Mockito.when(repositories.findActiveVersionStringsSorted(Set.of(extension.getId()), null))
                .thenReturn(versions.stream().collect(Collectors.groupingBy(ev -> ev.getExtension().getId(), Collectors.mapping(ev -> ev.getVersion(), Collectors.toList()))));
        Mockito.when(repositories.findVersionStringsSorted(extension, targetPlatform, true))
                .thenReturn(versions.stream().map(ExtensionVersion::getVersion).collect(Collectors.toList()));

        var fileTypes = List.of(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG);
        Mockito.when(repositories.findFileResourcesByExtensionVersionIdAndType(List.of(3L), fileTypes))
                .thenReturn(Collections.emptyList());
        Mockito.when(repositories.findNamespaceMemberships(List.of(namespace.getId())))
                .thenReturn(Collections.emptyList());

        return versions;
    }

    private void mockExtensionVersion() {
        var namespace = new Namespace();
        namespace.setId(1L);
        namespace.setPublicId("1234");
        namespace.setName("foo");

        var extension = new Extension();
        extension.setId(2L);
        extension.setPublicId("5678");
        extension.setName("bar");
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setId(3L);
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        extVersion.setDisplayName("Foo Bar");
        extVersion.setExtension(extension);

        Mockito.when(repositories.findActiveExtensionVersions(Set.of(extension.getId()), null))
                .thenReturn(List.of(extVersion));

        Mockito.when(repositories.findActiveVersions(any(QueryRequest.class)))
                .then((Answer<Page<ExtensionVersion>>) invocation -> {
                    var request = invocation.getArgument(0, QueryRequest.class);
                    var versions = namespace.getPublicId().equals(request.namespaceUuid)
                            || namespace.getName().equals(request.namespaceName)
                            || extension.getPublicId().equals(request.extensionUuid)
                            || extension.getName().equals(request.extensionName)
                            ? List.of(extVersion)
                            : Collections.<ExtensionVersion>emptyList();

                    return new PageImpl<>(versions, Pageable.ofSize(100), versions.size());
                });

        var fileTypes = List.of(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG);
        Mockito.when(repositories.findFileResourcesByExtensionVersionIdAndType(Set.of(extVersion.getId()), fileTypes))
                .thenReturn(Collections.emptyList());
        Mockito.when(repositories.findNamespaceMemberships(List.of(namespace.getId())))
                .thenReturn(Collections.emptyList());
    }

    private ExtensionVersion mockExtensionWithSignature() {
        return mockExtension(TargetPlatform.NAME_UNIVERSAL, true);
    }

    private ExtensionVersion mockExtension() {
        return mockExtension(TargetPlatform.NAME_UNIVERSAL);
    }

    private ExtensionVersion mockExtension(String targetPlatform) {
        return mockExtension(targetPlatform, false);
    }

    private ExtensionVersion mockExtension(String targetPlatform, boolean withSignature) {
        var namespace = new Namespace();
        namespace.setName("foo");
        namespace.setPublicId("1234");
        var extension = new Extension();
        extension.setName("bar");
        extension.setId(extension.getName().hashCode());
        extension.setNamespace(namespace);
        extension.setPublicId("5678");
        extension.setActive(true);
        var extVersion = new ExtensionVersion();
        extVersion.setTargetPlatform(targetPlatform);
        extVersion.setVersion("1.0.0");
        extVersion.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        extVersion.setActive(true);
        extVersion.setDisplayName("Foo Bar");
        extVersion.setExtension(extension);
        extension.getVersions().add(extVersion);
        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        Mockito.when(repositories.findExtension("bar", "foo"))
                .thenReturn(extension);
        Mockito.when(repositories.findVersion("1.0.0", targetPlatform, "bar", "foo"))
                .thenReturn(extVersion);
        Mockito.when(repositories.findVersions(extension))
                .thenReturn(Streamable.of(extVersion));
        Mockito.when(repositories.findActiveExtensions(namespace))
                .thenReturn(Streamable.of(extension));
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(0L);
        Mockito.when(repositories.countActiveReviews(extension))
                .thenReturn(0L);
        Mockito.when(repositories.findNamespace("foo"))
                .thenReturn(namespace);
        Mockito.when(repositories.findExtensions("bar"))
                .thenReturn(Streamable.of(extension));
        Mockito.when(repositories.findNamespaceByPublicId("1234"))
                .thenReturn(namespace);
        Mockito.when(repositories.findExtensionByPublicId("5678"))
                .thenReturn(extension);

        var download = new FileResource();
        download.setExtension(extVersion);
        download.setType(DOWNLOAD);
        download.setStorageType(STORAGE_DB);
        download.setName("extension-1.0.0.vsix");
        var signature = new FileResource();
        if(withSignature) {
            signature.setExtension(extVersion);
            signature.setType(DOWNLOAD_SIG);
            signature.setStorageType(STORAGE_DB);
            signature.setName("extension-1.0.0.sigzip");
        }
        Mockito.when(entityManager.merge(download)).thenReturn(download);
        Mockito.when(repositories.findFilesByType(anyCollection(), anyCollection())).thenAnswer(invocation -> {
            Collection<ExtensionVersion> extVersions = invocation.getArgument(0);
            Collection<String> types = invocation.getArgument(1);
            var extensionVersion = extVersions.iterator().hasNext()
                    ? extVersions.iterator().next()
                    : null;

            var files = new ArrayList<>();
            if(types.contains(DOWNLOAD) && download.getExtension().equals(extensionVersion)) {
                files.add(download);
            }
            if(withSignature && types.contains(DOWNLOAD_SIG) && signature.getExtension().equals(extensionVersion)) {
                files.add(signature);
            }

            return files;
        });

        return extVersion;
    }

    private String extensionJson(Consumer<ExtensionJson> content) throws JsonProcessingException {
        var json = new ExtensionJson();
        content.accept(json);
        return new ObjectMapper().writeValueAsString(json);
    }

    private String queryResultJson(Consumer<ExtensionJson>... contents) throws JsonProcessingException {
        var extensionJsons = new ArrayList<String>();
        for(var content : contents) {
            extensionJsons.add(extensionJson(content));
        }

        return "{\"extensions\":[" + String.join(",", extensionJsons) + "]}";
    }

    private FileResource mockReadme() {
        return mockReadme(TargetPlatform.NAME_UNIVERSAL);
    }

    private FileResource mockReadme(String targetPlatform) {
        var extVersion = mockExtension(targetPlatform);
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName("README");
        resource.setType(FileResource.README);
        resource.setContent("Please read me".getBytes());
        resource.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(resource)).thenReturn(resource);
        Mockito.when(repositories.findFileByName(extVersion, "README"))
                .thenReturn(resource);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.README))
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
        Mockito.when(entityManager.merge(resource)).thenReturn(resource);
        Mockito.when(repositories.findFileByName(extVersion, "CHANGELOG"))
                .thenReturn(resource);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.CHANGELOG))
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
        Mockito.when(entityManager.merge(resource)).thenReturn(resource);
        Mockito.when(repositories.findFileByName(extVersion, "LICENSE"))
                .thenReturn(resource);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.LICENSE))
                .thenReturn(resource);
        return resource;
    }

    private FileResource mockLatest() {
        var extVersion = mockExtension();
        var resource = new FileResource();
        resource.setExtension(extVersion);
        resource.setName("DOWNLOAD");
        resource.setType(FileResource.DOWNLOAD);
        resource.setContent("latest download".getBytes());
        resource.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(resource)).thenReturn(resource);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.DOWNLOAD))
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
        extension.setId(1L);
        var entry1 = new ExtensionSearch();
        entry1.id = 1;
        var searchHit = new SearchHit<>("0", "1", null, 1.0f, null, null, null, null, null, null, entry1);
        var searchHits = new SearchHitsImpl<>(1, TotalHitsRelation.EQUAL_TO, 1.0f, "1", null, List.of(searchHit), null, null);
        Mockito.when(search.isEnabled())
                .thenReturn(true);
        var searchOptions = new ISearchService.Options("foo", null, null, 10, 0, "desc", "relevance", false);
        Mockito.when(search.search(searchOptions))
                .thenReturn(searchHits);
        Mockito.when(repositories.findExtensions(Set.of(extension.getId())))
                .thenReturn(Streamable.of(extension));
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
            extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
            extVersion.setVersion("1.0.0");
            extVersion.setActive(true);
            Mockito.when(repositories.findExtension("bar", namespace))
                    .thenReturn(extension);
            Mockito.when(repositories.findVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, extension))
                    .thenReturn(extVersion);
        }
        Mockito.when(repositories.countActiveReviews(any(Extension.class)))
                .thenReturn(0L);
        Mockito.when(repositories.findVersions(any(Extension.class)))
                .thenReturn(Streamable.empty());
        Mockito.when(repositories.findFilesByType(anyCollection(), anyCollection()))
                .thenReturn(Collections.emptyList());
        Mockito.when(repositories.findVersions(eq("1.0.0"), any(Extension.class)))
                .thenReturn(Streamable.empty());
        if (mode.equals("owner")) {
            var ownerMem = new NamespaceMembership();
            ownerMem.setUser(token.getUser());
            ownerMem.setNamespace(namespace);
            ownerMem.setRole(NamespaceMembership.ROLE_OWNER);
            Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.of(ownerMem));
            Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(1L);
            Mockito.when(repositories.findMembership(token.getUser(), namespace))
                    .thenReturn(ownerMem);
            Mockito.when(repositories.isVerified(namespace, token.getUser()))
                    .thenReturn(true);
        } else if (mode.equals("contributor") || mode.equals("sole-contributor") || mode.equals("existing")) {
            var contribMem = new NamespaceMembership();
            contribMem.setUser(token.getUser());
            contribMem.setNamespace(namespace);
            contribMem.setRole(NamespaceMembership.ROLE_CONTRIBUTOR);
            Mockito.when(repositories.findMembership(token.getUser(), namespace))
                    .thenReturn(contribMem);
            Mockito.when(repositories.isVerified(namespace, token.getUser()))
                    .thenReturn(true);
            if (mode.equals("contributor")) {
                var otherUser = new UserData();
                otherUser.setLoginName("other_user");
                var ownerMem = new NamespaceMembership();
                ownerMem.setUser(otherUser);
                ownerMem.setNamespace(namespace);
                ownerMem.setRole(NamespaceMembership.ROLE_OWNER);
                Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                        .thenReturn(Streamable.of(ownerMem));
                Mockito.when(repositories.isVerified(namespace, token.getUser()))
                        .thenReturn(true);
            } else {
                Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.empty());
                Mockito.when(repositories.isVerified(namespace, token.getUser()))
                        .thenReturn(false);
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
                    .thenReturn(1L);
            if (mode.equals("privileged")) {
                token.getUser().setRole(UserData.ROLE_PRIVILEGED);
            }
        } else {
            Mockito.when(repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(Streamable.empty());
            Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                    .thenReturn(0L);
        }

        Mockito.when(entityManager.merge(any(Extension.class)))
                .then((Answer<Extension>) invocation -> invocation.getArgument(0, Extension.class));
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

    private String warningJson(String message) throws JsonProcessingException {
        var json = ResultJson.warning(message);
        return new ObjectMapper().writeValueAsString(json);
    }

    private byte[] createExtensionPackage(String name, String version, String license) throws IOException {
        return createExtensionPackage(name, version, license, false, null);
    }

    private byte[] createExtensionPackage(String name, String version, String license, boolean preRelease, String targetPlatform) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var archive = new ZipOutputStream(bytes);
        archive.putNextEntry(new ZipEntry("extension.vsixmanifest"));
        var vsixmanifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<PackageManifest Version=\"2.0.0\" xmlns=\"http://schemas.microsoft.com/developer/vsx-schema/2011\" xmlns:d=\"http://schemas.microsoft.com/developer/vsx-schema-design/2011\">" +
            "<Metadata>" +
            "<Identity Language=\"en-US\" Id=\""+ name +"\" Version=\"" + version + "\" Publisher=\"foo\" " + (targetPlatform != null ? "TargetPlatform=\"" + targetPlatform + "\"" : "")  + " />" +
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
            (preRelease ? "<Property Id=\"Microsoft.VisualStudio.Code.PreRelease\" Value=\"true\" />" : "") +
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
    @Import(SecurityConfig.class)
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

        @Bean
        ExtensionJsonCacheKeyGenerator extensionJsonCacheKeyGenerator() { return new ExtensionJsonCacheKeyGenerator(); }

        @Bean
        VersionService versionService() {
            return new VersionService();
        }

        @Bean
        LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKeyGenerator() {
            return new LatestExtensionVersionCacheKeyGenerator();
        }

        @Bean
        PublishExtensionVersionHandler publishExtensionVersionHandler() {
            return new PublishExtensionVersionHandler();
        }
    }
}