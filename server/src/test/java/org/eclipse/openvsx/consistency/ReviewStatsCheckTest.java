/******************************************************************************
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
 *****************************************************************************/
package org.eclipse.openvsx.consistency;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.eclipse.openvsx.AbstractPostgresContainerTest;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.search.SearchUtilService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests {@link ReviewStatsCheck} (issue #2327), run through {@link ConsistencyCheckService}.
 * <p>
 * {@code @Isolated}: the check scans every {@code Extension} row, colliding with unrelated tests
 * concurrently mutating their own.
 */
@SpringBootTest
@Isolated
class ReviewStatsCheckTest extends AbstractPostgresContainerTest {

    private static final String NAMESPACE = "review-stats-testns";
    private static final String EXTENSION = "review-stats-testext";
    private static final String USER_PREFIX = "review-stats-user";

    @Autowired
    ConsistencyCheckService consistencyCheckService;

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager txManager;

    @MockitoBean
    SearchUtilService search;

    @MockitoBean
    CacheService cache;

    @MockitoBean
    JobRequestScheduler scheduler;

    private long extensionId;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var namespace = new Namespace();
            namespace.setName(NAMESPACE);
            em.persist(namespace);

            var extension = new Extension();
            extension.setName(EXTENSION);
            extension.setNamespace(namespace);
            extension.setActive(true);
            em.persist(extension);
            em.flush();
            extensionId = extension.getId();
        });
    }

    @Test
    void fixesNeverSetStats() {
        givenActiveReviews(4, 2);
        setStats(null, null);

        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isEqualTo(1);
        assertStats(2L, 3.0);
        verifyRepaired();
    }

    @Test
    void fixesStaleStats() {
        givenActiveReviews(4, 2);
        setStats(3L, 4.5);

        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isEqualTo(1);
        assertStats(2L, 3.0);
        verifyRepaired();
    }

    @Test
    void ignoresInactiveReviews() {
        givenActiveReviews(4, 2);
        givenInactiveReview(1);
        setStats(2L, 3.0);

        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isZero();
        verifyNothingRepaired();
    }

    @Test
    void noReviews_nullAndZeroStatsAreConsistent() {
        givenInactiveReview(5);

        setStats(null, null);
        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isZero();

        setStats(0L, 0.0);
        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isZero();
        verifyNothingRepaired();
    }

    @Test
    void noReviews_nonZeroStatsAreRepairedToNull() {
        setStats(1L, 4.0);

        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isEqualTo(1);
        assertStats(0L, null);
        verifyRepaired();
    }

    @Test
    void ratingTolerance_boundary() {
        givenActiveReviews(4, 2);

        setStats(2L, 3.0005);
        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isZero();
        verifyNothingRepaired();

        setStats(2L, 3.002);
        assertThat(consistencyCheckService.fixAll(ReviewStatsCheck.ID)).isEqualTo(1);
        assertStats(2L, 3.0);
        verifyRepaired();
    }

    private void verifyRepaired() {
        verify(search).updateSearchEntry(any(Extension.class));
        verify(cache).evictExtensionJsons(any(Extension.class));
        verify(cache).evictLatestExtensionVersion(any(Extension.class));
    }

    private void verifyNothingRepaired() {
        verify(search, never()).updateSearchEntry(any(Extension.class));
        verifyNoInteractions(cache);
    }

    private void givenActiveReviews(int... ratings) {
        for (var rating : ratings) {
            persistReview(rating, true);
        }
    }

    private void givenInactiveReview(int rating) {
        persistReview(rating, false);
    }

    private void persistReview(int rating, boolean active) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var user = new UserData();
            user.setLoginName(USER_PREFIX + System.nanoTime());
            em.persist(user);

            var review = new ExtensionReview();
            review.setExtension(em.getReference(Extension.class, extensionId));
            review.setUser(user);
            review.setRating(rating);
            review.setActive(active);
            review.setTimestamp(LocalDateTime.now());
            em.persist(review);
        });
    }

    private void setStats(Long count, Double rating) {
        new TransactionTemplate(txManager).executeWithoutResult(
                status -> em
                        .createNativeQuery("update extension set review_count = :c, average_rating = :r where id = :id")
                        .setParameter("c", count)
                        .setParameter("r", rating)
                        .setParameter("id", extensionId)
                        .executeUpdate());
    }

    private void assertStats(Long count, Double rating) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.clear();
            var extension = em.find(Extension.class, extensionId);
            assertThat(extension.getReviewCount()).isEqualTo(count);
            assertThat(extension.getAverageRating()).isEqualTo(rating);
        });
    }

    @AfterEach
    void tearDown() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.createQuery("delete from PersistedLog p where p.user.loginName = :loginName")
                    .setParameter("loginName", "ConsistencyCheckUser").executeUpdate();
            em.createQuery("delete from ExtensionReview r where r.extension.namespace.name = :namespace")
                    .setParameter("namespace", NAMESPACE).executeUpdate();
            em.createQuery("delete from Extension e where e.namespace.name = :namespace")
                    .setParameter("namespace", NAMESPACE).executeUpdate();
            em.createQuery("delete from Namespace n where n.name = :namespace")
                    .setParameter("namespace", NAMESPACE).executeUpdate();
            em.createQuery("delete from UserData u where u.loginName like :prefix")
                    .setParameter("prefix", USER_PREFIX + "%").executeUpdate();
        });
    }
}
