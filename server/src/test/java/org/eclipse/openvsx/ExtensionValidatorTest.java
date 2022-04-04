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
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("latest");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("The version string 'latest' is reserved."));
    }

    @Test
    public void testInvalidVersion2() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1/2");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Invalid character '/' found in version (index 1)."));
    }

    @Test
    public void testInvalidTargetPlatform() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform("debian-x64");
        extension.setVersion("1");
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
        extension.setVersion("1");
        extension.setRepository("Foo and bar!");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0))
                .isEqualTo(new ExtensionValidator.Issue("Invalid URL in field 'repository': Foo and bar!"));
    }

    @Test
    public void testGitProtocol() {
        var validator = new ExtensionValidator();
        var extension = new ExtensionVersion();
        extension.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extension.setVersion("1");
        extension.setRepository("git+https://github.com/Foo/Bar.git");
        var issues = validator.validateMetadata(extension);
        assertThat(issues).isEmpty();
    }
    
}