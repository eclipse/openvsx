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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.eclipse.openvsx.entities.Extension;

/**
 * Common interface for all search service implementations.
 */
public interface ISearchService {
    
    /**
     * Indicates whether this storage service is enabled by application config.
     */
    boolean isEnabled();

    /**
     * Search with given options
     */
    SearchHits<ExtensionSearch> search(Options options);

    /**
     * Updating the search index has two modes:
     * <em>soft</em> ({@code clear} is set to {@code false}) means the index is created
     * if it does not exist yet, and
     * <em>hard</em> ({@code clear} is set to {@code true}) means the index is deleted
     * and then recreated.
     */
    void updateSearchIndex(boolean clear);

    /**
     * The given extensions have been added to the registry, we need to refresh the search index.
     */
    void updateSearchEntries(List<Extension> extensions);

    void updateSearchEntriesAsync(List<Extension> extensions);

    /**
     * The given extension has been added to the registry, we need to refresh the search index.
     */
    void updateSearchEntry(Extension extension);

    /**
     * The given extension has been removed from the registry, we need to refresh the search index.
     */
    void removeSearchEntry(Extension extension);

    /**
     * The given extensions have been removed from the registry, we need to refresh the search index.
     */
    void removeSearchEntries(Collection<Long> ids);

    public static class Options {
        public final String queryString;
        public final String category;
        public final String targetPlatform;
        public final int requestedSize;
        public final int requestedOffset;
        public final String sortOrder;
        public final String sortBy;
        public final boolean includeAllVersions;
        public final String[] namespacesToExclude;

        public Options(String queryString, String category, String targetPlatform, int size, int offset,
                       String sortOrder, String sortBy, boolean includeAllVersions, String... namespacesToExclude) {
            this.queryString = queryString;
            this.category = category;
            this.targetPlatform = targetPlatform;
            this.requestedSize = size;
            this.requestedOffset = offset;
            this.sortOrder = sortOrder;
            this.sortBy = sortBy;
            this.includeAllVersions = includeAllVersions;
            this.namespacesToExclude = namespacesToExclude;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Options options = (Options) o;
            return requestedSize == options.requestedSize
                    && requestedOffset == options.requestedOffset
                    && includeAllVersions == options.includeAllVersions
                    && Objects.equals(queryString, options.queryString)
                    && Objects.equals(category, options.category)
                    && Objects.equals(targetPlatform, options.targetPlatform)
                    && Objects.equals(sortOrder, options.sortOrder)
                    && Objects.equals(sortBy, options.sortBy)
                    && Arrays.equals(namespacesToExclude, options.namespacesToExclude);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(queryString, category, targetPlatform, requestedSize, requestedOffset, sortOrder, sortBy, includeAllVersions);
            result = 31 * result + Arrays.hashCode(namespacesToExclude);
            return result;
        }
    }
}
