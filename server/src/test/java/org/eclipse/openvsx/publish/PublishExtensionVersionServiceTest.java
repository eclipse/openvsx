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
package org.eclipse.openvsx.publish;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.TargetPlatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PublishExtensionVersionService#activateExtension}, focusing on the soft-delete
 * guard: a removed version is an immutable tombstone and must never be reactivated, even though it is
 * inactive and could otherwise be a reactivation candidate. This can happen when a version is soft-deleted
 * while an asynchronous scan for it is still in flight and the scan later completes (or is allowed by an
 * admin) and tries to activate it.
 */
@ExtendWith(MockitoExtension.class)
class PublishExtensionVersionServiceTest {

    @Mock
    RepositoryService repositories;
    @Mock
    EntityManager entityManager;
    @Mock
    StorageUtilService storageUtil;
    @Mock
    ExtensionService extensions;

    private PublishExtensionVersionService svc;

    @BeforeEach
    void setUp() {
        svc = new PublishExtensionVersionService(repositories, entityManager, storageUtil);
    }

    @Test
    void activateExtension_activatesLiveVersion() {
        var extVersion = version(1L, false);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);

        svc.activateExtension(extVersion, extensions);

        assertThat(extVersion.isActive()).isTrue();
        verify(extensions).updateExtension(extVersion.getExtension());
    }

    @Test
    void activateExtension_refusesRemovedVersion() {
        var extVersion = version(1L, true);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);

        svc.activateExtension(extVersion, extensions);

        assertThat(extVersion.isActive())
                .as("a soft-deleted tombstone must never be reactivated")
                .isFalse();
        verify(extensions, never()).updateExtension(any());
    }

    @Test
    void activateExtension_refusesMissingVersion() {
        // The row was purged (hard-deleted) between fetch and activation.
        var extVersion = version(1L, false);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(null);

        svc.activateExtension(extVersion, extensions);

        assertThat(extVersion.isActive()).isFalse();
        verify(extensions, never()).updateExtension(any());
    }

    private ExtensionVersion version(long id, boolean removed) {
        var namespace = new Namespace();
        namespace.setName("redhat");

        var extension = new Extension();
        extension.setName("vscode-yaml");
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setId(id);
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setActive(false);
        extVersion.setRemoved(removed);
        extVersion.setExtension(extension);
        return extVersion;
    }
}
