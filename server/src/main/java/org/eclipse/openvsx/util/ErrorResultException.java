/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

/**
 * Throw this exception to reply with a JSON object of the form
 * <pre>
 * { "error": "«message»" }
 * </pre
 */
public class ErrorResultException extends RuntimeException {

    private static final long serialVersionUID = 147466147310091931L;

    public ErrorResultException(String message) {
        super(message);
    }

    public ErrorResultException(String message, Throwable cause) {
        super(message, cause);
    }

}