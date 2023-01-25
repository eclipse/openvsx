/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Aspect
@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
public class RepositoryServiceAspect {

    @Autowired
    StorageUtilService storageUtil;

    @Around("execution(* org.eclipse.openvsx.repositories.RepositoryService.findFileByTypeAndName(..))")
    public Object findFileByTypeAndName(ProceedingJoinPoint joinPoint) throws Throwable {
        var args = joinPoint.getArgs();
        var resource =  new FileResource();
        resource.setExtension((ExtensionVersion) args[0]);
        resource.setName((String) args[2]);
        resource.setType((String) args[1]);
        resource.setStorageType(storageUtil.getActiveStorageType());
        return resource;
    }
}
