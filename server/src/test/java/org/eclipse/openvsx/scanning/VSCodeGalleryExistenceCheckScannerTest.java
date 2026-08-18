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

import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import org.eclipse.openvsx.adapter.ExtensionQueryResult;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VSCodeGalleryExistenceCheckScannerTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private RepositoryService repositories;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ScannerRegistry scannerRegistry;

    /**
     * Dummy result; irrelevant of content
     */
    private final ExtensionQueryResult dummyResult = new ExtensionQueryResult(
            List.of(
                    new ExtensionQueryResult.ResultItem(
                            List.of(
                                    new ExtensionQueryResult.Extension(
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null)),
                            List.of())));

    /**
     * Dummy result; irrelevant of content
     */
    private final ExtensionQueryResult emptyResult = new ExtensionQueryResult(
            List.of(new ExtensionQueryResult.ResultItem(List.of(), List.of())));

    private VSCodeGalleryExistenceCheckScanner newScanner() {
        var config = new VSCodeGalleryExistenceCheckConfig(true, true, true, "http://irrelevant");
        return new VSCodeGalleryExistenceCheckScanner(
                config,
                restTemplate,
                repositories,
                entityManager,
                scannerRegistry);
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
    void startScan_isClean_whenUpstreamIsDown() {
        var extVersion = extensionVersion(new UserData());
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(restTemplate.postForObject(anyString(), any(), any())).thenThrow(new RestClientException("whatever"));

        assertThrows(ScannerException.class, () -> newScanner().startScan(new Scanner.Command(1L, "scan-1")));
    }

    @Test
    void startScan_isClean_whenExtensionDoesNotExistUpstream() throws Exception {
        var extVersion = extensionVersion(new UserData());
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(emptyResult);

        var invocation = (Scanner.Invocation.Completed) newScanner().startScan(new Scanner.Command(1L, "scan-1"));

        assertTrue(invocation.result().isClean());
        verify(repositories, never()).isVerified(any(), any());
    }

    @Test
    void startScan_raisesThreat_whenExistsUpstreamAndNamespaceIsNotVerified() throws Exception {
        var user = new UserData();
        var extVersion = extensionVersion(user);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(dummyResult);
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
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(dummyResult);
        when(repositories.isVerified(extVersion.getExtension().getNamespace(), user)).thenReturn(true);

        var invocation = (Scanner.Invocation.Completed) newScanner().startScan(new Scanner.Command(1L, "scan-1"));

        assertTrue(invocation.result().isClean());
        assertEquals(0, invocation.result().getThreats().size());
    }

    @Test
    void startScan_raisesThreat_whenExistsUpstreamAndNoPublishingUserIsAttributed() throws Exception {
        var extVersion = extensionVersion(null);
        when(entityManager.find(ExtensionVersion.class, 1L)).thenReturn(extVersion);
        when(restTemplate.postForObject(anyString(), any(), any())).thenReturn(dummyResult);

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
