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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.eclipse.openvsx.entities.Extension;

import java.util.Collection;
import java.util.List;

/**
 * Wrap all available implementations and redirect to the implementation pickup
 * from configuration
 */
@Component
public class SearchUtilService implements ISearchService {

    @Autowired
    DatabaseSearchService databaseSearchService;

    @Autowired
    ElasticSearchService elasticSearchService;

    public boolean isEnabled() {
        return this.databaseSearchService.isEnabled() || this.elasticSearchService.isEnabled();
    }

    /**
     * Take the implementation being enabled. If two are defined, it's a
     * configuration error.
     */
    protected ISearchService getImplementation() {
        if (databaseSearchService.isEnabled() && elasticSearchService.isEnabled()) {
            throw new IllegalStateException(
                    "Only one search engine can be enabled at a time. Here both elasticsearch and database search are enabled.");
        }

        if (this.databaseSearchService.isEnabled()) {
            return this.databaseSearchService;
        }

        // return default implementation which is elastic search
        return this.elasticSearchService;

    }

    public SearchHits<ExtensionSearch> search(ElasticSearchService.Options options) {
        return getImplementation().search(options);
    }

    @Override
    public void updateSearchIndex(boolean clear) {
        getImplementation().updateSearchIndex(clear);
    }

    @Override
    public void updateSearchEntries(List<Extension> extensions) {
        getImplementation().updateSearchEntries(extensions);
    }

    @Override
    public void updateSearchEntriesAsync(List<Extension> extensions) {
        getImplementation().updateSearchEntriesAsync(extensions);
    }

    @Override
    public void updateSearchEntry(Extension extension) {
        getImplementation().updateSearchEntry(extension);
    }

    @Override
    public void removeSearchEntries(Collection<Long> ids) { getImplementation().removeSearchEntries(ids); }

    @Override
    public void removeSearchEntry(Extension extension) {
        getImplementation().removeSearchEntry(extension);
    }

}
