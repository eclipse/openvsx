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
package org.eclipse.openvsx;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.publish.PublishExtensionVersionHandler;
import org.eclipse.openvsx.publish.PublishingConfig;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.scanning.ExtensionScanPersistenceService;
import org.eclipse.openvsx.scanning.ExtensionScanService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.LogService;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ExtensionServiceTest {

    @Mock
    PublishingConfig publishingConfig;
    @Mock
    EntityManager entityManager;
    @Mock
    RepositoryService repositories;
    @Mock
    SearchUtilService search;
    @Mock
    CacheService cache;
    @Mock
    LogService logs;
    @Mock
    PublishExtensionVersionHandler publishHandler;
    @Mock
    JobRequestScheduler scheduler;
    @Mock
    ExtensionScanService scanService;
    @Mock
    ExtensionScanPersistenceService scanPersistenceService;

    private ExtensionService svc;

    @BeforeEach
    void setUp() {
        svc = new ExtensionService(publishingConfig, entityManager, repositories, search, cache, logs, publishHandler,
                scheduler, scanService, scanPersistenceService);
    }

    @Test
    void shouldNotReactivateExtensionsWithErroredScans() {
        var user = mockUser();
        var ext = mockExtension();
        var extVersion = mockExtensionVersion(ext, "1.1.0", ScanStatus.ERRORED, user);
        ext.getVersions().add(extVersion);

        svc.reactivateExtensions(user);

        assertThat(ext.isActive()).isFalse();
        assertThat(extVersion.isActive()).isFalse();
    }

    @Test
    void shouldNotReactivateExtensionsWithQuarantinedScans() {
        var user = mockUser();
        var ext = mockExtension();
        var extVersion = mockExtensionVersion(ext, "1.1.0", ScanStatus.QUARANTINED, user);
        ext.getVersions().add(extVersion);

        svc.reactivateExtensions(user);

        assertThat(ext.isActive()).isFalse();
        assertThat(extVersion.isActive()).isFalse();
    }

    @Test
    void shouldReactivateExtensionsWithPassedScans() {
        var user = mockUser();
        var ext = mockExtension();
        var extVersion = mockExtensionVersion(ext, "1.1.0", ScanStatus.PASSED, user);
        ext.getVersions().add(extVersion);

        svc.reactivateExtensions(user);

        assertThat(ext.isActive()).isTrue();
        assertThat(extVersion.isActive()).isTrue();
    }

    @Test
    void shouldReactivateExtensionsWithQuarantinedScansAndAllowed() {
        var user = mockUser();
        var ext = mockExtension();
        var extVersion = mockExtensionVersion(ext, "1.1.0", ScanStatus.QUARANTINED, user);
        ext.getVersions().add(extVersion);

        var scan = repositories.findLatestExtensionScan(extVersion);
        var decision = new AdminScanDecision();
        decision.setDecision(AdminScanDecision.ALLOWED);

        Mockito.when(repositories.findAdminScanDecision(scan)).thenReturn(decision);

        svc.reactivateExtensions(user);

        assertThat(ext.isActive()).isTrue();
        assertThat(extVersion.isActive()).isTrue();
    }

    // ---------- UTILITY ----------//

    private Extension mockExtension() {
        var namespace = new Namespace();
        namespace.setId(2);
        namespace.setPublicId("test-2");
        namespace.setName("redhat");

        var extension = new Extension();
        extension.setId(1);
        extension.setPublicId("test-1");
        extension.setName("vscode-yaml");
        extension.setAverageRating(3.0);
        extension.setReviewCount(10L);
        extension.setDownloadCount(100);
        extension.setActive(false);
        extension.setPublishedDate(LocalDateTime.parse("1999-12-01T09:00"));
        extension.setLastUpdatedDate(LocalDateTime.parse("2000-01-01T10:00"));
        extension.setNamespace(namespace);

        return extension;
    }

    private ExtensionVersion mockExtensionVersion(
            Extension extension,
            String version,
            ScanStatus status,
            UserData user)
    {
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform("linux");
        extVersion.setPreview(true);
        extVersion.setActive(false);
        extVersion.setTimestamp(LocalDateTime.parse("2000-01-01T10:00"));
        extVersion.setDisplayName("YAML");
        extVersion.setDescription("YAML Language Support");
        extVersion.setEngines(List.of("vscode@^1.31.0"));
        extVersion.setRepository("https://github.com/redhat-developer/vscode-yaml");
        extVersion.setDependencies(Collections.emptyList());
        extVersion.setBundledExtensions(Collections.emptyList());
        extVersion.setLocalizedLanguages(Collections.emptyList());
        extVersion.setExtension(extension);

        var completedAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        var scan = new ExtensionScan();
        scan.setId(10L);
        scan.setStatus(status);
        scan.setCompletedAt(completedAt);
        scan.setExtensionVersion(version);

        Mockito.when(repositories.findLatestExtensionScan(extVersion)).thenReturn(scan);
        Mockito.when(repositories.findVersionsByUser(user, false)).thenReturn(Streamable.of(extVersion));

        return extVersion;
    }

    private UserData mockUser() {
        var user = new UserData();
        user.setLoginName("test");
        user.setProvider("github");
        user.setEclipseToken(new AuthToken("12345", null, null, null, null, null));
        return user;
    }
}
