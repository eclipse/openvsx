/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.util.Streamable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class ElasticSearchServiceTest {

    @MockBean
    RepositoryService repositories;

    @MockBean
    ElasticsearchOperations searchOperations;

    @Autowired
    ElasticSearchService search;

    @AfterEach
    public void afterEach() {
        VersionUtil.clearCache();
    }

    @Test
    public void testRelevanceAverageRating() throws Exception {
        var index = mockIndex(true);
        var ext1 = mockExtension("foo", 3.0, 100, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension( "bar", 4.0, 100, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);

        assertThat(index.entries).hasSize(2);
        assertThat(index.entries.get(0).relevance).isLessThan(index.entries.get(1).relevance);
    }

    @Test
    public void testRelevanceReviewCount() throws Exception {
        var index = mockIndex(true);
        var ext1 = mockExtension("foo", 4.0, 2, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 4.0, 100, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);

        assertThat(index.entries).hasSize(2);
        assertThat(index.entries.get(0).relevance).isLessThan(index.entries.get(1).relevance);
    }

    @Test
    public void testRelevanceDownloadCount() throws Exception {
        var index = mockIndex(true);
        var ext1 = mockExtension("foo", 0.0, 0, 1, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 0.0, 0, 10, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);

        assertThat(index.entries).hasSize(2);
        assertThat(index.entries.get(0).relevance).isLessThan(index.entries.get(1).relevance);
    }

    @Test
    public void testRelevanceTimestamp() throws Exception {
        var index = mockIndex(true);
        var ext1 = mockExtension("foo", 0.0, 0, 0, LocalDateTime.parse("2020-02-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 0.0, 0, 0, LocalDateTime.parse("2020-10-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);

        assertThat(index.entries).hasSize(2);
        assertThat(index.entries.get(0).relevance).isLessThan(index.entries.get(1).relevance);
    }

    @Test
    public void testRelevanceUnverified1() throws Exception {
        var index = mockIndex(true);
        var ext1 = mockExtension("foo", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, true);
        var ext2 = mockExtension("bar", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);

        assertThat(index.entries).hasSize(2);
        assertThat(index.entries.get(0).relevance).isLessThan(index.entries.get(1).relevance);
    }

    @Test
    public void testRelevanceUnverified2() throws Exception {
        var index = mockIndex(true);
        var ext1 = mockExtension("foo", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), true, false);
        var ext2 = mockExtension("bar", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);

        assertThat(index.entries).hasSize(2);
        assertThat(index.entries.get(0).relevance).isLessThan(index.entries.get(1).relevance);
    }

    @Test
    public void testSoftUpdateExists() throws Exception {
        var index = mockIndex(true);
        mockExtensions();
        search.updateSearchIndex(false);

        assertThat(index.created).isFalse();
        assertThat(index.deleted).isFalse();
        assertThat(index.entries).hasSize(3);
    }

    @Test
    public void testSoftUpdateNotExists() throws Exception {
        var index = mockIndex(false);
        mockExtensions();
        search.updateSearchIndex(false);

        assertThat(index.created).isTrue();
        assertThat(index.deleted).isFalse();
        assertThat(index.entries).hasSize(3);
    }

    @Test
    public void testHardUpdateExists() throws Exception {
        var index = mockIndex(true);
        mockExtensions();
        search.updateSearchIndex(true);

        assertThat(index.created).isTrue();
        assertThat(index.deleted).isTrue();
        assertThat(index.entries).hasSize(3);
    }

    @Test
    public void testHardUpdateNotExists() throws Exception {
        var index = mockIndex(false);
        mockExtensions();
        search.updateSearchIndex(true);

        assertThat(index.created).isTrue();
        assertThat(index.deleted).isFalse();
        assertThat(index.entries).hasSize(3);
    }


    //---------- UTILITY ----------//

    private void mockStats() {
        Mockito.when(repositories.getMaxExtensionDownloadCount())
                .thenReturn(10);
        Mockito.when(repositories.getOldestExtensionTimestamp())
                .thenReturn(LocalDateTime.parse("2020-01-01T00:00"));
    }

    @SuppressWarnings("unchecked")
    private MockIndex mockIndex(boolean exists) {
        mockStats();

        var index = new MockIndex();
        Mockito.when(searchOperations.index(any(IndexQuery.class), any(IndexCoordinates.class)))
            .then(invocation -> {
                var query = invocation.getArgument(0, IndexQuery.class);
                index.entries.add((ExtensionSearch) query.getObject());
                return "test";
            });
        Mockito.doAnswer(invocation -> {
                var queries = (List<IndexQuery>) invocation.getArgument(0);
                queries.forEach(query -> index.entries.add((ExtensionSearch) query.getObject()));
                return null;
            }).when(searchOperations).bulkIndex(any(List.class), any(IndexCoordinates.class));

        var indexOps = Mockito.mock(IndexOperations.class);
        Mockito.when(searchOperations.indexOps(ExtensionSearch.class))
            .thenReturn(indexOps);
        Mockito.when(indexOps.getIndexCoordinates())
            .thenReturn(IndexCoordinates.of("extensions"));

        Mockito.when(indexOps.exists())
            .thenReturn(exists);
        Mockito.when(indexOps.delete())
            .then(invocation -> {
                if (!exists && !index.created)
                    throw new IllegalStateException("Index does not exist.");
                return index.deleted = true;
            });
        Mockito.when(indexOps.create())
            .then(invocation -> {
                if (exists && !index.deleted)
                    throw new IllegalStateException("Index already exists.");
                return index.created = true;
            });
        return index;
    }

    private Extension mockExtension(String name, double averageRating, int ratingCount, int downloadCount,
            LocalDateTime timestamp, boolean isUnverified, boolean isUnrelated) {
        var extension = new Extension();
        extension.setName(name);
        extension.setId(name.hashCode());
        extension.setAverageRating(averageRating);
        extension.setDownloadCount(downloadCount);
        Mockito.when(repositories.countActiveReviews(extension))
                .thenReturn((long) ratingCount);
        var namespace = new Namespace();
        namespace.setName("test");
        extension.setNamespace(namespace);
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(isUnverified ? 0l : 1l);
        var extVer = new ExtensionVersion();
        extVer.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVer.setTimestamp(timestamp);
        extVer.setActive(true);
        extVer.setExtension(extension);
        extension.getVersions().add(extVer);
        var user = new UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);
        extVer.setPublishedWith(token);
        Mockito.when(repositories.countMemberships(user, namespace))
                .thenReturn(isUnrelated ? 0l : 1l);
        return extension;
    }

    private void mockExtensions() {
        var ext1 = mockExtension("foo", 3.0, 1, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 3.0, 1, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext3 = mockExtension("baz", 3.0, 1, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        Mockito.when(repositories.findAllActiveExtensions())
                .thenReturn(Streamable.of(ext1, ext2, ext3));
    }

    static class MockIndex {
        final List<ExtensionSearch> entries = new ArrayList<>();
        boolean created;
        boolean deleted;
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        ElasticSearchService searchService() {
            return new ElasticSearchService();
        }

        @Bean
        RelevanceService relevanceService() {
            return new RelevanceService();
        }
    }
    
}