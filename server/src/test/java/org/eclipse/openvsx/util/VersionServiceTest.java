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

import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionServiceTest {

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
        var latest = new VersionService().getLatest(extension, null, false, false);
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
        var latest = new VersionService().getLatest(extension, null, false, false);
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
        var latest = new VersionService().getLatest(extension, null, false, true);
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
        var latest = new VersionService().getLatest(extension, TargetPlatform.NAME_LINUX_ARM64, false, false);
        assertEquals(major, latest);
    }

    @Test
    public void testGetLatestDTOVersion() {
        var release = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, "1.0.0");
        var minor = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, "0.0.5");
        var major = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, "0.3.0");

        var latest = new VersionService().getLatest(List.of(major, minor, release), false);
        assertEquals(release, latest);
    }

    @Test
    public void testGetLatestDTOTargetPlatformSort() {
        var version = "1.0.0";
        var universal = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, version);
        var linux = constructExtensionVersionDTO(TargetPlatform.NAME_LINUX_X64, version);
        var windows = constructExtensionVersionDTO(TargetPlatform.NAME_WIN32_ARM64, version);

        var latest = new VersionService().getLatest(List.of(windows, linux, universal), false);
        assertEquals(universal, latest);
    }

    private ExtensionVersionDTO constructExtensionVersionDTO(String targetPlatform, String version) {
        return new ExtensionVersionDTO(1, 2, version, targetPlatform, false, false,
            null, null, null, null, null, null,null,
            null, null, null, null, null);
    }
}
