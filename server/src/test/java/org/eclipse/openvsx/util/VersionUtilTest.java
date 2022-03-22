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
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VersionUtilTest {

    @AfterEach
    public void afterEach() {
        VersionUtil.clearCache();
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

        var latest = VersionUtil.getLatest(List.of(major, minor, release), Collections.emptyList());
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

        var latest = VersionUtil.getLatest(List.of(windows, linux, universal), Collections.emptyList());
        assertEquals(universal, latest);
    }

    @Test
    public void testGetLatestActivePredicate() {
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

        List<Predicate<ExtensionVersion>> predicates = List.of(ExtensionVersion::isActive);
        var latest = VersionUtil.getLatest(List.of(major, minor, release), predicates);
        assertEquals(minor, latest);
    }

    @Test
    public void testGetLatestTargetPlatformPredicate() {
        var release = new ExtensionVersion();
        release.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        release.setVersion("1.0.0");

        var minor = new ExtensionVersion();
        minor.setTargetPlatform(TargetPlatform.NAME_LINUX_ARM64);
        minor.setVersion("0.0.5");

        var major = new ExtensionVersion();
        major.setTargetPlatform(TargetPlatform.NAME_LINUX_ARM64);
        major.setVersion("0.3.0");

        List<Predicate<ExtensionVersion>> predicates = List.of(ev -> ev.getTargetPlatform().equals(TargetPlatform.NAME_LINUX_ARM64));
        var latest = VersionUtil.getLatest(List.of(major, minor, release), predicates);
        assertEquals(major, latest);
    }

    @Test
    public void testGetLatestDTOVersion() {
        var release = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, "1.0.0");
        var minor = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, "0.0.5");
        var major = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, "0.3.0");

        var latest = VersionUtil.getLatest(List.of(major, minor, release));
        assertEquals(release, latest);
    }

    @Test
    public void testGetLatestDTOTargetPlatformSort() {
        var version = "1.0.0";
        var universal = constructExtensionVersionDTO(TargetPlatform.NAME_UNIVERSAL, version);
        var linux = constructExtensionVersionDTO(TargetPlatform.NAME_LINUX_X64, version);
        var windows = constructExtensionVersionDTO(TargetPlatform.NAME_WIN32_ARM64, version);

        var latest = VersionUtil.getLatest(List.of(windows, linux, universal));
        assertEquals(universal, latest);
    }

    private ExtensionVersionDTO constructExtensionVersionDTO(String targetPlatform, String version) {
        return new ExtensionVersionDTO(1, 2, version, targetPlatform, false, false,
            null, null, null, null, null, null,null,
            null, null, null, null, null);
    }
}
