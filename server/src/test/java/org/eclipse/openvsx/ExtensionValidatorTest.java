/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.util.TargetPlatform;
import org.junit.jupiter.api.Test;

public class ExtensionValidatorTest {

    @Test
    public void testInvalidVersion1() {
        var validator = new ExtensionValidator();
        var issue = validator.validateExtensionVersion("latest");
        assertThat(issue).isPresent();
        assertThat(issue.get())
                .isEqualTo(new ExtensionValidator.Issue("The version string 'latest' is reserved."));
    }

    @Test
    public void testInvalidVersion2() {
        var validator = new ExtensionValidator();
        var issue = validator.validateExtensionVersion("1/2");
        assertThat(issue).isPresent();
        assertThat(issue.get())
                .isEqualTo(new ExtensionValidator.Issue("Invalid semantic version. See https://semver.org/."));
    }

    @Test
    public void testInvalidTargetPlatform() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform("debian-x64");
        extension.setVersion("1.0.0");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Unsupported target platform 'debian-x64'"));
    }

    @Test
    public void testInvalidURL() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1.0.0");
        extension.setRepository("Foo and bar!");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Invalid URL in field 'repository': Foo and bar!"));
    }

    @Test
    public void testInvalidURL2() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1.0.0");
        extension.setRepository("https://");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Invalid URL in field 'repository': https://"));
    }

    @Test
    public void testInvalidURL3() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1.0.0");
        extension.setRepository("http://");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Invalid URL in field 'repository': http://"));
    }

    @Test
    public void testMailtoURL() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1.0.0");
        extension.setRepository("mailto:foo@bar.net");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).isEmpty();
    }

    @Test
    public void testGitProtocol() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1.0.0");
        extension.setRepository("git+https://github.com/Foo/Bar.git");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).isEmpty();
    }
    
}