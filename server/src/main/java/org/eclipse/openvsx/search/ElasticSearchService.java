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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.RelevanceService.SearchStats;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TargetPlatform;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

@Component
public class ElasticSearchService implements ISearchService {

    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    protected final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    ElasticsearchOperations searchOperations;

    @Autowired
    RelevanceService relevanceService;

    @Value("${ovsx.elasticsearch.enabled:true}")
    boolean enableSearch;
    @Value("${ovsx.elasticsearch.clear-on-start:false}")
    boolean clearOnStart;

    @Value("${ovsx.elasticsearch.relevance.rating:1.0}")
    double ratingRelevance;
    @Value("${ovsx.elasticsearch.relevance.downloads:1.0}")
    double downloadsRelevance;
    @Value("${ovsx.elasticsearch.relevance.timestamp:1.0}")
    double timestampRelevance;
    @Value("${ovsx.elasticsearch.relevance.unverified:0.5}")
    double unverifiedRelevance;
    
    public boolean isEnabled() {
        return enableSearch;
    }

    /**
     * Application start listener that initializes the search index. If the application property
     * {@code ovsx.elasticsearch.clear-on-start} is set to {@code true}, the index is cleared
     * and rebuilt from scratch. If the property is {@code false} and the search index does
     * not exist yet, it is created and initialized. Otherwise nothing happens.
     */
    @EventListener
    @Transactional(readOnly = true)
    public void initSearchIndex(ApplicationStartedEvent event) {
        if (!isEnabled() || !clearOnStart && searchOperations.indexOps(ExtensionSearch.class).exists()) {
            return;
        }
        var stopWatch = new StopWatch();
        stopWatch.start();
        updateSearchIndex(clearOnStart);
        stopWatch.stop();
        logger.info("Initialized search index in " + stopWatch.getTotalTimeMillis() + " ms");
    }

    /**
     * Task scheduled once per day to soft-update the search index. This is necessary
     * because the relevance of index entries might consider the extension publishing
     * timestamps in relation to the current time.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional(readOnly = true)
    public void updateSearchIndex() {
        if (!isEnabled() || Math.abs(timestampRelevance) < 0.01) {
            return;
        }
        var stopWatch = new StopWatch();
        stopWatch.start();
        updateSearchIndex(false);
        stopWatch.stop();
        logger.info("Updated search index in " + stopWatch.getTotalTimeMillis() + " ms");
    }

    /**
     * Updating the search index has two modes:
     * <em>soft</em> ({@code clear} is set to {@code false}) means the index is created
     * if it does not exist yet, and
     * <em>hard</em> ({@code clear} is set to {@code true}) means the index is deleted
     * and then recreated.
     * In any case, this method scans all extensions in the database and indexes their
     * relevant metadata.
     */
    public void updateSearchIndex(boolean clear) {
        var locked = false;
        try {
            var indexOps = searchOperations.indexOps(ExtensionSearch.class);
            if (clear) {
                // Hard mode: delete the index if it exists, then recreate it
                rwLock.writeLock().lock();
                locked = true;
                if (indexOps.exists()) {
                    indexOps.delete();
                }
                indexOps.create();
            } else if (!indexOps.exists()) {
                // Soft mode: the index is created only when it does not exist yet
                rwLock.writeLock().lock();
                locked = true;
                indexOps.create();
            }
            
            // Scan all extensions and create index queries
            var allExtensions = repositories.findAllActiveExtensions();
            if (allExtensions.isEmpty()) {
                return;
            }
            var stats = new SearchStats(repositories);
            var indexQueries = allExtensions.map(extension ->
                new IndexQueryBuilder()
                    .withObject(relevanceService.toSearchEntry(extension, stats))
                    .build()
            ).toList();

            if (!locked) {
                // The write lock has not been acquired upfront, so do it just before submitting the index queries
                rwLock.writeLock().lock();
                locked = true;
            }
            searchOperations.bulkIndex(indexQueries, indexOps.getIndexCoordinates());
        } finally {
            if (locked) {
                rwLock.writeLock().unlock();
            }
        }
    }

