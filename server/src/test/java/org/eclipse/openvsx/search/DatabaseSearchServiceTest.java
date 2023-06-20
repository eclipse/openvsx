/********************************************************************************
 * Copyright (c) 2021 Red Hat, Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.openvsx.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.eclipse.openvsx.cache.LatestExtensionVersionCacheKeyGenerator;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.EntityManager;

@ExtendWith(SpringExtension.class)
public class DatabaseSearchServiceTest {

    @MockBean
    EntityManager entityManager;

    @MockBean
    RepositoryService repositories;

    @Autowired
    VersionService versions;

    @Autowired
    DatabaseSearchService search;

    @Test
    public void testCategory() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("openshift", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3)));

        var searchOptions = new ISearchService.Options(null, "Programming Languages", TargetPlatform.NAME_UNIVERSAL, 50, 0, null, null, false);
        var result = search.search(searchOptions);
        // should find two extensions
        assertThat(result.getTotalHits()).isEqualTo(2);
    }

    @Test
    public void testRelevance() throws Exception {
        var ext1 = mockExtension("yaml", 1.0, 100, 100, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 10000, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("openshift", 1.0, 100, 10, "redhat", Arrays.asList("Snippets", "Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3)));

        var searchOptions = new ISearchService.Options(null, null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, "relevance", false);
        var result = search.search(searchOptions);
        // should find all extensions but order should be different
        assertThat(result.getTotalHits()).isEqualTo(3);

        var hits = result.getSearchHits();
        // java should have the most relevance
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("openshift"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("yaml"));
        assertThat(getIdFromExtensionHits(hits, 2)).isEqualTo(getIdFromExtensionName("java"));
    }

    @Test
    public void testReverse() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2)));

        var searchOptions = new ISearchService.Options(null, "Programming Languages", TargetPlatform.NAME_UNIVERSAL, 50, 0, "desc", null, false);
        var result = search.search(searchOptions);
        // should find two extensions
        assertThat(result.getTotalHits()).isEqualTo(2);

        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("java"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("yaml"));
    }

    @Test
    public void testSimplePageSize() throws Exception {
        var ext1 = mockExtension("ext1", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("ext2", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("ext3", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext4 = mockExtension("ext4", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext5 = mockExtension("ext5", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext6 = mockExtension("ext6", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext7 = mockExtension("ext7", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4, ext5, ext6, ext7)));

        var pageSizeItems = 5;
        var searchOptions = new ISearchService.Options(null, null, TargetPlatform.NAME_UNIVERSAL, pageSizeItems, 0, null, null, false);

        var result = search.search(searchOptions);
        // 7 total hits
        assertThat(result.getTotalHits()).isEqualTo(7);
        // but as we limit the page size it should only contains 5
        var hits = result.getSearchHits();
        assertThat(hits.size()).isEqualTo(pageSizeItems);

        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("ext1"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("ext2"));
        assertThat(getIdFromExtensionHits(hits, 2)).isEqualTo(getIdFromExtensionName("ext3"));
        assertThat(getIdFromExtensionHits(hits, 3)).isEqualTo(getIdFromExtensionName("ext4"));
        assertThat(getIdFromExtensionHits(hits, 4)).isEqualTo(getIdFromExtensionName("ext5"));
    }

    @Test
    public void testPages() throws Exception {
        var ext1 = mockExtension("ext1", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("ext2", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("ext3", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext4 = mockExtension("ext4", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext5 = mockExtension("ext5", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext6 = mockExtension("ext6", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext7 = mockExtension("ext7", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4, ext5, ext6, ext7)));

        var pageSizeItems = 2;
        var searchOptions = new ISearchService.Options(null, null, TargetPlatform.NAME_UNIVERSAL, pageSizeItems, 4, null, null, false);
        var result = search.search(searchOptions);

        // 7 total hits
        assertThat(result.getTotalHits()).isEqualTo(7);
        // But it should only contains 2 search items as specified by the pageSize
        var hits = result.getSearchHits();
        assertThat(hits.size()).isEqualTo(pageSizeItems);

        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("ext5"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("ext6"));
    }

    @Test
    public void testQueryStringPublisherName() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("openshift", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Other"));
        var ext4 = mockExtension("foo", 4.0, 100, 0, "bar", Arrays.asList("Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options("redhat", null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, null, false);
        var result = search.search(searchOptions);
        // namespace finding
        assertThat(result.getTotalHits()).isEqualTo(3);

        // Check it found the correct extension
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("yaml"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("java"));
        assertThat(getIdFromExtensionHits(hits, 2)).isEqualTo(getIdFromExtensionName("openshift"));
    }

    @Test
    public void testQueryStringExtensionName() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("openshift", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Other"));
        var ext4 = mockExtension("foo", 4.0, 100, 0, "bar", Arrays.asList("Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options("openshift", null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, null, false);
        var result = search.search(searchOptions);
        // extension name finding
        assertThat(result.getTotalHits()).isEqualTo(1);

        // Check it found the correct extension
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("openshift"));
    }

    @Test
    public void testQueryStringDescription() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        versions.getLatest(ext2, null, false, false).setDescription("another desc");
        var ext3 = mockExtension("openshift", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Other"));
        versions.getLatest(ext3, null, false, false).setDescription("my custom desc");
        var ext4 = mockExtension("foo", 4.0, 100, 0, "bar", Arrays.asList("Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options("my custom desc", null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, null, false);
        var result = search.search(searchOptions);
        // custom description
        assertThat(result.getTotalHits()).isEqualTo(1);

        // Check it found the correct extension
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("openshift"));
    }

    @Test
    public void testQueryStringDisplayName() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        versions.getLatest(ext1, null, false, false).setDisplayName("This is a YAML extension");
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        versions.getLatest(ext2, null, false, false).setDisplayName("Red Hat");
        var ext3 = mockExtension("openshift", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Other"));
        var ext4 = mockExtension("foo", 4.0, 100, 0, "bar", Arrays.asList("Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options("Red Hat", null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, null, false);
        var result = search.search(searchOptions);

        // custom displayname
        assertThat(result.getTotalHits()).isEqualTo(1);

        // Check it found the correct extension
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("java"));
    }

    @Test
    public void testSortByTimeStamp() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        versions.getLatest(ext1, null, false, false).setTimestamp(LocalDateTime.parse("2021-10-10T00:00"));
        var ext2 = mockExtension("java", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        versions.getLatest(ext2, null, false, false).setTimestamp(LocalDateTime.parse("2021-10-07T00:00"));
        var ext3 = mockExtension("openshift", 4.0, 100, 0, "redhat", Arrays.asList("Snippets", "Other"));
        versions.getLatest(ext3, null, false, false).setTimestamp(LocalDateTime.parse("2021-10-11T00:00"));
        var ext4 = mockExtension("foo", 4.0, 100, 0, "bar", Arrays.asList("Other"));
        versions.getLatest(ext4, null, false, false).setTimestamp(LocalDateTime.parse("2021-10-06T00:00"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options(null, null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, "timestamp", false);
        var result = search.search(searchOptions);
        // all extensions should be there
        assertThat(result.getTotalHits()).isEqualTo(4);

        // test now the order
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("foo"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("java"));
        assertThat(getIdFromExtensionHits(hits, 2)).isEqualTo(getIdFromExtensionName("yaml"));
        assertThat(getIdFromExtensionHits(hits, 3)).isEqualTo(getIdFromExtensionName("openshift"));
    }

    @Test
    public void testSortByDownloadCount() throws Exception {
        var ext1 = mockExtension("yaml", 3.0, 100, 100, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 4.0, 100, 1000, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("openshift", 4.0, 100, 300, "redhat", Arrays.asList("Snippets", "Other"));
        var ext4 = mockExtension("foo", 4.0, 100, 500, "bar", Arrays.asList("Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options(null, null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, "downloadCount", false);
        var result = search.search(searchOptions);
        // all extensions should be there
        assertThat(result.getTotalHits()).isEqualTo(4);

        // test now the order
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("yaml"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("openshift"));
        assertThat(getIdFromExtensionHits(hits, 2)).isEqualTo(getIdFromExtensionName("foo"));
        assertThat(getIdFromExtensionHits(hits, 3)).isEqualTo(getIdFromExtensionName("java"));
    }

    @Test
    public void testSortByRating() throws Exception {
        var ext1 = mockExtension("yaml", 4.0, 1, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext2 = mockExtension("java", 5.0, 1, 0, "redhat", Arrays.asList("Snippets", "Programming Languages"));
        var ext3 = mockExtension("openshift", 2.0, 1, 0, "redhat", Arrays.asList("Snippets", "Other"));
        var ext4 = mockExtension("foo", 1.0, 1, 0, "bar", Arrays.asList("Other"));
        Mockito.when(repositories.findAllActiveExtensions()).thenReturn(Streamable.of(List.of(ext1, ext2, ext3, ext4)));

        var searchOptions = new ISearchService.Options(null, null, TargetPlatform.NAME_UNIVERSAL, 50, 0, null, "rating", false);
        var result = search.search(searchOptions);
        // all extensions should be there
        assertThat(result.getTotalHits()).isEqualTo(4);

        // test now the order
        var hits = result.getSearchHits();
        assertThat(getIdFromExtensionHits(hits, 0)).isEqualTo(getIdFromExtensionName("foo"));
        assertThat(getIdFromExtensionHits(hits, 1)).isEqualTo(getIdFromExtensionName("openshift"));
        assertThat(getIdFromExtensionHits(hits, 2)).isEqualTo(getIdFromExtensionName("yaml"));
        assertThat(getIdFromExtensionHits(hits, 3)).isEqualTo(getIdFromExtensionName("java"));
    }

    // ---------- UTILITY ----------//

    long getIdFromExtensionHits(List<SearchHit<ExtensionSearch>> hits, int index) {
        return hits.get(index).getContent().id;
    }

    long getIdFromExtensionName(String extensionName) {
        return extensionName.hashCode();
    }

    private Extension mockExtension(String name, double averageRating, long ratingCount, int downloadCount,
            String namespaceName, List<String> categories) {
        var extension = new Extension();
        extension.setName(name);
        extension.setId(name.hashCode());
        extension.setAverageRating(averageRating);
        extension.setReviewCount(ratingCount);
        extension.setDownloadCount(downloadCount);
        extension.setActive(true);
        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        extension.setNamespace(namespace);
        var isUnverified = false;
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(isUnverified ? 0l : 1l);
        var extVer = new ExtensionVersion();
        extVer.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVer.setCategories(categories);
        extVer.setTimestamp(LocalDateTime.parse("2021-10-01T00:00"));
        extVer.setActive(true);
        extVer.setExtension(extension);
        extension.getVersions().add(extVer);
        var user = new UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);
        extVer.setPublishedWith(token);
        Mockito.when(repositories.isVerified(namespace, user)).thenReturn(false);
        return extension;
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        DatabaseSearchService searchService() {
            return new DatabaseSearchService();
        }

        @Bean
        RelevanceService relevanceService() {
            return new RelevanceService();
        }

        @Bean
        VersionService versionService() {
            return new VersionService();
        }

        @Bean
        LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKeyGenerator() {
            return new LatestExtensionVersionCacheKeyGenerator();
        }
    }
}