/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import static org.eclipse.openvsx.entities.FileResource.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.MockTransactionTemplate;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.cache.LatestExtensionVersionCacheKeyGenerator;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.util.Streamable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@WebMvcTest(VSCodeAPI.class)
@AutoConfigureWebClient
@MockBean({
    ClientRegistrationRepository.class, GoogleCloudStorageService.class, AzureBlobStorageService.class,
    AzureDownloadCountService.class, CacheService.class, UpstreamVSCodeService.class,
    VSCodeIdService.class, EntityManager.class, EclipseService.class, ExtensionValidator.class,
    SimpleMeterRegistry.class
})
public class VSCodeAPITest {

    @MockBean
    EntityManager entityManager;

    @MockBean
    RepositoryService repositories;

    @MockBean
    SearchUtilService search;

    @MockBean
    ExtensionVersionIntegrityService integrityService;

    @Autowired
    MockMvc mockMvc;

    @Test
    public void testSearch() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersions(extension, null, "universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response.json")));
    }

    @Test
    public void testSearchMacOSXTarget() throws Exception {
        var targetPlatform = "darwin-x64";
        var extension = mockSearch(targetPlatform, true);
        mockExtensionVersions(extension, targetPlatform, targetPlatform);

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query-darwin.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response-darwin.json")));
    }

    @Test
    public void testSearchExcludeBuiltInExtensions() throws Exception {
        var extension = mockSearch(null, "vscode", true);
        mockExtensionVersions(extension, null,"universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response-builtin-extensions.json")));
    }

    @Test
    public void testSearchMultipleTargetsResponse() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersions(extension, null, "darwin-x64", "linux-x64", "alpine-arm64");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response-targets.json")));
    }

    @Test
    public void testFindById() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersions(extension, null, "universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findid-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findid-yaml-response.json")));
    }

    @Test
    public void testFindByIdAlpineTarget() throws Exception {
        var targetPlatform = "alpine-arm64";
        var extension = mockSearch(targetPlatform, true);
        mockExtensionVersions(extension, targetPlatform, targetPlatform);

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findid-yaml-query-alpine.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findid-yaml-response-alpine.json")));
    }

    @Test
    public void testFindByIdDuplicate() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersions(extension, null, "universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findid-yaml-duplicate-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findid-yaml-response.json")));
    }

    @Test
    public void testFindByIdInactive() throws Exception {
        mockSearch(false);
        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findid-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("empty-response.json")));
    }

    @Test
    public void testFindByName() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersions(extension, null, "universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findname-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findname-yaml-response.json")));
    }

    @Test
    public void testFindByNameLinuxTarget() throws Exception {
        var targetPlatform = "linux-x64";
        var extension = mockSearch(targetPlatform, true);
        mockExtensionVersions(extension, targetPlatform, targetPlatform);

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findname-yaml-query-linux.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findname-yaml-response-linux.json")));
    }

    @Test
    public void testFindByNameDuplicate() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersions(extension, null,"universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findname-yaml-duplicate-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findname-yaml-response.json")));
    }

    @Test
    public void testAsset() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                    "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"foo\":\"bar\"}"));
    }

    @Test
    public void testAssetMacOSX() throws Exception {
        var target = "darwin-arm64";
        mockExtensionVersion(target);
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}?targetPlatform={target}",
                "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest", target))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"foo\":\"bar\",\"target\":\"darwin-arm64\"}"));
    }

    @Test
    public void testAssetNotFound() throws Exception {
        var extVersion = mockExtensionVersion();
        Mockito.when(repositories.findFileByType(extVersion, FileResource.MANIFEST))
                .thenReturn(null);
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                    "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetItem() throws Exception {
        var extension = mockExtension();
        extension.setActive(true);
        Mockito.when(repositories.findExtension("vscode-yaml", "redhat")).thenReturn(extension);
        mockMvc.perform(get("/vscode/item?itemName={itemName}", "redhat.vscode-yaml"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/extension/redhat/vscode-yaml"));
    }

    @Test
    public void testWebResourceAsset() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.WebResources/extension/img/logo.png"))
                .andExpect(status().isOk())
                .andExpect(content().string("logo.png"));
    }

    @Test
    public void testNotWebResourceAsset() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.WebResources/img/logo.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssetExcludeBuiltInExtensions() throws Exception {
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                "vscode", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Built-in extension namespace 'vscode' not allowed"));
    }

    @Test
    public void testGetItemExcludeBuiltInExtensions() throws Exception {
        mockMvc.perform(get("/vscode/item?itemName={itemName}", "vscode.vscode-yaml"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Built-in extension namespace 'vscode' not allowed"));
    }

    @Test
    public void testGetItemBadRequest() throws Exception {
        mockMvc.perform(get("/vscode/item?itemName={itemName}", "vscode-yaml"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Expecting an item of the form `{publisher}.{name}`"));
    }

    @Test
    public void testBrowseNotFound() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);

        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        Mockito.when(repositories.findResourceFileResources(1L, "extension/img"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension/img"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBrowseExcludeBuiltInExtensions() throws Exception {
        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "vscode", "bar", "1.3.4", "extension/img"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Built-in extension namespace 'vscode' not allowed"));
    }

    @Test
    public void testBrowseTopDir() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);

        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        var vsixResource = mockFileResource(15, extVersion, "extension.vsixmanifest", RESOURCE, STORAGE_DB, "<xml></xml>".getBytes(StandardCharsets.UTF_8));
        var manifestResource = mockFileResource(16, extVersion, "extension/package.json", RESOURCE, STORAGE_DB, "{\"package\":\"json\"}".getBytes(StandardCharsets.UTF_8));
        var readmeResource = mockFileResource(17, extVersion, "extension/README.md", RESOURCE, STORAGE_DB, "README".getBytes(StandardCharsets.UTF_8));
        var changelogResource = mockFileResource(18, extVersion, "extension/CHANGELOG.md", RESOURCE, STORAGE_DB, "CHANGELOG".getBytes(StandardCharsets.UTF_8));
        var licenseResource = mockFileResource(19, extVersion, "extension/LICENSE.txt", RESOURCE, STORAGE_DB, "LICENSE".getBytes(StandardCharsets.UTF_8));
        var iconResource = mockFileResource(20, extVersion, "extension/images/icon128.png", RESOURCE, STORAGE_DB, "ICON128".getBytes(StandardCharsets.UTF_8));

        Mockito.when(repositories.findResourceFileResources(1L, ""))
                .thenReturn(List.of(vsixResource, manifestResource, readmeResource, changelogResource, licenseResource, iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}", namespaceName, extensionName, version))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension.vsixmanifest\",\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/\"]"));
    }

    @Test
    public void testBrowseVsixManifest() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);

        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        var content = "<xml></xml>".getBytes(StandardCharsets.UTF_8);
        var vsixResource = mockFileResource(15, extVersion, "extension.vsixmanifest", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findResourceFileResources(1L, "extension.vsixmanifest"))
                .thenReturn(List.of(vsixResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension.vsixmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testBrowseVsixManifestUniversal() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var targetPlatforms = List.of(TargetPlatform.NAME_UNIVERSAL, TargetPlatform.NAME_WIN32_X64, TargetPlatform.NAME_LINUX_X64);
        var extVersions = new ArrayList<ExtensionVersion>(targetPlatforms.size());
        for(var i = 0; i < targetPlatforms.size(); i++) {
            var extVersion = new ExtensionVersion();
            extVersion.setId(i + 1);
            extVersion.setVersion(version);
            extVersion.setTargetPlatform(targetPlatforms.get(i));
            extVersion.setExtension(extension);
            extVersions.add(extVersion);
        }

        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(extVersions);

        var content = "<xml></xml>".getBytes(StandardCharsets.UTF_8);
        var vsixResource = mockFileResource(15, extVersions.get(0), "extension.vsixmanifest", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findResourceFileResources(1L, "extension.vsixmanifest"))
                .thenReturn(List.of(vsixResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension.vsixmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testBrowseVsixManifestWindows() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var targetPlatforms = List.of(TargetPlatform.NAME_DARWIN_X64, TargetPlatform.NAME_LINUX_X64, TargetPlatform.NAME_WIN32_X64);
        var extVersions = new ArrayList<ExtensionVersion>(targetPlatforms.size());
        for(var i = 0; i < targetPlatforms.size(); i++) {
            var extVersion = new ExtensionVersion();
            extVersion.setId(i + 2);
            extVersion.setVersion(version);
            extVersion.setTargetPlatform(targetPlatforms.get(i));
            extVersion.setExtension(extension);
            extVersions.add(extVersion);
        }

        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(extVersions);

        var content = "<xml></xml>".getBytes(StandardCharsets.UTF_8);
        var vsixResource = mockFileResource(15, extVersions.get(2), "extension.vsixmanifest", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findResourceFileResources(4L, "extension.vsixmanifest"))
                .thenReturn(List.of(vsixResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension.vsixmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testBrowseExtensionDir() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);

        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        var manifestResource =  mockFileResource(16, extVersion, "extension/package.json", RESOURCE, STORAGE_DB, "{\"package\":\"json\"}".getBytes(StandardCharsets.UTF_8));
        var readmeResource = mockFileResource(17, extVersion, "extension/README.md", RESOURCE, STORAGE_DB, "README".getBytes(StandardCharsets.UTF_8));
        var changelogResource = mockFileResource(18, extVersion, "extension/CHANGELOG.md", RESOURCE, STORAGE_DB, "CHANGELOG".getBytes(StandardCharsets.UTF_8));
        var licenseResource = mockFileResource(19, extVersion, "extension/LICENSE.txt", RESOURCE, STORAGE_DB, "LICENSE".getBytes(StandardCharsets.UTF_8));
        var iconResource = mockFileResource(20, extVersion, "extension/images/icon128.png", RESOURCE, STORAGE_DB, "ICON128".getBytes(StandardCharsets.UTF_8));
        Mockito.when(repositories.findResourceFileResources(1L, "extension"))
                .thenReturn(List.of(manifestResource, readmeResource, changelogResource, licenseResource, iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[" +
                        "\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/package.json\"," +
                        "\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/README.md\"," +
                        "\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/CHANGELOG.md\"," +
                        "\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/LICENSE.txt\"," +
                        "\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/images/\"" +
                        "]"));
    }

    @Test
    public void testBrowsePackageJson() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);
        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        var content = "{\"package\":\"json\"}".getBytes(StandardCharsets.UTF_8);
        var manifestResource = mockFileResource(16, extVersion, "extension/package.json", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findResourceFileResources(1L, "extension/package.json"))
                .thenReturn(List.of(manifestResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension/package.json"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testBrowseImagesDir() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);
        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        var content = "ICON128".getBytes(StandardCharsets.UTF_8);
        var iconResource = mockFileResource(20, extVersion, "extension/images/icon128.png", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findResourceFileResources(1L, "extension/images"))
                .thenReturn(List.of(iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension/images/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/images/icon128.png\"]"));
    }

    @Test
    public void testBrowseIcon() throws Exception {
        var version = "1.3.4";
        var extensionName = "bar";
        var namespaceName = "foo";
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var extension = new Extension();
        extension.setId(0L);
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);
        Mockito.when(repositories.findActiveExtensionVersionsByVersion(version, extensionName, namespaceName))
                .thenReturn(List.of(extVersion));

        var content = "ICON128".getBytes(StandardCharsets.UTF_8);
        var iconResource = mockFileResource(20, extVersion, "extension/images/icon128.png", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findResourceFileResources(1L, "extension/images/icon128.png"))
                .thenReturn(List.of(iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", namespaceName, extensionName, version, "extension/images/icon128.png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testDownload() throws Exception {
        mockExtensionVersion();
        mockMvc.perform(get("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage",
                "redhat", "vscode-yaml", "0.5.2"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/vscode/asset/redhat/vscode-yaml/0.5.2/Microsoft.VisualStudio.Services.VSIXPackage"));
    }

    @Test
    public void testDownloadMacOSX() throws Exception {
        mockExtensionVersion("darwin-arm64");
        mockMvc.perform(get("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage?targetPlatform={target}",
                "redhat", "vscode-yaml", "0.5.2", "darwin-arm64"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/vscode/asset/redhat/vscode-yaml/0.5.2/Microsoft.VisualStudio.Services.VSIXPackage?targetPlatform=darwin-arm64"));
    }

    @Test
    public void testDownloadExcludeBuiltInExtensions() throws Exception {
        mockMvc.perform(get("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage",
                "vscode", "vscode-yaml", "0.5.2"))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Built-in extension namespace 'vscode' not allowed"));
    }

    // ---------- UTILITY ----------//
    private Extension mockSearch(boolean active) {
        return mockSearch(null, active);
    }

    private Extension mockSearch(String targetPlatform, boolean active) {
        return mockSearch(targetPlatform, null, active);
    }

    private Extension mockSearch(String targetPlatform, String namespaceName, boolean active) {
        var builtInExtensionNamespace = "vscode";
        var entry1 = new ExtensionSearch();
        entry1.id = 1;
        List<SearchHit<ExtensionSearch>> searchResults = !builtInExtensionNamespace.equals(namespaceName)
                ? Collections.singletonList(new SearchHit<>("0", "1", null, 1.0f, null, null, null, null, null, null, entry1))
                : Collections.emptyList();
        var searchHits = new SearchHitsImpl<>(searchResults.size(), TotalHitsRelation.EQUAL_TO, 1.0f, "1", null,
                searchResults, null, null);

        Mockito.when(integrityService.isEnabled())
                .thenReturn(true);
        Mockito.when(search.isEnabled())
                .thenReturn(true);
        var searchOptions = new ISearchService.Options("yaml", null, targetPlatform, 50, 0, "desc", "relevance", false, builtInExtensionNamespace);
        Mockito.when(search.search(searchOptions))
                .thenReturn(searchHits);

        var extension = mockExtension();
        List<Extension> results = active ? List.of(extension) : Collections.emptyList();
        Mockito.when(repositories.findActiveExtensionsById(List.of(entry1.id)))
                .thenReturn(results);

        var publicIds = Set.of(extension.getPublicId());
        Mockito.when(repositories.findActiveExtensionsByPublicId(publicIds, builtInExtensionNamespace))
                .thenReturn(results);

        var ids = List.of(extension.getId());
        Mockito.when(repositories.findActiveExtension(extension.getName(), extension.getNamespace().getName()))
                .thenReturn(extension);

        mockExtensionVersions(extension, targetPlatform, targetPlatform);
        return extension;
    }

    private Extension mockExtension() {
            var namespace = new Namespace();
            namespace.setId(2);
            namespace.setPublicId("test-2");
            namespace.setName("redhat");

            var extension = new Extension();
            extension.setId(1);
            extension.setPublicId("test-1");
            extension.setName("vscode-yaml");
            extension.setAverageRating(3.0);
            extension.setReviewCount(10L);
            extension.setDownloadCount(100);
            extension.setPublishedDate(LocalDateTime.parse("1999-12-01T09:00"));
            extension.setLastUpdatedDate(LocalDateTime.parse("2000-01-01T10:00"));
            extension.setNamespace(namespace);

            return extension;
    }

    private void mockExtensionVersions(Extension extension, String queryTargetPlatform, String... targetPlatforms) {
        var id = 2;
        var versions = new ArrayList<ExtensionVersion>(targetPlatforms.length);
        for(var targetPlatform : targetPlatforms) {
            versions.add(mockExtensionVersion(extension, id, targetPlatform));
            id++;
        }

        Mockito.when(repositories.findActiveExtensionVersions(Set.of(extension.getId()), queryTargetPlatform))
                .thenReturn(versions);

        mockFileResources(versions);
    }

    private ExtensionVersion mockExtensionVersion(Extension extension, long id, String targetPlatform) {
        var extVersion = new ExtensionVersion();
        extVersion.setId(id);
        extVersion.setVersion("0.5.2");
        extVersion.setTargetPlatform(targetPlatform);
        extVersion.setPreview(true);
        extVersion.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        extVersion.setDisplayName("YAML");
        extVersion.setDescription("YAML Language Support");
        extVersion.setEngines(List.of("vscode@^1.31.0"));
        extVersion.setRepository("https://github.com/redhat-developer/vscode-yaml");
        extVersion.setDependencies(Collections.emptyList());
        extVersion.setBundledExtensions(Collections.emptyList());
        extVersion.setLocalizedLanguages(Collections.emptyList());
        extVersion.setExtension(extension);

        var keyPair = new SignatureKeyPair();
        keyPair.setPublicId("123-456-789");
        extVersion.setSignatureKeyPair(keyPair);

        mockFileResources(List.of(extVersion));
        return extVersion;
    }

    private void mockFileResources(List<ExtensionVersion> extensionVersions) {
        var types = List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG, VSIXMANIFEST, DOWNLOAD_SIG);

        var files = new ArrayList<FileResource>();
        for(var extVersion : extensionVersions) {
            var id = extVersion.getId();
            files.add(mockFileResource(id * 100 + 5, extVersion, "redhat.vscode-yaml-0.5.2.vsix", DOWNLOAD));
            files.add(mockFileResource(id * 100 + 6, extVersion, "package.json", MANIFEST));
            files.add(mockFileResource(id * 100 + 7, extVersion, "README.md", README));
            files.add(mockFileResource(id * 100 + 8, extVersion, "CHANGELOG.md", CHANGELOG));
            files.add(mockFileResource(id * 100 + 9, extVersion, "LICENSE.txt", LICENSE));
            files.add(mockFileResource(id * 100 + 10, extVersion, "icon128.png", ICON));
            files.add(mockFileResource(id * 100 + 11, extVersion, "extension.vsixmanifest", VSIXMANIFEST));
            files.add(mockFileResource(id * 100 + 12, extVersion, "redhat.vscode-yaml-0.5.2.sigzip", DOWNLOAD_SIG));
            files.add(mockFileResource(id * 100 + 13, extVersion, "extension/themes/dark.json", RESOURCE));
            files.add(mockFileResource(id * 100 + 14, extVersion, "extension/img/logo.png", RESOURCE));
        }

        var ids = extensionVersions.stream().map(ExtensionVersion::getId).collect(Collectors.toSet());
        Mockito.when(repositories.findFileResourcesByExtensionVersionIdAndType(ids, types))
                .thenReturn(files);
    }

    private FileResource mockFileResource(long id, ExtensionVersion extVersion, String name, String type) {
        var resource = new FileResource();
        resource.setId(id);
        resource.setExtension(extVersion);
        resource.setName(name);
        resource.setType(type);

        return resource;
    }

    private FileResource mockFileResource(long id, ExtensionVersion extVersion, String name, String type, String storageType, byte[] content) {
        var resource = mockFileResource(id, extVersion, name, type);
        resource.setStorageType(storageType);
        resource.setContent(content);

        return resource;
    }

    private ExtensionVersion mockExtensionVersion() throws JsonProcessingException {
        return mockExtensionVersion(TargetPlatform.NAME_UNIVERSAL);
    }

    private ExtensionVersion mockExtensionVersion(String targetPlatform) throws JsonProcessingException {
        var namespace = new Namespace();
        namespace.setId(2);
        namespace.setPublicId("test-2");
        namespace.setName("redhat");
        var extension = new Extension();
        extension.setId(1);
        extension.setPublicId("test-1");
        extension.setName("vscode-yaml");
        extension.setActive(true);
        extension.setDownloadCount(100);
        extension.setAverageRating(3.0);
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extension.getVersions().add(extVersion);
        extVersion.setTargetPlatform(targetPlatform);
        extVersion.setVersion("0.5.2");
        extVersion.setPreRelease(true);
        extVersion.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        extVersion.setActive(true);
        extVersion.setDisplayName("YAML");
        extVersion.setDescription("YAML Language Support");
        extVersion.setRepository("https://github.com/redhat-developer/vscode-yaml");
        extVersion.setEngines(Lists.newArrayList("vscode@^1.31.0"));
        extVersion.setDependencies(Lists.newArrayList());
        extVersion.setBundledExtensions(Lists.newArrayList());
        Mockito.when(repositories.findExtensionByPublicId("test-1"))
                .thenReturn(extension);
        Mockito.when(repositories.findExtension("vscode-yaml", "redhat"))
                .thenReturn(extension);
        Mockito.when(repositories.findVersion("0.5.2", targetPlatform, "vscode-yaml", "redhat"))
                .thenReturn(extVersion);
        Mockito.when(repositories.findVersions(extension))
                .thenReturn(Streamable.of(extVersion));
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(0L);
        var extensionFile = new FileResource();
        extensionFile.setExtension(extVersion);
        extensionFile.setName("redhat.vscode-yaml-0.5.2.vsix");
        extensionFile.setType(FileResource.DOWNLOAD);
        extensionFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(extensionFile)).thenReturn(extensionFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.DOWNLOAD))
                .thenReturn(extensionFile);
        var manifestFile = new FileResource();
        manifestFile.setExtension(extVersion);
        manifestFile.setName("package.json");
        manifestFile.setType(FileResource.MANIFEST);
        var manifestContent = new HashMap<String, String>();
        manifestContent.put("foo", "bar");
        if(!targetPlatform.equals(TargetPlatform.NAME_UNIVERSAL))
            manifestContent.put("target", targetPlatform);
        manifestFile.setContent(new ObjectMapper().writeValueAsBytes(manifestContent));
        manifestFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(manifestFile)).thenReturn(manifestFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.MANIFEST))
                .thenReturn(manifestFile);
        var readmeFile = new FileResource();
        readmeFile.setExtension(extVersion);
        readmeFile.setName("README.md");
        readmeFile.setType(FileResource.README);
        readmeFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(readmeFile)).thenReturn(readmeFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.README))
                .thenReturn(readmeFile);
        var changelogFile = new FileResource();
        changelogFile.setExtension(extVersion);
        changelogFile.setName("CHANGELOG.md");
        changelogFile.setType(FileResource.CHANGELOG);
        changelogFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(changelogFile)).thenReturn(changelogFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.CHANGELOG))
                .thenReturn(changelogFile);
        var licenseFile = new FileResource();
        licenseFile.setExtension(extVersion);
        licenseFile.setName("LICENSE.txt");
        licenseFile.setType(FileResource.LICENSE);
        licenseFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(licenseFile)).thenReturn(licenseFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.LICENSE))
                .thenReturn(licenseFile);
        var iconFile = new FileResource();
        iconFile.setExtension(extVersion);
        iconFile.setName("icon128.png");
        iconFile.setType(FileResource.ICON);
        iconFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(iconFile)).thenReturn(iconFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.ICON))
                .thenReturn(iconFile);
        var vsixManifestFile = new FileResource();
        vsixManifestFile.setExtension(extVersion);
        vsixManifestFile.setName("extension.vsixmanifest");
        vsixManifestFile.setType(VSIXMANIFEST);
        vsixManifestFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(vsixManifestFile)).thenReturn(vsixManifestFile);
        Mockito.when(repositories.findFileByType(extVersion, VSIXMANIFEST))
                .thenReturn(vsixManifestFile);
        var signatureFile = new FileResource();
        signatureFile.setExtension(extVersion);
        signatureFile.setName("redhat.vscode-yaml-0.5.2.sigzip");
        signatureFile.setType(FileResource.DOWNLOAD_SIG);
        signatureFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(entityManager.merge(signatureFile)).thenReturn(signatureFile);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.DOWNLOAD_SIG))
                .thenReturn(signatureFile);
        var webResourceFile = new FileResource();
        webResourceFile.setExtension(extVersion);
        webResourceFile.setName("extension/img/logo.png");
        webResourceFile.setType(FileResource.RESOURCE);
        webResourceFile.setStorageType(STORAGE_DB);
        webResourceFile.setContent("logo.png".getBytes());
        Mockito.when(entityManager.merge(webResourceFile)).thenReturn(webResourceFile);
        Mockito.when(repositories.findFileByTypeAndName(extVersion, FileResource.RESOURCE, "extension/img/logo.png"))
                .thenReturn(webResourceFile);
        Mockito.when(repositories.findFilesByType(anyCollection(), anyCollection())).thenAnswer(invocation -> {
            Collection<ExtensionVersion> extVersions = invocation.getArgument(0);
            var types = invocation.getArgument(1);
            var expectedTypes = Arrays.asList(FileResource.MANIFEST, FileResource.README, FileResource.LICENSE, FileResource.ICON, FileResource.DOWNLOAD, FileResource.CHANGELOG, VSIXMANIFEST, DOWNLOAD_SIG);
            return types.equals(expectedTypes) && extVersions.iterator().hasNext() && extVersion.equals(extVersions.iterator().next())
                    ? Streamable.of(manifestFile, readmeFile, licenseFile, iconFile, extensionFile, changelogFile, vsixManifestFile, signatureFile)
                    : Streamable.empty();
        });

        return extVersion;
    }

    private String file(String name) throws UnsupportedEncodingException, IOException {
        try (var stream = getClass().getResourceAsStream(name)) {
            return CharStreams.toString(new InputStreamReader(stream, "UTF-8"));
        }
    }

    @TestConfiguration
    @Import(SecurityConfig.class)
    static class TestConfig {
        @Bean
        IExtensionQueryRequestHandler extensionQueryRequestHandler(LocalVSCodeService local, UpstreamVSCodeService upstream) {
            return new DefaultExtensionQueryRequestHandler(local, upstream);
        }

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
        LocalVSCodeService localVSCodeService() {
            return new LocalVSCodeService();
        }

        @Bean
        UserService userService() {
            return new UserService();
        }

        @Bean
        StorageUtilService storageUtilService() {
            return new StorageUtilService();
        }

        @Bean
        VersionService getVersionService() {
            return new VersionService();
        }

        @Bean
        LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKeyGenerator() {
            return new LatestExtensionVersionCacheKeyGenerator();
        }
    }

}
