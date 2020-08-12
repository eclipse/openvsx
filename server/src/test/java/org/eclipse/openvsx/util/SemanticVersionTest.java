/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class SemanticVersionTest {

    @Test
    public void testCompare() {
        assertThat(new SemanticVersion("2.0.0").compareTo(new SemanticVersion("1.2.3")))
                .isEqualTo(1);
        assertThat(new SemanticVersion("1.2.3").compareTo(new SemanticVersion("1.2.4")))
                .isEqualTo(-1);
        assertThat(new SemanticVersion("1.2.3-next.bc11e2c5").compareTo(new SemanticVersion("1.2.3-next.6aa3b0d6")))
                .isEqualTo(0);
        assertThat(new SemanticVersion("1.2.3").compareTo(new SemanticVersion("1.2.3-next.bc11e2c5")))
                .isEqualTo(1);
        assertThat(new SemanticVersion("10.0").compareTo(new SemanticVersion("9.0")))
                .isEqualTo(1);
    }

}