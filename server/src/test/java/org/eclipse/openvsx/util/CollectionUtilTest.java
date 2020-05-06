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

import com.google.common.collect.Lists;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionUtilTest {

    @Test
    public void testLimit() throws Exception {
        var source = Lists.newArrayList(1, 2, 3, 4, 5);
        var result = CollectionUtil.limit(source, 3);
        assertEquals(Lists.newArrayList(1, 2, 3), result);
    }

}