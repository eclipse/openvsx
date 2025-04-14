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

import jakarta.transaction.Transactional;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.RelevanceService.SearchStats;
import org.eclipse.openvsx.util.TargetPlatform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.openvsx.cache.CacheService.CACHE_AVERAGE_REVIEW_RATING;
import static org.eclipse.openvsx.cache.CacheService.CACHE_DATABASE_SEARCH;

/**
 * Alternative to ElasticSearch service using database search.
 */
@Component
public class DatabaseSearchService implements ISearchService {

    private final RelevanceService relevanceService;
    private final RepositoryService repositories;

    @Value("${ovsx.databasesearch.enabled:false}")
    boolean enableSearch;

    public DatabaseSearchService(RelevanceService relevanceService, RepositoryService repositories) {
        this.relevanceService = relevanceService;
        this.repositories = repositories;
    }

    public boolean isEnabled() {
        return enableSearch;
    }

    @Transactional
    @Cacheable(CACHE_DATABASE_SEARCH)
    @CacheEvict(value = CACHE_AVERAGE_REVIEW_RATING, allEntries = true)
    public SearchHits<ExtensionSearch> search(ISearchService.Options options) {
        // grab all extensions
        var matchingExtensions = repositories.findAllActiveExtensions();

        // no extensions in the database
        if (matchingExtensions.isEmpty()) {
            return new SearchHitsImpl<>(0,TotalHitsRelation.OFF, 0f, null, null, Collections.emptyList(), null, null, null);
        }

        // exlude namespaces
        if(options.namespacesToExclude() != null) {
            for(var namespaceToExclude : options.namespacesToExclude()) {
                matchingExtensions = matchingExtensions.filter(extension -> !extension.getNamespace().getName().equals(namespaceToExclude));
            }
        }

        // filter target platform
        if(TargetPlatform.isValid(options.targetPlatform())) {
            matchingExtensions = matchingExtensions.filter(extension -> extension.getVersions().stream().anyMatch(ev -> ev.getTargetPlatform().equals(options.targetPlatform())));
        }

        // filter category
        if (options.category() != null) {
            matchingExtensions = matchingExtensions.filter(extension -> {
                var latest = repositories.findLatestVersion(extension, null, false, true);
                return latest.getCategories().stream().anyMatch(category -> category.equalsIgnoreCase(options.category()));
            });
        }

        // filter text
        if (options.queryString() != null) {
            matchingExtensions = matchingExtensions.filter(extension -> {
                var latest = repositories.findLatestVersion(extension, null, false, true);
                return extension.getName().toLowerCase().contains(options.queryString().toLowerCase())
                    || extension.getNamespace().getName().contains(options.queryString().toLowerCase())
                    || (latest.getDescription() != null && latest.getDescription()
                        .toLowerCase().contains(options.queryString().toLowerCase()))
                    || (latest.getDisplayName() != null && latest.getDisplayName()
                        .toLowerCase().contains(options.queryString().toLowerCase()));
            });
        }

        // need to perform the sortBy ()
        // 'relevance' | 'timestamp' | 'rating' | 'downloadCount';

        Stream<ExtensionSearch> searchEntries;
        if("relevance".equals(options.sortBy()) || "rating".equals(options.sortBy())) {
            var searchStats = new SearchStats(repositories);
            searchEntries = matchingExtensions.stream().map(extension -> relevanceService.toSearchEntry(extension, searchStats));
        } else {
            searchEntries = matchingExtensions.stream().map(extension -> {
                var latest = repositories.findLatestVersion(extension, null, false, true);
                var targetPlatforms = repositories.findExtensionTargetPlatforms(extension);
                return extension.toSearch(latest, targetPlatforms);
            });
        }

        var comparators = new HashMap<>(Map.of(
                "relevance", new RelevanceComparator(),
                "timestamp", new TimestampComparator(),
                "rating", new RatingComparator(),
                "downloadCount", new DownloadedCountComparator()
        ));

        var comparator = comparators.get(options.sortBy());
        if(comparator != null) {
            searchEntries = searchEntries.sorted(comparator);
        }

        var sortedExtensions = searchEntries.collect(Collectors.toList());

        // need to do sortOrder
        // 'asc' | 'desc';
        if ("desc".equals(options.sortOrder())) {
            // reverse the order
            Collections.reverse(sortedExtensions);
        }

        // Paging
        var totalHits = sortedExtensions.size();
        var endIndex = Math.min(sortedExtensions.size(), options.requestedOffset() + options.requestedSize());
        var startIndex = Math.min(endIndex, options.requestedOffset());
        sortedExtensions = sortedExtensions.subList(startIndex, endIndex);

        List<SearchHit<ExtensionSearch>> searchHits;
        if (sortedExtensions.isEmpty()) {
            searchHits = Collections.emptyList();
        } else {
            // client is interested only in the extension IDs
            searchHits = sortedExtensions.stream().map(extensionSearch -> new SearchHit<>(null, null, null, 0.0f, null, null, null, null, null, null, extensionSearch)).collect(Collectors.toList());
        }

        return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 0f, null, null, searchHits, null, null, null);
    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchIndex(boolean clear) {
        // The @CacheEvict annotation clears the cache when asked to update the search index
    }

    @Override
    @Async
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchEntriesAsync(List<Extension> extensions) {
        // The @CacheEvict annotation clears the cache when asked to update search entries
    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchEntries(List<Extension> extensions) {
        // The @CacheEvict annotation clears the cache when asked to update search entries
    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchEntry(Extension extension) {
        // The @CacheEvict annotation clears the cache when asked to update a search entry
    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void removeSearchEntries(Collection<Long> ids) {
        // The @CacheEvict annotation clears the cache when asked to removes search entries
    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void removeSearchEntry(Extension extension) {
        // The @CacheEvict annotation clears the cache when asked to remove a search entry
    }

    /**
     * Sort by download count
     */
    class DownloadedCountComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            // download count
            return Integer.compare(ext1.getDownloadCount(), ext2.getDownloadCount());
        }
    }

    /**
     * Sort by averageRating
     */
    class RatingComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            if (ext1.getRating() == null) {
                return -1;
            } else if (ext2.getRating() == null) {
                return 1;
            }
            // rating
            return Double.compare(ext1.getRating(), ext2.getRating());
        }
    }

    /**
     * Sort by timestamp
     */
    class TimestampComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            // timestamp
            return Long.compare(ext1.getTimestamp(), ext2.getTimestamp());
        }
    }

    /**
     * Sort by relevance
     */
    class RelevanceComparator implements Comparator<ExtensionSearch> {

        @Override
        public int compare(ExtensionSearch ext1, ExtensionSearch ext2) {
            return Double.compare(ext1.getRelevance(), ext2.getRelevance());
        }
    }
}
