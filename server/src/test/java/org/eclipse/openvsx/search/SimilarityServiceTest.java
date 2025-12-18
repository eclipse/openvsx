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
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SimilarityServiceTest {

    @Mock
    SimilarityConfig similarityConfig;

    @Mock
    RepositoryService repositories;

    @InjectMocks
    SimilarityService similarityService;

    @BeforeEach
    void setUp() {
        // Ensure sensible defaults so tests only change what they need.
        // We keep these stubs lenient because some tests short-circuit before reading them.
        lenient().when(similarityConfig.isEnabled()).thenReturn(true);
        lenient().when(similarityConfig.isNewExtensionsOnly()).thenReturn(false);
        lenient().when(similarityConfig.isSkipVerifiedPublishers()).thenReturn(false);
        lenient().when(similarityConfig.isCheckAgainstVerifiedOnly()).thenReturn(false);
        lenient().when(similarityConfig.getLevenshteinThreshold()).thenReturn(0.15);
    }

    @Test
    void shouldReturnEmptyWhenSimilarityChecksDisabled() {
        // Disable the feature and ensure we do not hit the database.
        when(similarityConfig.isEnabled()).thenReturn(false);

        var result = similarityService.findSimilarExtensions("ext", "ns", "Display", List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repositories);
    }

    @Test
    void shouldSkipVerifiedPublisherNamespaceWhenConfigured() {
        // If publisher is verified and skipping is enabled, nothing should be checked.
        when(similarityConfig.isSkipVerifiedPublishers()).thenReturn(true);
        var namespace = new Namespace();
        when(repositories.findNamespace("ns")).thenReturn(namespace);
        when(repositories.hasMemberships(namespace, NamespaceMembership.ROLE_OWNER)).thenReturn(true);

        var result = similarityService.findSimilarExtensions("ext", "ns", "Display", List.of());

        assertThat(result).isEmpty();
        verify(repositories).findNamespace("ns");
        verify(repositories, never()).findSimilarExtensionsByLevenshtein(any(), any(), any(), any(), anyDouble(), anyBoolean(), anyInt());
    }

    @Test
    void shouldDelegateToRepositoryWithConfigValues() {
        // Happy path delegates to repository with the configured flags.
        when(similarityConfig.isCheckAgainstVerifiedOnly()).thenReturn(true);
        when(similarityConfig.getLevenshteinThreshold()).thenReturn(0.2);
        var expected = List.of(new Extension());
        when(repositories.findSimilarExtensionsByLevenshtein("ext", "ns", "Display", List.of(), 0.2, true, 10))
                .thenReturn(expected);

        var result = similarityService.findSimilarExtensions("ext", "ns", "Display", List.of());

        assertThat(result).isSameAs(expected);
        verify(repositories).findSimilarExtensionsByLevenshtein("ext", "ns", "Display", List.of(), 0.2, true, 10);
    }

    @Test
    void shouldSkipSimilarityWhenConfiguredForNewExtensionsOnlyAndExtensionAlreadyExists() {
        // When configured for new extensions only, follow-up releases should skip similarity checks.
        // We identify "existing" by counting versions for the namespace + extension.
        when(similarityConfig.isNewExtensionsOnly()).thenReturn(true);
        when(repositories.countVersions("ns", "ext")).thenReturn(1);

        var result = similarityService.findSimilarExtensions("ext", "ns", "Display", List.of());

        assertThat(result).isEmpty();
        verify(repositories).countVersions("ns", "ext");
        verify(repositories, never()).findSimilarExtensionsByLevenshtein(any(), any(), any(), any(), anyDouble(), anyBoolean(), anyInt());
    }

    @Test
    void shouldHandleNullExtensionVersionGracefully() {
        // Null extension version should return an empty list without hitting the repo.
        var result = similarityService.findSimilarExtensions((ExtensionVersion) null);

        assertThat(result).isEmpty();
        verifyNoInteractions(repositories);
    }

    @Test
    void shouldExcludeOwnNamespaceWhenCheckingFromExtensionVersion() {
        // Extension version path must exclude its own namespace from comparisons.
        var namespace = new Namespace();
        namespace.setName("self");
        var extension = new Extension();
        extension.setName("ext");
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setDisplayName("Display");
        extVersion.setExtension(extension);

        when(repositories.findSimilarExtensionsByLevenshtein("ext", "self", "Display", List.of("self"), 0.15, false, 10))
                .thenReturn(List.of(new Extension()));

        var result = similarityService.findSimilarExtensions(extVersion);

        assertThat(result).hasSize(1);
        verify(repositories).findSimilarExtensionsByLevenshtein("ext", "self", "Display", List.of("self"), 0.15, false, 10);
    }

    @Test
    void shouldWrapRepositoryErrorsWhenCheckingExtensions() {
        // Repository failures should be wrapped with a user-friendly runtime error.
        when(repositories.findSimilarExtensionsByLevenshtein(any(), any(), any(), any(), anyDouble(), anyBoolean(), anyInt()))
                .thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> similarityService.findSimilarExtensions("ext", "ns", "Display", List.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to verify extension name uniqueness");
    }

    @Test
    void shouldReturnEmptyNamespacesWhenDisabled() {
        // Namespace similarity should bail out when disabled.
        when(similarityConfig.isEnabled()).thenReturn(false);

        var result = similarityService.findSimilarNamespaces("ns", List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(repositories);
    }

    @Test
    void shouldDelegateNamespaceSimilarityWithConfigFlags() {
        // Happy path delegates namespace similarity search with correct flags.
        when(similarityConfig.isCheckAgainstVerifiedOnly()).thenReturn(true);
        when(similarityConfig.getLevenshteinThreshold()).thenReturn(0.1);
        var existing = new Namespace();
        when(repositories.findSimilarNamespacesByLevenshtein("ns", List.of("skip"), 0.1, true, 10))
                .thenReturn(List.of(existing));

        var result = similarityService.findSimilarNamespaces("ns", List.of("skip"));

        assertThat(result).containsExactly(existing);
        verify(repositories).findSimilarNamespacesByLevenshtein("ns", List.of("skip"), 0.1, true, 10);
    }

    @Test
    void shouldIgnoreEmptyNamespaceInput() {
        // Empty namespace names should not trigger database work.
        var resultEmpty = similarityService.findSimilarNamespaces("", List.of());
        var resultNull = similarityService.findSimilarNamespaces(null, List.of());

        assertThat(resultEmpty).isEmpty();
        assertThat(resultNull).isEmpty();
        verifyNoInteractions(repositories);
    }
}

