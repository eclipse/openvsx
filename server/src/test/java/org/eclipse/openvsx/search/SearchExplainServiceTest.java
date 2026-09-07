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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;

import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;

/**
 * Taking a search score apart. The score a result ranked on is the product of how well the query matched
 * its text and the relevance stored on its document, and only the product survives into the result list -
 * which is why a result list that looks wrong says nothing about which half to go and fix.
 */
@ExtendWith(MockitoExtension.class)
class SearchExplainServiceTest {

    @Mock
    ElasticSearchService elasticSearch;

    @Mock
    SearchUtilService search;

    @Mock
    RelevanceService relevanceService;

    @Mock
    RepositoryService repositories;

    SearchExplainService explainService;

    @BeforeEach
    void setUp() {
        explainService = new SearchExplainService(elasticSearch, search, relevanceService, repositories);
    }

    private static ExtensionSearch document(long id, String namespace, String name, double relevance) {
        var doc = new ExtensionSearch();
        doc.setId(id);
        doc.setNamespace(namespace);
        doc.setName(name);
        doc.setRelevance(relevance);
        doc.setDownloadCount(640718);
        doc.setTimestamp(LocalDateTime.parse("2024-01-16T04:14:26").toEpochSecond(ZoneOffset.UTC));
        return doc;
    }

    private void givenHits(SearchHit<ExtensionSearch>... hits) {
        Mockito.when(elasticSearch.isEnabled()).thenReturn(true);
        SearchHits<ExtensionSearch> searchHits = new SearchHitsImpl<>(
                hits.length,
                TotalHitsRelation.EQUAL_TO,
                0f,
                Duration.ZERO,
                null,
                null,
                List.of(hits),
                null,
                null,
                null);
        Mockito.when(elasticSearch.searchWithScores(any())).thenReturn(searchHits);
    }

    @SuppressWarnings("unchecked")
    private static SearchHit<ExtensionSearch> hit(ExtensionSearch doc, float score) {
        return new SearchHit<>(null, String.valueOf(doc.getId()), null, score, null, null, null, null, null, null, doc);
    }

    /**
     * The query multiplies the text score by the stored relevance, so dividing the ranked score by the
     * relevance recovers the text score - the half that says whether the query matched this document well,
     * as distinct from whether the registry considers it a good extension.
     */
    @Test
    @SuppressWarnings("unchecked")
    void separatesTheTextScoreFromTheRelevanceItWasMultipliedBy() {
        var doc = document(1L, "yzhang", "markdown-all-in-one", 1.5);
        givenHits(hit(doc, 3.0f));

        var explained = explainService.explain("markdown", 20, "relevance", "desc");

        var entry = explained.entries().getFirst();
        assertThat(entry.score()).isEqualTo(3.0);
        assertThat(entry.storedRelevance()).isEqualTo(1.5);
        assertThat(entry.textScore()).isCloseTo(2.0, within(0.0001));
    }

    // A relevance of zero makes the product zero whatever the query matched, so there is no text score
    // hiding in it to recover - and reporting one would be inventing a number.
    @Test
    @SuppressWarnings("unchecked")
    void reportsNoTextScoreWhenTheRelevanceIsZero() {
        givenHits(hit(document(1L, "foo", "bar", 0.0), 0.0f));

        assertThat(explainService.explain("markdown", 20, "relevance", "desc").entries().getFirst().textScore())
                .isNull();
    }

    // The extension behind a document can have been purged since it was indexed. That is worth reporting
    // as an unknown breakdown rather than failing the whole listing.
    @Test
    @SuppressWarnings("unchecked")
    void survivesAnExtensionThatIsNoLongerThere() {
        givenHits(hit(document(7L, "gone", "away", 1.0), 1.0f));
        Mockito.when(repositories.findExtension(7L)).thenReturn(null);

        var entry = explainService.explain("markdown", 20, "relevance", "desc").entries().getFirst();

        assertThat(entry.namespace()).isEqualTo("gone");
        assertThat(entry.currentRelevance()).isNull();
        assertThat(entry.rating()).isNull();
    }

    // Only elasticsearch scores anything; the database engine orders rows. Reporting a breakdown of a
    // score that was never computed would be describing a search nobody ran.
    @Test
    void refusesWhenElasticsearchIsNotTheEngine() {
        Mockito.when(elasticSearch.isEnabled()).thenReturn(false);
        Mockito.when(search.getIndexStats())
                .thenReturn(new SearchIndexStats(true, SearchIndexStats.DATABASE, false, null, 0L, null));

        assertThatThrownBy(() -> explainService.explain("markdown", 20, "relevance", "desc"))
                .isInstanceOf(ErrorResultException.class)
                .hasMessageContaining("database");
    }
}
