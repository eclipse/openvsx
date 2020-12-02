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

import org.eclipse.openvsx.json.ResultJson;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Throw this exception to reply with a JSON object of the form
 * 
 * <pre>
 * { "error": "«message»" }
 * </pre>
 * 
 * The message will likely be shown to the user, so it should be chosen with care.
 */
public class ErrorResultException extends RuntimeException {

    private static final long serialVersionUID = 147466147310091931L;

    private final HttpStatus status;

    public ErrorResultException(String message) {
        super(message);
        this.status = null;
    }

    public ErrorResultException(String message, Throwable cause) {
        super(message, cause);
        this.status = null;
    }

    public ErrorResultException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ResponseEntity<ResultJson> toResponseEntity() {
        var json = ResultJson.error(getMessage());
        var responseStatus = status != null ? status : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(json, responseStatus);
    }

    public <T extends ResultJson> ResponseEntity<T> toResponseEntity(Class<T> resultType) {
        try {
            var json = resultType.getDeclaredConstructor().newInstance();
            json.error = getMessage();
            var responseStatus = status != null ? status : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(json, responseStatus);
        } catch (ReflectiveOperationException exc) {
            throw new RuntimeException(exc);
        }
    }

}