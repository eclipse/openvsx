/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import org.eclipse.openvsx.entities.Extension;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class LatestExtensionVersionCacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        var extension = (Extension) params[0];
        var targetPlatform = (String) params[1];
        var preRelease = params[2];
        var onlyActive = params[3];
        var extensionName = extension.getName();
        var namespaceName = extension.getNamespace().getName();
        return namespaceName + "." + extensionName + "-latest@" + targetPlatform +
                ",pre-release=" + preRelease + ",only-active=" + onlyActive;
    }
}
