/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.VersionAlias;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class ExtensionJsonCacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        var version = params.length == 4 ? (String) params[3] : VersionAlias.LATEST;
        return generate((String) params[0], (String) params[1], (String) params[2], version);
    }

    public String generate(String namespaceName, String extensionName, String targetPlatform, String version) {
        return NamingUtil.toFileFormat(namespaceName, extensionName, version, targetPlatform);
    }
}
