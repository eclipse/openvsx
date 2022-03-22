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

import java.util.Objects;
import org.springframework.data.domain.Pageable;
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
    SearchHits<ExtensionSearch> search(Options options, Pageable pageRequest);

    /**
     * Updating the search index has two modes:
     * <em>soft</em> ({@code clear} is set to {@code false}) means the index is created
     * if it does not exist yet, and
     * <em>hard</em> ({@code clear} is set to {@code true}) means the index is deleted
     * and then recreated.
     */
    void updateSearchIndex(boolean clear);

    /**
     * The given extension has been added to the registry, we need to refresh the search index.
     */
    void updateSearchEntry(Extension extension);

    /**
     * The given extension has been removed from the registry, we need to refresh the search index.
     */
    void removeSearchEntry(Extension extension);

    public static class Options {
        public final String queryString;
        public final String category;
        public final String targetPlatform;
        public final int requestedSize;
        public final int requestedOffset;
        public final String sortOrder;
        public final String sortBy;
        public final boolean includeAllVersions;

        public Options(String queryString, String category, String targetPlatform, int size, int offset,
                       String sortOrder, String sortBy, boolean includeAllVersions) {
            this.queryString = queryString;
            this.category = category;
            this.targetPlatform = targetPlatform;
            this.requestedSize = size;
            this.requestedOffset = offset;
            this.sortOrder = sortOrder;
            this.sortBy = sortBy;
            this.includeAllVersions = includeAllVersions;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof Options))
                return false;
            var other = (Options) obj;
            if (!Objects.equals(this.queryString, other.queryString))
                return false;
            if (!Objects.equals(this.category, other.category))
                return false;
            if (!Objects.equals(this.targetPlatform, other.targetPlatform))
                return false;
            if (this.requestedSize != other.requestedSize)
                return false;
            if (this.requestedOffset != other.requestedOffset)
                return false;
            if (!Objects.equals(this.sortOrder, other.sortOrder))
                return false;
            if (!Objects.equals(this.sortBy, other.sortBy))
                return false;
            if (this.includeAllVersions != other.includeAllVersions)
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(queryString, category, targetPlatform, requestedSize, requestedOffset,
                    sortOrder, sortBy, includeAllVersions);
        }
    }


}
