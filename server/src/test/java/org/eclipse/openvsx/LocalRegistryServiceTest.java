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
package org.eclipse.openvsx;

import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.search.SimilarityCheckService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalRegistryServiceTest {

    @Mock
    EntityManager entityManager;

    @Mock
    RepositoryService repositories;

    @Mock
    ExtensionService extensions;

    @Mock
    VersionService versions;

    @Mock
    UserService users;

    @Mock
    SearchUtilService searchUtilService;

    @Mock
    ExtensionValidator validator;

    @Mock
    StorageUtilService storageUtilService;

    @Mock
    EclipseService eclipse;

    @Mock
    CacheService cacheService;

    @Mock
    ExtensionVersionIntegrityService integrityService;

    @Mock
    SimilarityCheckService similarityCheckService;

    private LocalRegistryService registryService;

    @BeforeEach
    void setUp() {
        registryService = new LocalRegistryService(
                entityManager,
                repositories,
                extensions,
                versions,
                users,
                searchUtilService,
                validator,
                storageUtilService,
                eclipse,
                cacheService,
                integrityService,
                similarityCheckService
        );

        doNothing().when(eclipse).checkPublisherAgreement(any());
    }

    @Test
    void shouldRejectNamespaceWhenSimilarNameExists() {
        // Build request with a name that collides with an existing namespace.
        var json = new NamespaceJson();
        json.setName("new-space");
        var user = new UserData();

        when(validator.validateNamespace("new-space")).thenReturn(Optional.empty());
        when(repositories.findNamespaceName("new-space")).thenReturn(null);
        when(similarityCheckService.findSimilarNamespacesForCreation("new-space", user))
                .thenReturn(List.of(buildNamespace("new-space-1")));

        assertThatThrownBy(() -> registryService.createNamespace(json, user))
                .isInstanceOf(ErrorResultException.class)
                .hasMessageContaining("too similar to existing namespace");

        verify(entityManager, never()).persist(any(Namespace.class));
    }

    @Test
    void shouldRejectExistingNamespaceBeforeSimilarityCheck() {
        // If the namespace already exists, we should fail fast and avoid extra work.
        var json = new NamespaceJson();
        json.setName("duplicate");
        var user = new UserData();

        when(validator.validateNamespace("duplicate")).thenReturn(Optional.empty());
        when(repositories.findNamespaceName("duplicate")).thenReturn("duplicate");

        assertThatThrownBy(() -> registryService.createNamespace(json, user))
                .isInstanceOf(ErrorResultException.class)
                .hasMessageContaining("Namespace already exists: duplicate");

        // No persistence and no similarity checks should occur when we bail out early.
        verify(entityManager, never()).persist(any(Namespace.class));
        verify(similarityCheckService, never()).findSimilarNamespacesForCreation(any(), any());
    }

    @Test
    void shouldCreateNamespaceAndAssignContributorRole() {
        // Happy path: namespace is new and not similar, so we persist both entities.
        var json = new NamespaceJson();
        json.setName("clean-ns");
        var user = new UserData();

        when(validator.validateNamespace("clean-ns")).thenReturn(Optional.empty());
        when(repositories.findNamespaceName("clean-ns")).thenReturn(null);
        when(similarityCheckService.findSimilarNamespacesForCreation("clean-ns", user)).thenReturn(List.of());

        registryService.createNamespace(json, user);

        // Capture persisted entities to verify they are wired as expected.
        var namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
        var membershipCaptor = ArgumentCaptor.forClass(NamespaceMembership.class);

        verify(entityManager).persist(namespaceCaptor.capture());
        verify(entityManager).persist(membershipCaptor.capture());

        var persistedNamespace = namespaceCaptor.getValue();
        var persistedMembership = membershipCaptor.getValue();

        assertThat(persistedNamespace.getName()).isEqualTo("clean-ns");
        assertThat(persistedMembership.getNamespace()).isSameAs(persistedNamespace);
        assertThat(persistedMembership.getUser()).isSameAs(user);
        assertThat(persistedMembership.getRole()).isEqualTo(NamespaceMembership.ROLE_CONTRIBUTOR);
    }

    private Namespace buildNamespace(String name) {
        var namespace = new Namespace();
        namespace.setName(name);
        return namespace;
    }

    private NamespaceMembership buildMembership(UserData user, String namespaceName) {
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        return membership;
    }
}

