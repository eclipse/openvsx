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

import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.openvsx.util.TargetPlatform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.RelevanceService.SearchStats;
import org.elasticsearch.search.aggregations.Aggregations;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import static org.eclipse.openvsx.cache.CacheService.CACHE_DATABASE_SEARCH;

/**
 * Alternative to ElasticSearch service using database search.
 */
@Component
public class DatabaseSearchService implements ISearchService {

    @Value("${ovsx.databasesearch.enabled:false}")
    boolean enableSearch;

    @Autowired
    RelevanceService relevanceService;

    public boolean isEnabled() {
        return enableSearch;
    }

    @Autowired
    RepositoryService repositories;

    @Cacheable(CACHE_DATABASE_SEARCH)
    public SearchHits<ExtensionSearch> search(ISearchService.Options options, Pageable pageRequest) {
        // grab all extensions
        var matchingExtensions = repositories.findAllActiveExtensions();

        // no extensions in the database
        if (matchingExtensions.isEmpty()) {
            Aggregations aggregations = new Aggregations(Collections.emptyList());
            return new SearchHitsImpl<ExtensionSearch>(0,TotalHitsRelation.OFF, 0f, "", Collections.emptyList(), aggregations);
        }

        // filter target platform
        if(TargetPlatform.isValid(options.targetPlatform)) {
            matchingExtensions = matchingExtensions.filter(extension -> extension.getVersions().stream().anyMatch(ev -> ev.getTargetPlatform().equals(options.targetPlatform)));
        }

        // filter category
        if (options.category != null) {
            matchingExtensions = matchingExtensions.filter(extension -> extension.getLatest().getCategories().stream()
                    .anyMatch(category -> category.equalsIgnoreCase(options.category)));
        }

        // filter text
        if (options.queryString != null) {
            matchingExtensions = matchingExtensions.filter(extension ->
            extension.getName().toLowerCase().contains(options.queryString.toLowerCase())
                    || extension.getNamespace().getName().contains(options.queryString.toLowerCase())
                    || (extension.getLatest().getDescription() != null && extension.getLatest().getDescription()
                            .toLowerCase().contains(options.queryString.toLowerCase()))
                    || (extension.getLatest().getDisplayName() != null && extension.getLatest().getDisplayName()
                            .toLowerCase().contains(options.queryString.toLowerCase())));
        }

        List<ExtensionSearch> sortedExtensions;

        // need to perform the sortBy ()
        // 'relevance' | 'timestamp' | 'averageRating' | 'downloadCount';

        if ("relevance".equals(options.sortBy)) {
            // for relevance we're using relevance service to get the relevance item
            var searchStats = new SearchStats(repositories);

            // needs to add relevance on extensions
            sortedExtensions = matchingExtensions
                    .map(extension -> relevanceService.toSearchEntry(extension, searchStats))
                    .stream()
                    .sorted(new RelevanceComparator())
                    .collect(Collectors.toList());
        } else {
            sortedExtensions = matchingExtensions.stream()
                    .map(Extension::toSearch)
                    .collect(Collectors.toList());
            if ("downloadCount".equals(options.sortBy)) {
                sortedExtensions.sort(new DownloadedCountComparator());
            } else if ("averageRating".equals(options.sortBy)) {
                sortedExtensions.sort(new AverageRatingComparator());
            } else if ("timestamp".equals(options.sortBy)) {
                sortedExtensions.sort(new TimestampComparator());
            }
        }

        // need to do sortOrder
        // 'asc' | 'desc';
        if ("desc".equals(options.sortOrder)) {
            // reverse the order
            Collections.reverse(sortedExtensions);
        }

        // Paging
        var totalHits = sortedExtensions.size();
        if (pageRequest != null) {
            var pageNumber = pageRequest.getPageNumber();
            var pageSize = pageRequest.getPageSize();

            var toSkip = 0;
            if (pageNumber >= 1) {
                // page is zero indexed
                toSkip = pageNumber * (pageSize - 1) + pageNumber;
            }
            // if something to skip, remove the first elements
            if (toSkip > 0 && toSkip < sortedExtensions.size()) {
                sortedExtensions = sortedExtensions.subList(toSkip, sortedExtensions.size());
            }

            // keep only the pageSize elements
            if (sortedExtensions.size() > pageSize) {
                sortedExtensions = sortedExtensions.subList(0, pageSize);
            }
        }

        List<SearchHit<ExtensionSearch>> searchHits;
        if (sortedExtensions.isEmpty()) {
            searchHits = Collections.emptyList();
        } else {
            // client is interested only in the extension IDs
            searchHits = sortedExtensions.stream().map(extensionSearch -> {
                return new SearchHit<ExtensionSearch>(null, null, 0.0f, Collections.emptyList().toArray(),
                        Collections.emptyMap(), extensionSearch);
            }).collect(Collectors.toList());
        }

        Aggregations aggregations = new Aggregations(Collections.emptyList());
        SearchHits<ExtensionSearch> searchHitsResult = new SearchHitsImpl<ExtensionSearch>(totalHits,
                TotalHitsRelation.OFF, 0f, "", searchHits, aggregations);

        return searchHitsResult;
    }

    /**
     * Clear the cache when asked to update the search index. It could be done also
     * through a cron job as well
     */
    @CacheEvict(value = CACHE_DATABASE_SEARCH)
    @Override
    public void updateSearchIndex(boolean clear) {

    }

    @Override
    public void updateSearchEntry(Extension extension) {
        // refresh the index
        this.updateSearchIndex(true);

    }

    @Override
    public void removeSearchEntry(Extension extension) {
        // refresh the index
        this.updateSearchIndex(true);
    }

    /**
     * Sort by download count
     */
    class DownloadedCountComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            // download count
            return Integer.compare(ext1.downloadCount, ext2.downloadCount);
        }
    }

    /**
     * Sort by averageRating
     */
    class AverageRatingComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            if (ext1.averageRating == null) {
                return -1;
            } else if (ext2.averageRating == null) {
                return 1;
            }
            // averageRating
            return Double.compare(ext1.averageRating, ext2.averageRating);
        }
    }

    /**
     * Sort by timestamp
     */
    class TimestampComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            // timestamp
            return Long.compare(ext1.timestamp, ext2.timestamp);
        }

    }

    /**
     * Sort by relevance
     */
    class RelevanceComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            return Double.compare(ext1.relevance, ext2.relevance);
        }

    }

}
