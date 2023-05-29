package org.eclipse.openvsx.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class SemanticVersionTest {

    @Test
    public void testParseFullVersion() {
        var semver = SemanticVersion.parse("1.2.31-rc1+armhf");
        assertEquals(1, semver.getMajor());
        assertEquals(2, semver.getMinor());
        assertEquals(31, semver.getPatch());
        assertEquals("rc1", semver.getPreRelease());
        assertEquals("armhf", semver.getBuildMetadata());
    }

    @Test
    public void testParseNumbersVersion() {
        var semver = SemanticVersion.parse("1.2.3");
        assertEquals(1, semver.getMajor());
        assertEquals(2, semver.getMinor());
        assertEquals(3, semver.getPatch());
        assertNull(semver.getPreRelease());
        assertNull(semver.getBuildMetadata());
    }

    @Test
    public void testParseInvalidVersion() {
        var exception = assertThrows(RuntimeException.class, () -> SemanticVersion.parse("9.2"));
        assertEquals("Invalid semantic version. See https://semver.org/.", exception.getMessage());
    }

    @Test
    public void testCompare() {
        assertThat(SemanticVersion.parse("2.0.0").compareTo(SemanticVersion.parse("1.2.3")))
                .isEqualTo(1);
        assertThat(SemanticVersion.parse("1.2.3").compareTo(SemanticVersion.parse("1.2.4")))
                .isEqualTo(-1);
        assertThat(SemanticVersion.parse("1.2.3-next.bc11e2c5").compareTo(SemanticVersion.parse("1.2.3-next.6aa3b0d6")))
                .isEqualTo(0);
        assertThat(SemanticVersion.parse("1.2.3").compareTo(SemanticVersion.parse("1.2.3-next.bc11e2c5")))
                .isEqualTo(1);
    }
}
