/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.search;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests for SimilarityCheckService which handles enforcement policy.
 */
@ExtendWith(MockitoExtension.class)
class SimilarityCheckServiceTest {

    @Mock
    SimilarityConfig config;

    @Mock
    SimilarityService similarityService;

    @Mock
    RepositoryService repositories;

    @InjectMocks
    SimilarityCheckService similarityCheckService;

    UserData user;

    @BeforeEach
    void setUp() {
        user = new UserData();
        user.setLoginName("testuser");
    }

    @Test
    void shouldReturnEmptyWhenSimilarityChecksDisabled() {
        // When the feature is disabled, no database work should happen.
        when(config.isEnabled()).thenReturn(false);

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verifyNoInteractions(similarityService);
        verifyNoInteractions(repositories);
    }

    @Test
    void shouldExcludeOwnerNamespacesWhenConfigured() {
        // When exclude-owner-namespaces is enabled, we should build a list of owner namespaces to exclude.
        when(config.isEnabled()).thenReturn(true);
        when(config.isExcludeOwnerNamespaces()).thenReturn(true);
        when(config.getLevenshteinThreshold()).thenReturn(0.15);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(false);

        var namespace1 = new Namespace();
        namespace1.setName("owned-ns");
        var membership1 = new NamespaceMembership();
        membership1.setNamespace(namespace1);
        membership1.setRole(NamespaceMembership.ROLE_OWNER);

        var namespace2 = new Namespace();
        namespace2.setName("contributor-ns");
        var membership2 = new NamespaceMembership();
        membership2.setNamespace(namespace2);
        membership2.setRole(NamespaceMembership.ROLE_CONTRIBUTOR);

        when(repositories.findMemberships(user)).thenReturn(Streamable.of(membership1, membership2));
        when(similarityService.findSimilarExtensions("ext", "ns", "Display", List.of("owned-ns"), 0.15, false, 10))
                .thenReturn(List.of());

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verify(repositories).findMemberships(user);
        verify(similarityService).findSimilarExtensions("ext", "ns", "Display", List.of("owned-ns"), 0.15, false, 10);
    }

