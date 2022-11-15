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
import org.eclipse.openvsx.util.VersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.stereotype.Component;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.RelevanceService.SearchStats;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import javax.transaction.Transactional;

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

    @Autowired
    VersionService versions;

    @Cacheable(CACHE_DATABASE_SEARCH)
    @Transactional
    public SearchHits<ExtensionSearch> search(ISearchService.Options options) {
        // grab all extensions
        var matchingExtensions = repositories.findAllActiveExtensions();

        // no extensions in the database
        if (matchingExtensions.isEmpty()) {
            return new SearchHitsImpl<>(0,TotalHitsRelation.OFF, 0f, "", Collections.emptyList(), null, null);
        }

        // exlude namespaces
        if(options.namespacesToExclude != null) {
            for(var namespaceToExclude : options.namespacesToExclude) {
                matchingExtensions = matchingExtensions.filter(extension -> !extension.getNamespace().getName().equals(namespaceToExclude));
            }
        }

        // filter target platform
        if(TargetPlatform.isValid(options.targetPlatform)) {
            matchingExtensions = matchingExtensions.filter(extension -> extension.getVersions().stream().anyMatch(ev -> ev.getTargetPlatform().equals(options.targetPlatform)));
        }

        // filter category
        if (options.category != null) {
            matchingExtensions = matchingExtensions.filter(extension -> {
                var latest = versions.getLatest(extension, null, false, true);
                return latest.getCategories().stream().anyMatch(category -> category.equalsIgnoreCase(options.category));
            });
        }

        // filter text
        if (options.queryString != null) {
            matchingExtensions = matchingExtensions.filter(extension -> {
                var latest = versions.getLatest(extension, null, false, true);
                return extension.getName().toLowerCase().contains(options.queryString.toLowerCase())
                    || extension.getNamespace().getName().contains(options.queryString.toLowerCase())
                    || (latest.getDescription() != null && latest.getDescription()
                        .toLowerCase().contains(options.queryString.toLowerCase()))
                    || (latest.getDisplayName() != null && latest.getDisplayName()
                        .toLowerCase().contains(options.queryString.toLowerCase()));
            });
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
                    .map(extension -> {
                        var latest = versions.getLatest(extension, null, false, true);
                        return extension.toSearch(latest);
                    })
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
        var endIndex = Math.min(sortedExtensions.size(), options.requestedOffset + options.requestedSize);
        var startIndex = Math.min(endIndex, options.requestedOffset);
        sortedExtensions = sortedExtensions.subList(startIndex, endIndex);

        List<SearchHit<ExtensionSearch>> searchHits;
        if (sortedExtensions.isEmpty()) {
            searchHits = Collections.emptyList();
        } else {
            // client is interested only in the extension IDs
            searchHits = sortedExtensions.stream().map(extensionSearch -> new SearchHit<>(null, null, null, 0.0f, null, null, null, null, null, null, extensionSearch)).collect(Collectors.toList());
        }

        return new SearchHitsImpl<>(totalHits, TotalHitsRelation.OFF, 0f, "", searchHits, null, null);
    }

    /**
     * Clear the cache when asked to update the search index. It could be done also
     * through a cron job as well
     */
    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchIndex(boolean clear) {

    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchEntries(List<Extension> extensions) {

    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void updateSearchEntry(Extension extension) {

    }

    @Override
    @CacheEvict(value = CACHE_DATABASE_SEARCH, allEntries = true)
    public void removeSearchEntry(Extension extension) {

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
