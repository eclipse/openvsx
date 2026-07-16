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

import org.junit.jupiter.api.Test;

import java.util.List;

class CollectionUtilTest {

    @Test
    void testLimit() {
        var source = List.of(1, 2, 3, 4, 5);
        var result = CollectionUtil.limit(source, 3);
        assertThat(result).isEqualTo(List.of(1, 2, 3));
    }
}
