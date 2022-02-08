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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.*;

import javax.persistence.EntityManager;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import net.javacrumbs.shedlock.core.LockProvider;
import org.eclipse.openvsx.MockTransactionTemplate;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.dto.ExtensionDTO;
import org.eclipse.openvsx.dto.ExtensionReviewCountDTO;
import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.dto.FileResourceDTO;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
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
    AzureDownloadCountService.class, LockProvider.class
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
        mockSearch(true);
        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("search-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("search-yaml-response.json")));
    }

    @Test
    public void testFindById() throws Exception {
        mockSearch(true);
        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findid-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findid-yaml-response.json")));
    }

    @Test
    public void testFindByIdDuplicate() throws Exception {
        mockSearch(true);
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
        mockSearch(true);
        mockMvc.perform(post("/vscode/gallery/extensionquery")
                .content(file("findname-yaml-query.json"))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(file("findname-yaml-response.json")));
    }

    @Test
    public void testFindByNameDuplicate() throws Exception {
        mockSearch(true);
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
    public void testAssetNotFound() throws Exception {
        var extVersion = mockExtension();
        Mockito.when(repositories.findFileByType(extVersion, FileResource.MANIFEST))
                .thenReturn(null);
        mockMvc.perform(get("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}",
                    "redhat", "vscode-yaml", "0.5.2", "Microsoft.VisualStudio.Code.Manifest"))
                .andExpect(status().isNotFound());
    }


    // ---------- UTILITY ----------//

    private void mockSearch(boolean active) {
        var entry1 = new ExtensionSearch();
        entry1.id = 1;
        var searchHit = new SearchHit<ExtensionSearch>("0", "1", 1.0f, null, null, entry1);
        var searchHits = new SearchHitsImpl<ExtensionSearch>(1, TotalHitsRelation.EQUAL_TO, 1.0f, "1",
                Arrays.asList(searchHit), new Aggregations(Collections.emptyList()));
        Mockito.when(search.isEnabled())
                .thenReturn(true);
        var searchOptions = new ISearchService.Options("yaml", null, 50, 0, "desc", "relevance", false);
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
                .thenReturn(List.of(new ExtensionReviewCountDTO(extension.getId(), 10l)));

        var name = extension.getName();
        var namespaceName = extension.getNamespace().getName();
        Mockito.when(repositories.findActiveExtensionDTOByNameAndNamespaceName(name, namespaceName))
                .thenReturn(extension);

        mockFileResourceDTOs(extension.getLatest());
    }

    private ExtensionDTO mockExtensionDTO() {
            var id = 1;
            var publicId = "test-1";
            var name = "vscode-yaml";
            var preview = true;
            var averageRating = 3.0;
            var downloadCount = 100;
            var namespaceId = 2;
            var namespacePublicId = "test-2";
            var namespaceName = "redhat";
            var latestId = 3;
            var latestVersion = "0.5.2";
            var latestPreRelease = false;
            var latestTimestamp = LocalDateTime.parse("2000-01-01T10:00");
            var latestDisplayName = "YAML";
            var latestDescription = "YAML Language Support";
            var latestEngines = "vscode@^1.31.0";
            var latestRepository = "https://github.com/redhat-developer/vscode-yaml";

            return new ExtensionDTO(id,publicId,name,preview,averageRating,downloadCount,namespaceId,namespacePublicId,
                    namespaceName,latestId,latestVersion,latestPreRelease,latestTimestamp,latestDisplayName,latestDescription,
                    latestEngines,null,null,null,latestRepository,null,
                    null,null,null);
    }

    private void mockFileResourceDTOs(ExtensionVersionDTO extensionVersion) {
        var ids = new HashSet<>(List.of(extensionVersion.getId()));
        var types = List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG, WEB_RESOURCE);

        var extensionFile = new FileResourceDTO(5, extensionVersion.getId(), "redhat.vscode-yaml-0.5.2.vsix", DOWNLOAD);
        var manifestFile = new FileResourceDTO(6, extensionVersion.getId(), "package.json", MANIFEST);
        var readmeFile = new FileResourceDTO(7, extensionVersion.getId(), "README.md", README);
        var changelogFile = new FileResourceDTO(8, extensionVersion.getId(), "CHANGELOG.md", CHANGELOG);
        var licenseFile = new FileResourceDTO(9, extensionVersion.getId(), "LICENSE.txt", LICENSE);
        var iconFile = new FileResourceDTO(10, extensionVersion.getId(), "icon128.png", ICON);

        Mockito.when(repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(ids, types))
                .thenReturn(List.of(manifestFile, readmeFile, licenseFile, iconFile, extensionFile, changelogFile));
    }
    
    private ExtensionVersion mockExtension() {
        var extension = new Extension();
        extension.setId(1);
        extension.setPublicId("test-1");
        extension.setName("vscode-yaml");
        extension.setActive(true);
        extension.setDownloadCount(100);
        extension.setAverageRating(3.0);
        var namespace = new Namespace();
        namespace.setId(2);
        namespace.setPublicId("test-2");
        namespace.setName("redhat");
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extension.setLatest(extVersion);
        extVersion.setExtension(extension);
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
        Mockito.when(repositories.findVersion("0.5.2", "vscode-yaml", "redhat"))
                .thenReturn(extVersion);
        Mockito.when(repositories.findVersions(extension))
                .thenReturn(Streamable.of(extVersion));
        Mockito.when(repositories.getVersionStrings(extension))
                .thenReturn(Streamable.of(extVersion.getVersion()));
        Mockito.when(repositories.getActiveVersionStrings(extension))
                .thenReturn(Streamable.of(extVersion.getVersion()));
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
        manifestFile.setContent("{\"foo\":\"bar\"}".getBytes());
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