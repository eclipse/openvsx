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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import javax.transaction.Transactional;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
public class SearchService {

    private static final double BUILTIN_PENALTY = 0.5;

    Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    ElasticsearchOperations searchOperations;

    @Value("${ovsx.elasticsearch.enabled:true}")
    boolean enableSearch;

    public boolean isEnabled() {
        return enableSearch;
    }

    @EventListener
    @Transactional
    public void initSearchIndex(ApplicationStartedEvent event) {
        if (!isEnabled()) {
            return;
        }
        var stopWatch = new StopWatch();
        stopWatch.start();
        updateSearchIndex();
        stopWatch.stop();
        logger.info("Initialized search index in " + stopWatch.getTotalTimeMillis() + " ms");
    }

    public void updateSearchIndex() {
        if (searchOperations.indexExists(ExtensionSearch.class)) {
            searchOperations.deleteIndex(ExtensionSearch.class);
        }
        searchOperations.createIndex(ExtensionSearch.class);
        var allExtensions = repositories.findAllExtensions();
        if (allExtensions.isEmpty()) {
            return;
        }
        var stats = new SearchStats();
        var indexQueries = allExtensions.map(extension ->
            new IndexQueryBuilder()
                .withObject(toSearchEntry(extension, stats))
                .build()
        ).toList();
        searchOperations.bulkIndex(indexQueries);
    }

    public void updateSearchEntry(Extension extension) {
        var stats = new SearchStats();
        var indexQuery = new IndexQueryBuilder()
                .withObject(toSearchEntry(extension, stats))
                .build();
        searchOperations.index(indexQuery);
    }

    protected ExtensionSearch toSearchEntry(Extension extension, SearchStats stats) {
        var entry = extension.toSearch();
        var ratingValue = (entry.averageRating != null ? entry.averageRating : 0.0) / 5.0;
        var downloadsValue = entry.downloadCount / stats.downloadRef;
        var timestamp = extension.getLatest().getTimestamp();
        var timestampValue = Duration.between(stats.oldest, timestamp).toSeconds() / stats.timestampRef;
        entry.relevance = 2 * limit(ratingValue) + 2 * limit(downloadsValue) + 1 * limit(timestampValue);
        // Reduce the relevance value of built-in extensions to show other results first
        if ("vscode".equals(entry.namespace)) {
            entry.relevance *= BUILTIN_PENALTY;
        }
        return entry;
    }

    private double limit(double value) {
        if (value < 0.0)
            return 0.0;
        else if (value > 1.0)
            return 1.0;
        else
            return value;
    }

    public Page<ExtensionSearch> search(String queryString, String category, Pageable pageRequest) {
        var queryBuilder = new NativeSearchQueryBuilder()
                .withIndices("extensions")
                .withPageable(pageRequest);
        if (!Strings.isNullOrEmpty(queryString)) {
            var boolQuery = QueryBuilders.boolQuery();

            // Fuzzy matching of search query in multiple fields
            var multiMatchQuery = QueryBuilders.multiMatchQuery(queryString)
                    .field("name").boost(5)
                    .field("displayName").boost(5)
                    .field("tags").boost(3)
                    .field("namespace").boost(2)
                    .field("description")
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2);
            boolQuery.should(multiMatchQuery).boost(5);

            // Prefix matching of search query in display name and namespace
            var namePrefixQuery = QueryBuilders.prefixQuery("displayName", queryString.trim());
            boolQuery.should(namePrefixQuery).boost(2);
            var namespacePrefixQuery = QueryBuilders.prefixQuery("namespace", queryString.trim());
            boolQuery.should(namespacePrefixQuery);

            queryBuilder.withQuery(boolQuery);
        }

        if (!Strings.isNullOrEmpty(category)) {
            // Filter by selected category
            queryBuilder.withFilter(QueryBuilders.matchPhraseQuery("categories", category));
        }

        // Configure default sorting of results
        queryBuilder.withSort(SortBuilders.scoreSort());
        queryBuilder.withSort(SortBuilders.fieldSort("relevance").order(SortOrder.DESC));

        return searchOperations.queryForPage(queryBuilder.build(), ExtensionSearch.class);
    }

    protected class SearchStats {
        final double downloadRef;
        final double timestampRef;
        final LocalDateTime oldest;

        public SearchStats() {
            this.downloadRef = (repositories.getMaxExtensionDownloadCount() + 100) * 1.5;
            this.oldest = repositories.getOldestExtensionTimestamp();
            var now = LocalDateTime.now(ZoneId.of("UTC"));
            this.timestampRef = Duration.between(this.oldest, now).toSeconds();
        }
    }

}