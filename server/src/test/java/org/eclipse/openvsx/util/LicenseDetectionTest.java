/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import com.google.common.io.ByteStreams;

import org.junit.jupiter.api.Test;

public class LicenseDetectionTest {

    @Test
    public void testInvalid() throws Exception {
        var detection = new LicenseDetection();
        var result = detection.detectLicense("This is a funny license.");
        assertThat(result).isNull();;
    }

    @Test
    public void testMIT() throws Exception {
        var license = detect("MIT.txt");
        assertThat(license).isEqualTo("MIT");
    }

    @Test
    public void testGPL() throws Exception {
        var license = detect("GPL-3.0.txt");
        assertThat(license).isEqualTo("GPL-3.0");
    }

    @Test
    public void testAGPL() throws Exception {
        var license = detect("AGPL-3.0.txt");
        assertThat(license).isEqualTo("AGPL-3.0");
    }

    private String detect(String fileName) throws IOException {
        try (
            var stream = getClass().getResourceAsStream(fileName);
        ) {
            var bytes = ByteStreams.toByteArray(stream);
            var detection = new LicenseDetection();
            return detection.detectLicense(bytes);
        }
    }

}