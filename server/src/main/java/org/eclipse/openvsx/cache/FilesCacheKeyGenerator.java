/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import org.eclipse.openvsx.adapter.WebResourceService;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.storage.IStorageService;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class FilesCacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        if(target instanceof WebResourceService) {
            var namespace = (String) params[0];
            var extension = (String) params[1];
            var targetPlatform = (String) params[2];
            var version = (String) params[3];
            var name = (String) params[4];
            return generate(namespace, extension, targetPlatform, version, name);
        }
        if(target instanceof IStorageService) {
            return generate((FileResource) params[0]);
        }

        throw new UnsupportedOperationException();
    }

    public String generate(FileResource resource) {
        var extVersion = resource.getExtension();
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        return generate(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion(), resource.getName());
    }

    public String generate(String namespace, String extension, String targetPlatform, String version, String name) {
        return UrlUtil.createApiFileUrl("", namespace, extension, targetPlatform, version, name);
    }
}
