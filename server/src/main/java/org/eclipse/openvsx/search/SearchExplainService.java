/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.search;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

import org.eclipse.openvsx.json.SearchExplainJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.RelevanceService.SearchStats;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;

/**
 * Answers "why is this result here, and why is that one above it".
 * <p>
 * A search score is two numbers multiplied: how well the query matched the document's text, and a
 * relevance computed from the extension's rating, downloads and age when the document was indexed. Only
 * the product is visible from outside, so a result list that looks wrong gives no clue as to which half
 * is responsible - and the two want entirely different fixes. This takes them apart.
 */
@Component
public class SearchExplainService {

    private final ElasticSearchService elasticSearch;
    private final SearchUtilService search;
    private final RelevanceService relevanceService;
    private final RepositoryService repositories;

    public SearchExplainService(
            ElasticSearchService elasticSearch,
            SearchUtilService search,
            RelevanceService relevanceService,
            RepositoryService repositories
    ) {
        this.elasticSearch = elasticSearch;
        this.search = search;
        this.relevanceService = relevanceService;
        this.repositories = repositories;
    }

    public SearchExplainJson explain(String query, int size, String sortBy, String sortOrder) {
        // Only elasticsearch scores anything. The database engine orders rows, so there is no score to
        // take apart and reporting one would be inventing it.
        if (!elasticSearch.isEnabled()) {
            throw new ErrorResultException(
                    "There are no search scores to explain: searches are answered by '"
                            + search.getIndexStats().implementation() + "'.");
        }

        var options = new ISearchService.Options(query, null, null, size, 0, sortOrder, sortBy, false, null);
        var hits = elasticSearch.searchWithScores(options);

        // One SearchStats for the whole listing, as an indexing run would use: recomputing it per entry
        // would let the reference values drift between rows of the same table.
        var stats = new SearchStats(repositories);
        var entries = new ArrayList<SearchExplainJson.SearchExplainEntryJson>(hits.getSearchHits().size());
        var position = 0;
        for (var hit : hits.getSearchHits()) {
            entries.add(toEntry(position++, hit.getScore(), hit.getContent(), stats));
        }

        return new SearchExplainJson(
                query,
                hits.getTotalHits(),
                new SearchExplainJson.SearchReferencesJson(
                        (long) stats.downloadRef,
                        stats.oldest == null ? null : TimeUtil.toUTCString(stats.oldest),
                        stats.averageReviewRating),
                entries);
    }

    private SearchExplainJson.SearchExplainEntryJson toEntry(
            int position,
            float score,
            ExtensionSearch document,
            SearchStats stats
    ) {
        // Recomputed from the extension rather than read off the document, which holds only the product.
        // Where the two disagree the index is stale, and saying so is more use than hiding it.
        var extension = repositories.findExtension(document.getId());
        var breakdown = extension == null ? null : relevanceService.explainRelevance(extension, stats);

        var storedRelevance = document.getRelevance();
        // The query multiplies the text score by the stored relevance, so dividing recovers the other
        // half. A relevance of zero leaves nothing to recover - the product is zero whatever matched.
        var textScore = storedRelevance == 0.0 ? null : score / storedRelevance;

        return new SearchExplainJson.SearchExplainEntryJson(
                position,
                document.getNamespace(),
                document.getName(),
                document.getDownloadCount(),
                document.getRating(),
                TimeUtil.toUTCString(LocalDateTime.ofEpochSecond(document.getTimestamp(), 0, ZoneOffset.UTC)),
                score,
                textScore,
                storedRelevance,
                breakdown == null ? null : breakdown.total(),
                breakdown == null ? null : breakdown.rating(),
                breakdown == null ? null : breakdown.downloads(),
                breakdown == null ? null : breakdown.timestamp(),
                breakdown != null && breakdown.unverified(),
                breakdown != null && breakdown.deprecated());
    }
}
