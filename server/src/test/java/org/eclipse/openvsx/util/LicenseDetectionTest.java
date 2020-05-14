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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.io.ByteStreams;

public class LicenseDetectionTest {

    @Test
    public void testInvalid() throws Exception {
        var detection = new LicenseDetection();
        var result = detection.detectLicense("This is a funny license.");
        assertNull(result);
    }

    @Test
    public void testMIT() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("MIT.txt");
        ) {
            var bytes = ByteStreams.toByteArray(stream);
            var detection = new LicenseDetection();
            var result = detection.detectLicense(bytes);
            assertEquals("MIT", result);
        }
    }

}