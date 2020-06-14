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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
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

    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected final Logger logger = LoggerFactory.getLogger(SearchService.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    ElasticsearchOperations searchOperations;

    @Value("${ovsx.elasticsearch.enabled:true}")
    boolean enableSearch;

    @Value("${ovsx.elasticsearch.relevance.rating:1.0}")
    double ratingRelevance;
    @Value("${ovsx.elasticsearch.relevance.downloads:1.0}")
    double downloadsRelevance;
    @Value("${ovsx.elasticsearch.relevance.timestamp:1.0}")
    double timestampRelevance;
    @Value("${ovsx.elasticsearch.relevance.builtin:0.5}")
    double builtinRelevance;

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
        try {
            rwLock.writeLock().lock();
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
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void updateSearchEntry(Extension extension) {
        try {
            rwLock.writeLock().lock();
            var stats = new SearchStats();
            var indexQuery = new IndexQueryBuilder()
                    .withObject(toSearchEntry(extension, stats))
                    .build();
            searchOperations.index(indexQuery);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeSearchEntry(Extension extension) {
        try {
            rwLock.writeLock().lock();
            searchOperations.delete(ExtensionSearch.class, Long.toString(extension.getId()));
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    protected ExtensionSearch toSearchEntry(Extension extension, SearchStats stats) {
        var entry = extension.toSearch();
        var ratingValue = (entry.averageRating != null ? entry.averageRating : 0.0) / 5.0;
        var downloadsValue = entry.downloadCount / stats.downloadRef;
        var timestamp = extension.getLatest().getTimestamp();
        var timestampValue = Duration.between(stats.oldest, timestamp).toSeconds() / stats.timestampRef;
        entry.relevance = ratingRelevance * limit(ratingValue)
                + downloadsRelevance * limit(downloadsValue)
                + timestampRelevance * limit(timestampValue);

        // Reduce the relevance value of built-in extensions to show other results first
        if ("vscode".equals(entry.namespace)) {
            entry.relevance *= builtinRelevance;
        }
    
        if (Double.isNaN(entry.relevance) || Double.isInfinite(entry.relevance)) {
            var message = "Invalid relevance for entry " + entry.namespace + "." + entry.name;
            try {
                message += " " + new ObjectMapper().writeValueAsString(stats);
            } catch (JsonProcessingException exc) {
                // Ignore exception
            }
            logger.error(message);
            entry.relevance = 0.0;
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

    public Page<ExtensionSearch> search(String queryString, String category, Pageable pageRequest, String sortOrder, String sortBy) {
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
            var prefixString = queryString.trim().toLowerCase();
            var namePrefixQuery = QueryBuilders.prefixQuery("displayName", prefixString);
            boolQuery.should(namePrefixQuery).boost(2);
            var namespacePrefixQuery = QueryBuilders.prefixQuery("namespace", prefixString);
            boolQuery.should(namespacePrefixQuery);

            queryBuilder.withQuery(boolQuery);
        }

        if (!Strings.isNullOrEmpty(category)) {
            // Filter by selected category
            queryBuilder.withFilter(QueryBuilders.matchPhraseQuery("categories", category));
        }

        if (sortOrder == "asc" || sortOrder == 1) {
            sortOrder = "1";
        } else if (sortOrder == "desc" || sortOrder == 0) {
            sortOrder = "0";
        }

        if (!(sortOrder == "0") && !(sortOrder == "1")) {
            logger.error("value of sortOrder is {}", sortOrder);
            throw new ErrorResultException("sortOrder parameter must be either 'asc' or 'desc', or '0' or '1'.");
        }

        if ("relevance".equals(sortBy)) {
            queryBuilder.withSort(SortBuilders.scoreSort());
        }

        if ("relevance".equals(sortBy) || "averageRating".equals(sortBy)) {
            queryBuilder.withSort(
                    SortBuilders.fieldSort(sortBy).unmappedType("float").order(SortOrder.fromString(sortOrder)));
        } else if ("timestamp".equals(sortBy)) {
            queryBuilder.withSort(
                    SortBuilders.fieldSort(sortBy).unmappedType("long").order(SortOrder.fromString(sortOrder)));
        } else if ("downloadCount".equals(sortBy)) {
            queryBuilder.withSort(
                    SortBuilders.fieldSort(sortBy).unmappedType("integer").order(SortOrder.fromString(sortOrder)));
        } else {
            throw new ErrorResultException(
                    "sortBy parameter must be 'relevance', 'timestamp', 'averageRating' or 'downloadCount'");
        }
        
        try {
            rwLock.readLock().lock();
            return searchOperations.queryForPage(queryBuilder.build(), ExtensionSearch.class);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    protected class SearchStats {
        protected final double downloadRef;
        protected final double timestampRef;
        protected final LocalDateTime oldest;

        public SearchStats() {
            var now = TimeUtil.getCurrentUTC();
            var maxDownloads = repositories.getMaxExtensionDownloadCount();
            var oldestTimestamp = repositories.getOldestExtensionTimestamp();
            this.downloadRef = maxDownloads * 1.5 + 100;
            this.oldest = oldestTimestamp == null ? now : oldestTimestamp;
            this.timestampRef = Duration.between(this.oldest, now).toSeconds() + 60;
        }
    }

}