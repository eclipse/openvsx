/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

package org.eclipse.openvsx.storage;

import org.springframework.http.CacheControl;

import java.util.concurrent.TimeUnit;

class StorageUtil {

    private StorageUtil(){}

    static CacheControl getCacheControl(String fileName) {
        // Files are requested with a version string in the URL, so their content cannot change
        return CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic();
    }
}