    @Test
    void shouldNotExcludeNamespacesWhenConfigDisabled() {
        // When exclude-owner-namespaces is disabled, pass an empty exclude list.
        when(config.isEnabled()).thenReturn(true);
        when(config.isExcludeOwnerNamespaces()).thenReturn(false);
        when(config.getLevenshteinThreshold()).thenReturn(0.15);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(false);
        when(similarityService.findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10))
                .thenReturn(List.of());

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verifyNoInteractions(repositories);
        verify(similarityService).findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10);
    }

    @Test
    void shouldDelegateSimilarExtensionsToServiceWhenEnabled() {
        // Happy path: when enabled, delegate to SimilarityService with proper parameters.
        when(config.isEnabled()).thenReturn(true);
        when(config.isExcludeOwnerNamespaces()).thenReturn(false);
        when(config.getLevenshteinThreshold()).thenReturn(0.15);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(false);
        var expected = List.of(new Extension());
        when(similarityService.findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10))
                .thenReturn(expected);

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isSameAs(expected);
        verify(similarityService).findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10);
    }

    @Test
    void shouldSkipCheckForExistingExtensionWhenConfiguredForNewOnly() {
        // When configured for new extensions only, skip if extension already has versions.
        when(config.isEnabled()).thenReturn(true);
        when(config.isNewExtensionsOnly()).thenReturn(true);
        when(repositories.countVersions("ns", "ext")).thenReturn(1);

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verify(repositories).countVersions("ns", "ext");
        verifyNoInteractions(similarityService);
    }

    @Test
    void shouldCheckNewExtensionEvenWhenConfiguredForNewOnly() {
        // When configured for new extensions only, still check if extension has no versions.
        when(config.isEnabled()).thenReturn(true);
        when(config.isNewExtensionsOnly()).thenReturn(true);
        when(config.getLevenshteinThreshold()).thenReturn(0.15);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(false);
        when(repositories.countVersions("ns", "ext")).thenReturn(0);
        when(similarityService.findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10))
                .thenReturn(List.of());

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verify(repositories).countVersions("ns", "ext");
        verify(similarityService).findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10);
    }

    @Test
    void shouldSkipCheckForVerifiedPublisherWhenConfigured() {
        // When configured to skip verified publishers, check if namespace has owner memberships.
        when(config.isEnabled()).thenReturn(true);
        when(config.isSkipVerifiedPublishers()).thenReturn(true);
        var namespace = new Namespace();
        when(repositories.findNamespace("ns")).thenReturn(namespace);
        when(repositories.hasMemberships(namespace, NamespaceMembership.ROLE_OWNER)).thenReturn(true);

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verify(repositories).findNamespace("ns");
        verify(repositories).hasMemberships(namespace, NamespaceMembership.ROLE_OWNER);
        verifyNoInteractions(similarityService);
    }

    @Test
    void shouldCheckVerifiedPublisherWhenSkipIsDisabled() {
        // When skip verified publishers is disabled, check even if namespace has owner memberships.
        when(config.isEnabled()).thenReturn(true);
        when(config.isSkipVerifiedPublishers()).thenReturn(false);
        when(config.getLevenshteinThreshold()).thenReturn(0.15);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(false);
        when(similarityService.findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10))
                .thenReturn(List.of());

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verify(similarityService).findSimilarExtensions("ext", "ns", "Display", List.of(), 0.15, false, 10);
    }

    @Test
    void shouldPassConfiguredThresholdAndVerifiedOnlyFlag() {
        // Verify that config values are correctly passed to SimilarityService.
        when(config.isEnabled()).thenReturn(true);
        when(config.isExcludeOwnerNamespaces()).thenReturn(false);
        when(config.getLevenshteinThreshold()).thenReturn(0.25);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(true);
        when(similarityService.findSimilarExtensions("ext", "ns", "Display", List.of(), 0.25, true, 10))
                .thenReturn(List.of());

        var result = similarityCheckService.findSimilarExtensionsForPublishing(
            "ext", "ns", "Display", user
        );

        assertThat(result).isEmpty();
        verify(similarityService).findSimilarExtensions("ext", "ns", "Display", List.of(), 0.25, true, 10);
    }

    @Test
    void shouldReturnEmptyNamespacesWhenDisabled() {
        // When disabled, namespace creation checks should not hit the database.
        when(config.isEnabled()).thenReturn(false);

        var result = similarityCheckService.findSimilarNamespacesForCreation("ns", user);

        assertThat(result).isEmpty();
        verifyNoInteractions(similarityService);
        verifyNoInteractions(repositories);
    }

    @Test
    void shouldDelegateSimilarNamespacesToServiceWhenEnabled() {
        // When enabled, delegate to SimilarityService with config parameters.
        when(config.isEnabled()).thenReturn(true);
        when(config.isExcludeOwnerNamespaces()).thenReturn(false);
        when(config.getLevenshteinThreshold()).thenReturn(0.15);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(false);
        var expected = List.of(new Namespace());
        when(similarityService.findSimilarNamespaces("ns", List.of(), 0.15, false, 10))
                .thenReturn(expected);

        var result = similarityCheckService.findSimilarNamespacesForCreation("ns", user);

        assertThat(result).isSameAs(expected);
        verify(similarityService).findSimilarNamespaces("ns", List.of(), 0.15, false, 10);
    }

    @Test
    void shouldExcludeOwnerNamespacesForNamespaceCreation() {
        // When exclude-owner-namespaces is enabled for namespace creation.
        when(config.isEnabled()).thenReturn(true);
        when(config.isExcludeOwnerNamespaces()).thenReturn(true);
        when(config.getLevenshteinThreshold()).thenReturn(0.2);
        when(config.isCheckAgainstVerifiedOnly()).thenReturn(true);

        var namespace1 = new Namespace();
        namespace1.setName("owned-ns");
        var membership1 = new NamespaceMembership();
        membership1.setNamespace(namespace1);
        membership1.setRole(NamespaceMembership.ROLE_OWNER);

        when(repositories.findMemberships(user)).thenReturn(Streamable.of(membership1));
        when(similarityService.findSimilarNamespaces("ns", List.of("owned-ns"), 0.2, true, 10))
                .thenReturn(List.of());

        var result = similarityCheckService.findSimilarNamespacesForCreation("ns", user);

        assertThat(result).isEmpty();
        verify(repositories).findMemberships(user);
        verify(similarityService).findSimilarNamespaces("ns", List.of("owned-ns"), 0.2, true, 10);
    }
}

