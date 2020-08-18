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

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
public class SearchServiceTest {

    @MockBean
    RepositoryService repositories;

    @MockBean
    ElasticsearchOperations searchOperations;

    @Autowired
    SearchService search;

    @Test
    public void testRelevanceAverageRating() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 3.0, 100, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 4.0, 100, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }

    @Test
    public void testRelevanceReviewCount() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 4.0, 2, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 4.0, 100, 0, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }

    @Test
    public void testRelevanceDownloadCount() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 0.0, 0, 1, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 0.0, 0, 10, LocalDateTime.parse("2020-01-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }

    @Test
    public void testRelevanceTimestamp() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 0.0, 0, 0, LocalDateTime.parse("2020-02-01T00:00"), false, false);
        var ext2 = mockExtension("bar", 0.0, 0, 0, LocalDateTime.parse("2020-10-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }

    @Test
    public void testRelevancePublic() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), true, true);
        var ext2 = mockExtension("bar", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }

    @Test
    public void testRelevanceUnrelated1() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, true);
        var ext2 = mockExtension("bar", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, false);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }

    @Test
    public void testRelevanceUnrelated2() throws Exception {
        var index = mockIndex();
        var ext1 = mockExtension("foo", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), false, true);
        var ext2 = mockExtension("bar", 4.0, 10, 10, LocalDateTime.parse("2020-10-01T00:00"), true, true);
        search.updateSearchEntry(ext1);
        search.updateSearchEntry(ext2);
        
        assertThat(index).hasSize(2);
        assertThat(index.get(0).relevance).isLessThan(index.get(1).relevance);
    }


    //---------- UTILITY ----------//

    private void mockStats() {
        Mockito.when(repositories.getMaxExtensionDownloadCount())
                .thenReturn(10);
        Mockito.when(repositories.getOldestExtensionTimestamp())
                .thenReturn(LocalDateTime.parse("2020-01-01T00:00"));
    }

    private List<ExtensionSearch> mockIndex() {
        mockStats();
        var entryList = new ArrayList<ExtensionSearch>();
        Mockito.when(searchOperations.index(any(IndexQuery.class)))
                .then(invocation -> {
                    var query = invocation.getArgument(0, IndexQuery.class);
                    entryList.add((ExtensionSearch) query.getObject());
                    return "test";
                });
        return entryList;
    }

    private Extension mockExtension(String name, double averageRating, int ratingCount, int downloadCount,
            LocalDateTime timestamp, boolean isPublic, boolean isUnrelated) {
        var extension = new Extension();
        extension.setName(name);
        extension.setAverageRating(averageRating);
        extension.setDownloadCount(downloadCount);
        Mockito.when(repositories.countActiveReviews(extension))
                .thenReturn((long) ratingCount);
        var namespace = new Namespace();
        namespace.setName("test");
        extension.setNamespace(namespace);
        Mockito.when(repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER))
                .thenReturn(isPublic ? 0l : 1l);
        var extVer = new ExtensionVersion();
        extVer.setExtension(extension);
        extVer.setTimestamp(timestamp);
        extension.setLatest(extVer);
        var user = new UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);
        extVer.setPublishedWith(token);
        Mockito.when(repositories.countMemberships(user, namespace))
                .thenReturn(isUnrelated ? 0l : 1l);
        return extension;
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        SearchService searchService() {
            return new SearchService();
        }
    }
    
}