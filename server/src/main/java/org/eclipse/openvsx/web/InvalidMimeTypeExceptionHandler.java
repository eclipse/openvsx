package org.eclipse.openvsx.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class InvalidMimeTypeExceptionHandler {

    @ExceptionHandler(value = InvalidMimeTypeException.class)
    public ResponseEntity<Void> invalidMimeTypeException(InvalidMimeTypeException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }
}


