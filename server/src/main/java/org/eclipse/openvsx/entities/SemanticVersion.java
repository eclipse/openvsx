/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import org.apache.commons.lang3.StringUtils;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Embeddable
public class SemanticVersion implements Comparable<SemanticVersion>, Serializable {

    // source: https://semver.org/, search for: https://regex101.com/r/vkijKf/1/
    public static final Pattern VERSION_PARSE_PATTERN = Pattern.compile("^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)(?:-(?<prerelease>(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?<buildmetadata>[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    // has been modified to only use non-capturing groups (?:.*), so that it can be used as a URI template regex
    public static final String VERSION_PATH_PARAM_REGEX = "(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)(?:-(?:(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+(?:[0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?";

    public static SemanticVersion parse(String version) {
        try {
            var matcher = VERSION_PARSE_PATTERN.matcher(version);
            matcher.find();

            var semver = new SemanticVersion();
            semver.setMajor(Integer.parseInt(matcher.group("major")));
            semver.setMinor(Integer.parseInt(matcher.group("minor")));
            semver.setPatch(Integer.parseInt(matcher.group("patch")));
            semver.setPreRelease(matcher.group("prerelease"));
            semver.setBuildMetadata(matcher.group("buildmetadata"));
            return semver;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new RuntimeException("Invalid semantic version. See https://semver.org/.");
        }
    }

    int major;
    int minor;
    int patch;
    String preRelease;
    boolean isPreRelease;
    String buildMetadata;

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getPatch() {
        return patch;
    }

    public void setPatch(int patch) {
        this.patch = patch;
    }

    public String getPreRelease() {
        return preRelease;
    }

    public void setPreRelease(String preRelease) {
        this.preRelease = preRelease;
        this.isPreRelease = !StringUtils.isEmpty(preRelease);
    }

    public boolean isIsPreRelease() {
        return isPreRelease;
    }

    public void setIsPreRelease(boolean isPreRelease) {
        // do nothing, property is derived from preRelease
    }

    public String getBuildMetadata() {
        return buildMetadata;
    }

    public void setBuildMetadata(String buildMetadata) {
        this.buildMetadata = buildMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SemanticVersion that = (SemanticVersion) o;
        return major == that.major
                && minor == that.minor
                && patch == that.patch
                && isPreRelease == that.isPreRelease
                && Objects.equals(preRelease, that.preRelease)
                && Objects.equals(buildMetadata, that.buildMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease, isPreRelease, buildMetadata);
    }

    @Override
    public int compareTo(SemanticVersion that) {
        List<Supplier<Integer>> comparators = List.of(
                () -> Integer.compare(this.getMajor(), that.getMajor()),
                () -> Integer.compare(this.getMinor(), that.getMinor()),
                () -> Integer.compare(this.getPatch(), that.getPatch()),
                () -> -Boolean.compare(this.isIsPreRelease(), that.isIsPreRelease())
        );

        var compare = 0;
        for(var comparator : comparators) {
            compare = comparator.get();
            if(compare != 0) {
                return compare;
            }
        }

        return compare;
    }
}