    public void updateSearchEntry(Extension extension) {
        if (!isEnabled()) {
            return;
        }
        try {
            rwLock.writeLock().lock();
            var stats = new SearchStats(repositories);
            var indexQuery = new IndexQueryBuilder()
                    .withObject(relevanceService.toSearchEntry(extension, stats))
                    .build();
            var indexOps = searchOperations.indexOps(ExtensionSearch.class);
            searchOperations.index(indexQuery, indexOps.getIndexCoordinates());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeSearchEntry(Extension extension) {
        if (!isEnabled()) {
            return;
        }
        try {
            rwLock.writeLock().lock();
            var indexOps = searchOperations.indexOps(ExtensionSearch.class);
            searchOperations.delete(Long.toString(extension.getId()), indexOps.getIndexCoordinates());
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public SearchHits<ExtensionSearch> search(Options options) {
        var indexOps = searchOperations.indexOps(ExtensionSearch.class);
        var settings = indexOps.getSettings(true);
        var maxResultWindow = Long.parseLong(settings.get("index.max_result_window").toString());
        var resultWindow = options.requestedOffset + options.requestedSize;
        if(resultWindow > maxResultWindow) {
            throw new ErrorResultException("Result window is too large, offset + size must be less than or equal to: " + maxResultWindow + " but was " + resultWindow);
        }

        var queryBuilder = new NativeSearchQueryBuilder();
        if (!Strings.isNullOrEmpty(options.queryString)) {
            var boolQuery = QueryBuilders.boolQuery();

            boolQuery.should(QueryBuilders.termQuery("extensionId.keyword", options.queryString)).boost(10);

            // Fuzzy matching of search query in multiple fields
            var multiMatchQuery = QueryBuilders.multiMatchQuery(options.queryString)
                    .field("name").boost(5)
                    .field("displayName").boost(5)
                    .field("tags").boost(3)
                    .field("namespace").boost(2)
                    .field("description")
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2);
            boolQuery.should(multiMatchQuery).boost(5);

            // Prefix matching of search query in display name and namespace
            var prefixString = options.queryString.trim().toLowerCase();
            var namePrefixQuery = QueryBuilders.prefixQuery("displayName", prefixString);
            boolQuery.should(namePrefixQuery).boost(2);
            var namespacePrefixQuery = QueryBuilders.prefixQuery("namespace", prefixString);
            boolQuery.should(namespacePrefixQuery);

            queryBuilder.withQuery(boolQuery);
        }

        if (!Strings.isNullOrEmpty(options.category)) {
            // Filter by selected category
            queryBuilder.withFilter(QueryBuilders.matchPhraseQuery("categories", options.category));
        }
        if (TargetPlatform.isValid(options.targetPlatform)) {
            // Filter by selected target platform
            queryBuilder.withFilter(QueryBuilders.matchPhraseQuery("targetPlatforms", options.targetPlatform));
        }

        // Sort search results according to 'sortOrder' and 'sortBy' options
        sortResults(queryBuilder, options.sortOrder, options.sortBy);

        var pages = new ArrayList<Pageable>();
        pages.add(PageRequest.of(options.requestedOffset / options.requestedSize, options.requestedSize));
        if(options.requestedOffset % options.requestedSize > 0) {
            // size is not exact multiple of offset; this means we need to get two pages
            // e.g. when offset is 20 and size is 50, you want results 20 to 70 which span pages 0 and 1 of a 50 item page
            pages.add(pages.get(0).next());
        }

        var searchHitsList = new ArrayList<SearchHits<ExtensionSearch>>(pages.size());
        for(var page : pages) {
            queryBuilder.withPageable(page);
            try {
                rwLock.readLock().lock();
                var searchHits = searchOperations.search(queryBuilder.build(), ExtensionSearch.class, indexOps.getIndexCoordinates());
                searchHitsList.add(searchHits);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        if(searchHitsList.size() == 2) {
            var firstSearchHitsPage = searchHitsList.get(0);
            var secondSearchHitsPage = searchHitsList.get(1);

            List<SearchHit<ExtensionSearch>> searchHits = new ArrayList<>(firstSearchHitsPage.getSearchHits());
            searchHits.addAll(secondSearchHitsPage.getSearchHits());
            var endIndex = Math.min(searchHits.size(), options.requestedOffset + options.requestedSize);
            var startIndex = Math.min(endIndex, options.requestedOffset);
            searchHits = searchHits.subList(startIndex, endIndex);
            return new SearchHitsImpl<>(
                    firstSearchHitsPage.getTotalHits(),
                    firstSearchHitsPage.getTotalHitsRelation(),
                    firstSearchHitsPage.getMaxScore(),
                    null,
                    searchHits,
                    null
            );
        } else {
            return searchHitsList.get(0);
        }
    }

    private void sortResults(NativeSearchQueryBuilder queryBuilder, String sortOrder, String sortBy) {
        if (!"asc".equalsIgnoreCase(sortOrder) && !"desc".equalsIgnoreCase(sortOrder)) {
            throw new ErrorResultException("sortOrder parameter must be either 'asc' or 'desc'.");
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
    }
}
