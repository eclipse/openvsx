/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.settings;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.eclipse.openvsx.json.ResultJson;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;

@Aspect
@Component
public class ReadOnlyEndpointAspect {

    private final SettingsService settings;

    public ReadOnlyEndpointAspect(SettingsService settings) {
        this.settings = settings;
    }

    @Around("(execution(org.springframework.http.ResponseEntity org.eclipse.openvsx.RegistryAPI.*(..)) ||" +
            " execution(org.springframework.http.ResponseEntity org.eclipse.openvsx.UserAPI.*(..))  ||" +
            " execution(org.springframework.http.ResponseEntity org.eclipse.openvsx.admin.*API.*(..))) && @annotation(MutatingOperation)")
    public Object handleMutatingEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        if (settings.isReadOnly()) {
            return ResponseEntity.status(409).body(ResultJson.error("Registry is in read-only mode."));
        } else {
            return joinPoint.proceed();
        }
    }
}
