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

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static LocalDateTime getCurrentUTC() {
        return LocalDateTime.now(ZoneId.of("UTC"));
    }

    public static String toUTCString(LocalDateTime dateTime) {
        return dateTime.toString() + 'Z';
    }
    
}