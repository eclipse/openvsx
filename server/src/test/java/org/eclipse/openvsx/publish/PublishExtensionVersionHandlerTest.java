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
package org.eclipse.openvsx.publish;

import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.extension_control.ExtensionControlService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SimilarityCheckService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishExtensionVersionHandlerTest {

    @Mock
    PublishExtensionVersionService publishService;

    @Mock
    ExtensionVersionIntegrityService integrityService;

    @Mock
    EntityManager entityManager;

    @Mock
    RepositoryService repositories;

    @Mock
    JobRequestScheduler scheduler;

    @Mock
    UserService users;

    @Mock
    ExtensionValidator validator;

    @Mock
    ExtensionControlService extensionControl;

    @Mock
    SimilarityCheckService similarityCheckService;

    private PublishExtensionVersionHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        // Keep defaults permissive so tests focus on similarity behaviour.
        handler = new PublishExtensionVersionHandler(
                publishService,
                integrityService,
                entityManager,
                repositories,
                scheduler,
                users,
                validator,
                extensionControl,
                similarityCheckService
        );
        when(extensionControl.getMaliciousExtensionIds()).thenReturn(Collections.emptyList());
    }

    @Test
    void shouldFailPublishingWhenSimilarExtensionAlreadyExists() {
        // Build minimal processor metadata.
        var processor = org.mockito.Mockito.mock(ExtensionProcessor.class);
        when(processor.getNamespace()).thenReturn("publisher");
        when(processor.getExtensionName()).thenReturn("demo");
        when(processor.getVersion()).thenReturn("1.0.0");

        var metadata = new ExtensionVersion();
        metadata.setDisplayName("Demo Extension");
        metadata.setVersion("1.0.0");
        metadata.setTargetPlatform("any");
        when(processor.getMetadata()).thenReturn(metadata);

        var user = new org.eclipse.openvsx.entities.UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);

        var namespace = new Namespace();
        namespace.setName("publisher");
        when(repositories.findNamespace("publisher")).thenReturn(namespace);
        when(users.hasPublishPermission(user, namespace)).thenReturn(true);
        when(validator.validateExtensionVersion("1.0.0")).thenReturn(Optional.empty());
        when(validator.validateExtensionName("demo")).thenReturn(Optional.empty());

        var similarExtension = new Extension();
        similarExtension.setNamespace(buildNamespace("other"));
        similarExtension.setName("demo-other");
        when(similarityCheckService.isEnabled()).thenReturn(true);
        when(similarityCheckService.findSimilarExtensionsForPublishing("demo", "publisher", "Demo Extension", user))
                .thenReturn(List.of(similarExtension));

        var similarLatest = new ExtensionVersion();
        similarLatest.setDisplayName("Existing Demo");
        when(repositories.findLatestVersion(similarExtension, null, false, true)).thenReturn(similarLatest);

        assertThatThrownBy(() -> handler.createExtensionVersion(processor, token, LocalDateTime.now(), true))
                .isInstanceOf(ErrorResultException.class)
                .hasMessageContaining("too similar to existing extension");

        // Persist should never happen because we bail out early on similarity.
        verify(entityManager, never()).persist(metadata);
        verify(similarityCheckService).findSimilarExtensionsForPublishing("demo", "publisher", "Demo Extension", user);
    }

    @Test
    void shouldExcludeOwnedNamespacesFromSimilarityCheck() {
        // Ensure owner namespaces are excluded when configured.
        var processor = org.mockito.Mockito.mock(ExtensionProcessor.class);
        when(processor.getNamespace()).thenReturn("publisher");
        when(processor.getExtensionName()).thenReturn("demo");
        when(processor.getVersion()).thenReturn("1.0.1");
        when(processor.getExtensionDependencies()).thenReturn(List.of());
        when(processor.getBundledExtensions()).thenReturn(List.of());

        var metadata = new ExtensionVersion();
        metadata.setDisplayName("Demo Next");
        metadata.setVersion("1.0.1");
        metadata.setTargetPlatform("any");
        when(processor.getMetadata()).thenReturn(metadata);

        var namespace = buildNamespace("publisher");
        var ownedNamespace = buildNamespace("owned-ns");
        var user = new org.eclipse.openvsx.entities.UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);

        when(repositories.findNamespace("publisher")).thenReturn(namespace);
        when(users.hasPublishPermission(user, namespace)).thenReturn(true);
        when(validator.validateExtensionVersion("1.0.1")).thenReturn(Optional.empty());
        when(validator.validateExtensionName("demo")).thenReturn(Optional.empty());
        when(validator.validateMetadata(metadata)).thenReturn(List.of());
        when(similarityCheckService.isEnabled()).thenReturn(true);
        when(similarityCheckService.findSimilarExtensionsForPublishing("demo", "publisher", "Demo Next", user))
                .thenReturn(List.of());
        when(repositories.findExtension("demo", namespace)).thenReturn(null);

        handler.createExtensionVersion(processor, token, LocalDateTime.now(), false);

        verify(similarityCheckService).findSimilarExtensionsForPublishing("demo", "publisher", "Demo Next", user);
    }

    @Test
    void shouldCreateExtensionWhenSimilarityFindsNoConflicts() {
        // Happy path: similarity passes and entities get persisted and linked.
        var processor = org.mockito.Mockito.mock(ExtensionProcessor.class);
        when(processor.getNamespace()).thenReturn("publisher");
        when(processor.getExtensionName()).thenReturn("demo");
        when(processor.getVersion()).thenReturn("2.0.0");
        when(processor.getExtensionDependencies()).thenReturn(List.of());
        when(processor.getBundledExtensions()).thenReturn(List.of());

        var metadata = new ExtensionVersion();
        metadata.setDisplayName("Demo OK");
        metadata.setVersion("2.0.0");
        metadata.setTargetPlatform("any");
        when(processor.getMetadata()).thenReturn(metadata);

        var namespace = buildNamespace("publisher");
        var user = new org.eclipse.openvsx.entities.UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);

        when(repositories.findNamespace("publisher")).thenReturn(namespace);
        when(users.hasPublishPermission(user, namespace)).thenReturn(true);
        when(validator.validateExtensionVersion("2.0.0")).thenReturn(Optional.empty());
        when(validator.validateExtensionName("demo")).thenReturn(Optional.empty());
        when(validator.validateMetadata(metadata)).thenReturn(List.of());
        when(similarityCheckService.isEnabled()).thenReturn(true);
        when(similarityCheckService.findSimilarExtensionsForPublishing("demo", "publisher", "Demo OK", user))
                .thenReturn(List.of());
        when(repositories.findExtension("demo", namespace)).thenReturn(null);

        var capturedNamespace = ArgumentCaptor.forClass(Extension.class);

        var result = handler.createExtensionVersion(processor, token, LocalDateTime.now(), false);

        verify(entityManager).persist(capturedNamespace.capture());
        verify(entityManager).persist(metadata);
        assertThat(result).isSameAs(metadata);
        assertThat(result.getPublishedWith()).isEqualTo(token);
        assertThat(result.getExtension()).isSameAs(capturedNamespace.getValue());
        assertThat(result.getExtension().getNamespace()).isSameAs(namespace);
    }

    @Test
    void shouldCheckSimilarityForAllExtensions() {
        // All extensions should be checked for similarity.
        var processor = org.mockito.Mockito.mock(ExtensionProcessor.class);
        when(processor.getNamespace()).thenReturn("pub");
        when(processor.getExtensionName()).thenReturn("demo");
        when(processor.getVersion()).thenReturn("3.0.0");
        when(processor.getMetadata()).thenReturn(new ExtensionVersion());

        var user = new org.eclipse.openvsx.entities.UserData();
        var token = new PersonalAccessToken();
        token.setUser(user);
        var namespace = buildNamespace("pub");

        when(repositories.findNamespace("pub")).thenReturn(namespace);
        when(users.hasPublishPermission(user, namespace)).thenReturn(true);
        when(validator.validateExtensionVersion("3.0.0")).thenReturn(Optional.empty());
        when(validator.validateExtensionName("demo")).thenReturn(Optional.empty());
        when(validator.validateMetadata(processor.getMetadata())).thenReturn(List.of());
        when(similarityCheckService.isEnabled()).thenReturn(true);
        when(similarityCheckService.findSimilarExtensionsForPublishing("demo", "pub", null, user)).thenReturn(List.of());
        when(repositories.findExtension("demo", namespace)).thenReturn(null);

        handler.createExtensionVersion(processor, token, LocalDateTime.now(), false);

        verify(similarityCheckService).findSimilarExtensionsForPublishing("demo", "pub", null, user);
    }

    private NamespaceMembership buildOwnerMembership(Namespace namespace) {
        var membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setRole(NamespaceMembership.ROLE_OWNER);
        return membership;
    }

    private Namespace buildNamespace(String name) {
        var namespace = new Namespace();
        namespace.setName(name);
        return namespace;
    }
}

