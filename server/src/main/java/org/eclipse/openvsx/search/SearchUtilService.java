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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.eclipse.openvsx.entities.Extension;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * Wrap all available implementations and redirect to the implementation pickup
 * from configuration
 */
@Component
public class SearchUtilService implements ISearchService {

    private final DatabaseSearchService databaseSearchService;
    private final ElasticSearchService elasticSearchService;
    private final ObservationRegistry observations;

    public SearchUtilService(
            DatabaseSearchService databaseSearchService,
            ElasticSearchService elasticSearchService,
            ObservationRegistry observations
    ) {
        this.databaseSearchService = databaseSearchService;
        this.elasticSearchService = elasticSearchService;
        this.observations = observations;
    }

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
//        Observation.createNotStarted("SearchUtilService#updateSearchEntriesAsync", observations).observe(() -> {
            getImplementation().updateSearchEntriesAsync(extensions);
//        });
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
