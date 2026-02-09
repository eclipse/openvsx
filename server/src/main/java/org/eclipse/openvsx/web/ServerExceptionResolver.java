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
package org.eclipse.openvsx.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.annotation.Nonnull;

@RestControllerAdvice
public class ServerExceptionResolver extends DefaultHandlerExceptionResolver {

    public ServerExceptionResolver() {
        super();
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected void logException(@Nonnull Exception ex, @Nonnull HttpServletRequest request) {
        // do not log HttpMediaTypeNotSupportedException, see https://github.com/eclipse/openvsx/issues/1505
        // this just pollutes the server logs but bringing no added value
        if (!(ex instanceof HttpMediaTypeNotSupportedException)) {
            super.logException(ex, request);
        }
    }
}
