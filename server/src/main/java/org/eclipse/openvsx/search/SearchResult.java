/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.search;

import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;

public class SearchResult {

    private long totalHits;
    private List<ExtensionSearch> hits;

    public SearchResult() {
        totalHits = 0;
        hits = Collections.emptyList();
    }

    public SearchResult(long totalHits, @NotNull List<ExtensionSearch> hits) {
        this.totalHits = totalHits;
        this.hits = hits;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(long totalHits) {
        this.totalHits = totalHits;
    }

    public List<ExtensionSearch> getHits() {
        return hits;
    }

    public void setHits(List<ExtensionSearch> hits) {
        this.hits = hits;
    }

    public boolean hasSearchHits() {
        return !hits.isEmpty();
    }
}
