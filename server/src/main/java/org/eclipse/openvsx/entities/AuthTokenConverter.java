/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Converter
public class AuthTokenConverter implements AttributeConverter<AuthToken, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthTokenConverter() {
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Override
    public String convertToDatabaseColumn(AuthToken data) {
        if (data == null)
            return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exc) {
            throw new RuntimeException("Failed to serialize AuthToken to DB column.", exc);
        }
    }

    @Override
    public AuthToken convertToEntityAttribute(String raw) {
        if (raw == null)
            return null;
        try {
            return objectMapper.readValue(raw, AuthToken.class);
        } catch (JsonProcessingException exc) {
            throw new RuntimeException("Failed to parse AuthToken from DB column.", exc);
        }
    }

}