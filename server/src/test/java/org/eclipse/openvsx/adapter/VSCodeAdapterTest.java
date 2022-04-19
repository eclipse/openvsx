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

import javax.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import net.javacrumbs.shedlock.core.LockProvider;
import org.eclipse.openvsx.MockTransactionTemplate;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.dto.ExtensionDTO;
import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.dto.FileResourceDTO;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
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
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionUtil;
import org.elasticsearch.search.aggregations.Aggregations;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(VSCodeAdapter.class)
@AutoConfigureWebClient
@MockBean({
    ClientRegistrationRepository.class, GoogleCloudStorageService.class, AzureBlobStorageService.class,
    AzureDownloadCountService.class, LockProvider.class, CacheService.class
})
public class VSCodeAdapterTest {

    @MockBean
    RepositoryService repositories;

    @MockBean
    VSCodeIdService idService;

    @MockBean
    SearchUtilService search;

    @MockBean
    EntityManager entityManager;

    @MockBean
    EclipseService eclipse;

    @Autowired
    MockMvc mockMvc;

    @Test
    public void testSearch() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersionDTOs(extension, null, "universal");

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
        mockExtensionVersionDTOs(extension, targetPlatform, targetPlatform);

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query-darwin.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response-darwin.json")));
    }

    @Test
    public void testSearchMultipleTargetsResponse() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersionDTOs(extension, null, "darwin-x64", "linux-x64", "alpine-arm64");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response-targets.json")));
    }

    @Test
    public void testFindById() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersionDTOs(extension, null, "universal");

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
        mockExtensionVersionDTOs(extension, targetPlatform, targetPlatform);

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findid-yaml-query-alpine.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findid-yaml-response-alpine.json")));
    }

    @Test
    public void testFindByIdDuplicate() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersionDTOs(extension, null, "universal");

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
        mockExtensionVersionDTOs(extension, null, "universal");

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
        mockExtensionVersionDTOs(extension, targetPlatform, targetPlatform);

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findname-yaml-query-linux.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findname-yaml-response-linux.json")));
    }

    @Test
    public void testFindByNameDuplicate() throws Exception {
        var extension = mockSearch(true);
        mockExtensionVersionDTOs(extension, null,"universal");

        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findname-yaml-duplicate-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findname-yaml-response.json")));
    }

    @Test
    public void testAsset() throws Exception {
        mockExtension();
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                    "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"foo\":\"bar\"}"));
    }

    @Test
    public void testAssetMacOSX() throws Exception {
        var target = "darwin-arm64";
        mockExtension(target);
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}?targetPlatform={target}",
                "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest", target))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"foo\":\"bar\",\"target\":\"darwin-arm64\"}"));
    }

    @Test
    public void testAssetNotFound() throws Exception {
        var extVersion = mockExtension();
        Mockito.when(repositories.findFileByType(extVersion, FileResource.MANIFEST))
                .thenReturn(null);
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                    "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetItem() throws Exception {
        mockMvc.perform(get("/vscode/item?itemName={itemName}", "redhat.vscode-yaml"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/extension/redhat/vscode-yaml"));
    }

    @Test
    public void testWebResourceAsset() throws Exception {
        mockExtension();
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.WebResources/extension/img/logo.png"))
                .andExpect(status().isOk())
                .andExpect(content().string("logo.png"));
    }

    @Test
    public void testNotWebResourceAsset() throws Exception {
        mockExtension();
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.WebResources/img/logo.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBrowseNotFound() throws Exception {
        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", "extension/img"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "foo", "bar", "1.3.4", "extension/img"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBrowseTopDir() throws Exception {
        var vsixResource = new FileResourceDTO(15, 1, "extension.vsixmanifest", RESOURCE, STORAGE_DB, "<xml></xml>".getBytes(StandardCharsets.UTF_8));
        var manifestResource = new FileResourceDTO(16, 1, "extension/package.json", RESOURCE, STORAGE_DB, "{\"package\":\"json\"}".getBytes(StandardCharsets.UTF_8));
        var readmeResource = new FileResourceDTO(17, 1, "extension/README.md", RESOURCE, STORAGE_DB, "README".getBytes(StandardCharsets.UTF_8));
        var changelogResource = new FileResourceDTO(18, 1, "extension/CHANGELOG.md", RESOURCE, STORAGE_DB, "CHANGELOG".getBytes(StandardCharsets.UTF_8));
        var licenseResource = new FileResourceDTO(19, 1, "extension/LICENSE.txt", RESOURCE, STORAGE_DB, "LICENSE".getBytes(StandardCharsets.UTF_8));
        var iconResource = new FileResourceDTO(20, 1, "extension/images/icon128.png", RESOURCE, STORAGE_DB, "ICON128".getBytes(StandardCharsets.UTF_8));
        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", ""))
                .thenReturn(List.of(vsixResource, manifestResource, readmeResource, changelogResource, licenseResource, iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}", "foo", "bar", "1.3.4"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension.vsixmanifest\",\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/\"]"));
    }

    @Test
    public void testBrowseVsixManifest() throws Exception {
        var content = "<xml></xml>".getBytes(StandardCharsets.UTF_8);
        var vsixResource = new FileResourceDTO(15, 1, "extension.vsixmanifest", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", "extension.vsixmanifest"))
                .thenReturn(List.of(vsixResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "foo", "bar", "1.3.4", "extension.vsixmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testBrowseExtensionDir() throws Exception {
        var manifestResource = new FileResourceDTO(16, 1, "extension/package.json", RESOURCE, STORAGE_DB, "{\"package\":\"json\"}".getBytes(StandardCharsets.UTF_8));
        var readmeResource = new FileResourceDTO(17, 1, "extension/README.md", RESOURCE, STORAGE_DB, "README".getBytes(StandardCharsets.UTF_8));
        var changelogResource = new FileResourceDTO(18, 1, "extension/CHANGELOG.md", RESOURCE, STORAGE_DB, "CHANGELOG".getBytes(StandardCharsets.UTF_8));
        var licenseResource = new FileResourceDTO(19, 1, "extension/LICENSE.txt", RESOURCE, STORAGE_DB, "LICENSE".getBytes(StandardCharsets.UTF_8));
        var iconResource = new FileResourceDTO(20, 1, "extension/images/icon128.png", RESOURCE, STORAGE_DB, "ICON128".getBytes(StandardCharsets.UTF_8));

        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", "extension"))
                .thenReturn(List.of(manifestResource, readmeResource, changelogResource, licenseResource, iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "foo", "bar", "1.3.4", "extension/"))
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
        var content = "{\"package\":\"json\"}".getBytes(StandardCharsets.UTF_8);
        var manifestResource = new FileResourceDTO(16, 1, "extension/package.json", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", "extension/package.json"))
                .thenReturn(List.of(manifestResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "foo", "bar", "1.3.4", "extension/package.json"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }

    @Test
    public void testBrowseImagesDir() throws Exception {
        var iconResource = new FileResourceDTO(20, 1, "extension/images/icon128.png", RESOURCE, STORAGE_DB, "ICON128".getBytes(StandardCharsets.UTF_8));
        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", "extension/images"))
                .thenReturn(List.of(iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "foo", "bar", "1.3.4", "extension/images/"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[\"http://localhost/vscode/unpkg/foo/bar/1.3.4/extension/images/icon128.png\"]"));
    }

    @Test
    public void testBrowseIcon() throws Exception {
        var content = "ICON128".getBytes(StandardCharsets.UTF_8);
        var iconResource = new FileResourceDTO(20, 1, "extension/images/icon128.png", RESOURCE, STORAGE_DB, content);
        Mockito.when(repositories.findAllResourceFileResourceDTOs("foo", "bar", "1.3.4", "extension/images/icon128.png"))
                .thenReturn(List.of(iconResource));

        mockMvc.perform(get("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/{path}", "foo", "bar", "1.3.4", "extension/images/icon128.png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));
    }


    @Test
    public void testGetItemBadRequest() throws Exception {
        mockMvc.perform(get("/vscode/item?itemName={itemName}", "vscode-yaml"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testDownload() throws Exception {
        mockMvc.perform(get("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage",
                "redhat", "vscode-yaml", "0.5.2"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/vscode/asset/redhat/vscode-yaml/0.5.2/Microsoft.VisualStudio.Services.VSIXPackage"));
    }

    @Test
    public void testDownloadMacOSX() throws Exception {
        mockMvc.perform(get("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage?targetPlatform={target}",
                "redhat", "vscode-yaml", "0.5.2", "darwin-arm64"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost/vscode/asset/redhat/vscode-yaml/0.5.2/Microsoft.VisualStudio.Services.VSIXPackage?targetPlatform=darwin-arm64"));
    }

    // ---------- UTILITY ----------//
    private ExtensionDTO mockSearch(boolean active) {
        return mockSearch(null, active);
    }

    private ExtensionDTO mockSearch(String targetPlatform, boolean active) {
        var entry1 = new ExtensionSearch();
        entry1.id = 1;
        var searchHit = new SearchHit<ExtensionSearch>("0", "1", 1.0f, null, null, entry1);
        var searchHits = new SearchHitsImpl<ExtensionSearch>(1, TotalHitsRelation.EQUAL_TO, 1.0f, "1",
                Arrays.asList(searchHit), new Aggregations(Collections.emptyList()));
        Mockito.when(search.isEnabled())
                .thenReturn(true);
        var searchOptions = new ISearchService.Options("yaml", null, targetPlatform, 50, 0, "desc", "relevance", false);
        Mockito.when(search.search(searchOptions, PageRequest.of(0, 50)))
                .thenReturn(searchHits);

        var extension = mockExtensionDTO();
        List<ExtensionDTO> results = active ? List.of(extension) : Collections.emptyList();
        Mockito.when(repositories.findAllActiveExtensionDTOsById(List.of(entry1.id)))
                .thenReturn(results);

        var publicIds = Set.of(extension.getPublicId());
        Mockito.when(repositories.findAllActiveExtensionDTOsByPublicId(publicIds))
                .thenReturn(results);

        var ids = List.of(extension.getId());
        Mockito.when(repositories.findAllActiveReviewCountsByExtensionId(ids))
                .thenReturn(Map.of(extension.getId(), 10));

        var name = extension.getName();
        var namespaceName = extension.getNamespace().getName();
        Mockito.when(repositories.findActiveExtensionDTO(name, namespaceName))
                .thenReturn(extension);

        mockExtensionVersionDTOs(extension, targetPlatform, targetPlatform);
        return extension;
    }

    private ExtensionDTO mockExtensionDTO() {
            var id = 1;
            var publicId = "test-1";
            var name = "vscode-yaml";
            var averageRating = 3.0;
            var downloadCount = 100;
            var namespaceId = 2;
            var namespacePublicId = "test-2";
            var namespaceName = "redhat";

            return new ExtensionDTO(id,publicId,name,averageRating,downloadCount, LocalDateTime.parse("1999-12-01T09:00"),
                    LocalDateTime.parse("2000-01-01T10:00"), namespaceId,namespacePublicId, namespaceName);
    }

    private void mockExtensionVersionDTOs(ExtensionDTO extension, String queryTargetPlatform, String... targetPlatforms) {
        var id = 2;
        var versions = new ArrayList<ExtensionVersionDTO>(targetPlatforms.length);
        for(var targetPlatform : targetPlatforms) {
            versions.add(mockExtensionVersionDTO(extension, id, targetPlatform));
            id++;
        }

        Mockito.when(repositories.findAllActiveExtensionVersionDTOs(Set.of(extension.getId()), queryTargetPlatform))
                .thenReturn(versions);

        mockFileResourceDTOs(versions);
    }

    private ExtensionVersionDTO mockExtensionVersionDTO(ExtensionDTO extension, long id, String targetPlatform) {
        return new ExtensionVersionDTO(
                    extension.getId(), id, "0.5.2", targetPlatform, true, false, LocalDateTime.parse("2000-01-01T10:00"),
                    "YAML", "YAML Language Support", "vscode@^1.31.0", null, null,
                    null, "https://github.com/redhat-developer/vscode-yaml", null, null, null, null);
    }

    private void mockFileResourceDTOs(List<ExtensionVersionDTO> extensionVersions) {
        var ids = extensionVersions.stream().map(ExtensionVersionDTO::getId).collect(Collectors.toSet());
        var types = List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG, RESOURCE);

        var files = new ArrayList<FileResourceDTO>();
        for(var id : ids) {
            files.add(new FileResourceDTO(id * 100 + 5, id, "redhat.vscode-yaml-0.5.2.vsix", DOWNLOAD));
            files.add(new FileResourceDTO(id * 100 + 6, id, "package.json", MANIFEST));
            files.add(new FileResourceDTO(id * 100 + 7, id, "README.md", README));
            files.add(new FileResourceDTO(id * 100 + 8, id, "CHANGELOG.md", CHANGELOG));
            files.add(new FileResourceDTO(id * 100 + 9, id, "LICENSE.txt", LICENSE));
            files.add(new FileResourceDTO(id * 100 + 10, id, "icon128.png", ICON));
            files.add(new FileResourceDTO(id * 100 + 11, id, "extension/themes/dark.json", RESOURCE));
            files.add(new FileResourceDTO(id * 100 + 12, id, "extension/img/logo.png", RESOURCE));
        }

        Mockito.when(repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(ids, types))
                .thenReturn(files);
    }

    private ExtensionVersion mockExtension() throws JsonProcessingException {
        return mockExtension(TargetPlatform.NAME_UNIVERSAL);
    }

    private ExtensionVersion mockExtension(String targetPlatform) throws JsonProcessingException {
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
                .thenReturn(0l);
        Mockito.when(repositories.countActiveReviews(extension))
                .thenReturn(10l);
        var extensionFile = new FileResource();
        extensionFile.setExtension(extVersion);
        extensionFile.setName("redhat.vscode-yaml-0.5.2.vsix");
        extensionFile.setType(FileResource.DOWNLOAD);
        extensionFile.setStorageType(FileResource.STORAGE_DB);
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
        Mockito.when(repositories.findFileByType(extVersion, FileResource.MANIFEST))
                .thenReturn(manifestFile);
        var readmeFile = new FileResource();
        readmeFile.setExtension(extVersion);
        readmeFile.setName("README.md");
        readmeFile.setType(FileResource.README);
        readmeFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.README))
                .thenReturn(readmeFile);
        var changelogFile = new FileResource();
        changelogFile.setExtension(extVersion);
        changelogFile.setName("CHANGELOG.md");
        changelogFile.setType(FileResource.CHANGELOG);
        changelogFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.CHANGELOG))
                .thenReturn(changelogFile);
        var licenseFile = new FileResource();
        licenseFile.setExtension(extVersion);
        licenseFile.setName("LICENSE.txt");
        licenseFile.setType(FileResource.LICENSE);
        licenseFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.LICENSE))
                .thenReturn(licenseFile);
        var iconFile = new FileResource();
        iconFile.setExtension(extVersion);
        iconFile.setName("icon128.png");
        iconFile.setType(FileResource.ICON);
        iconFile.setStorageType(FileResource.STORAGE_DB);
        Mockito.when(repositories.findFileByType(extVersion, FileResource.ICON))
                .thenReturn(iconFile);
        var webResourceFile = new FileResource();
        webResourceFile.setExtension(extVersion);
        webResourceFile.setName("extension/img/logo.png");
        webResourceFile.setType(FileResource.RESOURCE);
        webResourceFile.setStorageType(STORAGE_DB);
        webResourceFile.setContent("logo.png".getBytes());
        Mockito.when(repositories.findFileByTypeAndName(extVersion, FileResource.RESOURCE, "extension/img/logo.png"))
                .thenReturn(webResourceFile);
        Mockito.when(repositories.findFilesByType(extVersion, Arrays.asList(FileResource.MANIFEST, FileResource.README, FileResource.LICENSE, FileResource.ICON, FileResource.DOWNLOAD, FileResource.CHANGELOG)))
                .thenReturn(Streamable.of(manifestFile, readmeFile, licenseFile, iconFile, extensionFile, changelogFile));
        return extVersion;
    }

    private String file(String name) throws UnsupportedEncodingException, IOException {
        try (
            var stream = getClass().getResourceAsStream(name);
        ) {
            return CharStreams.toString(new InputStreamReader(stream, "UTF-8"));
        }
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
        UserService userService() {
            return new UserService();
        }

        @Bean
        StorageUtilService storageUtilService() {
            return new StorageUtilService();
        }
    }

}