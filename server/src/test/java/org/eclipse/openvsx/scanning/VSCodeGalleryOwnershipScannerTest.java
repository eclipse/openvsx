/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.scanning;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.openvsx.adapter.PublicIds;
import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VSCodeGalleryOwnershipScannerTest {

    @Mock
    private VSCodeIdService vsCodeIdService;
    @Mock
    private RepositoryService repositories;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ScannerRegistry scannerRegistry;

    private VSCodeGalleryOwnershipScanner newScanner() {
        return new VSCodeGalleryOwnershipScanner(vsCodeIdService, repositories, entityManager, scannerRegistry);
    }

    private ExtensionVersion extensionVersion(UserData publisher) {
        var namespace = new Namespace();
        namespace.setName("acme");

        var extension = new Extension();
        extension.setName("widget");
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setExtension(extension);
        if (publisher != null) {
            var token = new PersonalAccessToken();
            token.setUser(publisher);
            extVersion.setPublishedWith(token);
        }
        return extVersion;
    }

    @Test
    void startScan_isClean_whenExtensionDoesNotExistUpstream() throws Exception {
        var extVersion = extensionVersion(new UserData());
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(vsCodeIdService.getUpstreamPublicIds(any())).thenReturn(new PublicIds(null, null));

        var invocation = (Scanner.Invocation.Completed) newScanner().startScan(new Scanner.Command(1L, "scan-1"));

        assertTrue(invocation.result().isClean());
        verify(repositories, never()).isVerified(any(), any());
    }

    @Test
    void startScan_raisesThreat_whenExistsUpstreamAndNamespaceIsNotVerified() throws Exception {
        var user = new UserData();
        var extVersion = extensionVersion(user);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(vsCodeIdService.getUpstreamPublicIds(any())).thenReturn(new PublicIds("acme-pub-id", "widget-pub-id"));
        when(repositories.isVerified(extVersion.getExtension().getNamespace(), user)).thenReturn(false);

        var invocation = (Scanner.Invocation.Completed) newScanner().startScan(new Scanner.Command(1L, "scan-1"));

        assertFalse(invocation.result().isClean());
        assertEquals(1, invocation.result().getThreats().size());
    }

    @Test
    void startScan_raisesThreat_whenExistsUpstreamAndNamespaceIsVerified() throws Exception {
        var user = new UserData();
        var extVersion = extensionVersion(user);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(vsCodeIdService.getUpstreamPublicIds(any())).thenReturn(new PublicIds("acme-pub-id", "widget-pub-id"));
        when(repositories.isVerified(extVersion.getExtension().getNamespace(), user)).thenReturn(true);

        var invocation = (Scanner.Invocation.Completed) newScanner().startScan(new Scanner.Command(1L, "scan-1"));

        assertTrue(invocation.result().isClean());
        assertEquals(0, invocation.result().getThreats().size());
    }

    @Test
    void startScan_raisesThreat_whenExistsUpstreamAndNoPublishingUserIsAttributed() throws Exception {
        var extVersion = extensionVersion(null);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(vsCodeIdService.getUpstreamPublicIds(any())).thenReturn(new PublicIds("acme-pub-id", "widget-pub-id"));

        var invocation = (Scanner.Invocation.Completed) newScanner().startScan(new Scanner.Command(1L, "scan-1"));

        assertFalse(invocation.result().isClean());
        verify(repositories, never()).isVerified(any(), any());
    }

    @Test
    void startScan_throws_whenExtensionVersionNotFound() {
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(null);

        assertThrows(ScannerException.class, () -> newScanner().startScan(new Scanner.Command(1L, "scan-1")));
    }
}
