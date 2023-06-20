/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
public class VersionServiceTest {

    @MockBean
    EntityManager entityManager;

    @Autowired
    VersionService versions;

    @Test
    public void testGetVersions() {
        var release = new ExtensionVersion();
        release.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        release.setVersion("1.0.0");

        var minor = new ExtensionVersion();
        minor.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        minor.setVersion("0.0.5");

        var major = new ExtensionVersion();
        major.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        major.setVersion("0.3.0");

        var versionList = List.of(major, minor, release);
        var extension = new Extension();
        extension.getVersions().addAll(versionList);

        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        var actualVersionList = versions.getVersionsTrxn(extension);
        assertEquals(versionList, actualVersionList);
    }

    @Test
    public void testGetLatestVersion() {
        var release = new ExtensionVersion();
        release.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        release.setVersion("1.0.0");

        var minor = new ExtensionVersion();
        minor.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        minor.setVersion("0.0.5");

        var major = new ExtensionVersion();
        major.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        major.setVersion("0.3.0");

        var extension = new Extension();
        extension.getVersions().addAll(List.of(major, minor, release));
        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        var latest = versions.getLatest(extension, null, false, false);
        assertEquals(release, latest);
    }

    @Test
    public void testGetLatestTargetPlatformSort() {
        var version = "1.0.0";
        var universal = new ExtensionVersion();
        universal.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        universal.setVersion(version);

        var linux = new ExtensionVersion();
        linux.setTargetPlatform(TargetPlatform.NAME_LINUX_X64);
        linux.setVersion(version);

        var windows = new ExtensionVersion();
        windows.setTargetPlatform(TargetPlatform.NAME_WIN32_ARM64);
        windows.setVersion(version);

        var extension = new Extension();
        extension.getVersions().addAll(List.of(windows, linux, universal));
        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        var latest = versions.getLatest(extension, null, false, false);
        assertEquals(universal, latest);
    }

    @Test
    public void testGetLatestActive() {
        var release = new ExtensionVersion();
        release.setActive(false);
        release.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        release.setVersion("1.0.0");

        var minor = new ExtensionVersion();
        minor.setActive(true);
        minor.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        minor.setVersion("0.0.5");

        var major = new ExtensionVersion();
        major.setActive(false);
        major.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        major.setVersion("0.3.0");

        var extension = new Extension();
        extension.getVersions().addAll(List.of(major, minor, release));
        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        var latest = versions.getLatest(extension, null, false, true);
        assertEquals(minor, latest);
    }

    @Test
    public void testGetLatestByTargetPlatform() {
        var release = new ExtensionVersion();
        release.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        release.setVersion("1.0.0");

        var minor = new ExtensionVersion();
        minor.setTargetPlatform(TargetPlatform.NAME_LINUX_ARM64);
        minor.setVersion("0.0.5");

        var major = new ExtensionVersion();
        major.setTargetPlatform(TargetPlatform.NAME_LINUX_ARM64);
        major.setVersion("0.3.0");

        var extension = new Extension();
        extension.getVersions().addAll(List.of(major, minor, release));
        Mockito.when(entityManager.merge(extension)).thenReturn(extension);
        var latest = versions.getLatest(extension, TargetPlatform.NAME_LINUX_ARM64, false, false);
        assertEquals(major, latest);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        VersionService versionService() {
            return new VersionService();
        }
    }
}
