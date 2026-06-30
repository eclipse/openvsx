/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
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

@Converter
class UserRoleConverter implements AttributeConverter<UserData.Role, String> {

    @Override
    public String convertToDatabaseColumn(UserData.Role role) {
        return role != null ? role.toString() : null;
    }

    @Override
    public UserData.Role convertToEntityAttribute(String value) {
        return UserData.Role.valueOfIgnoreCase(value);
    }
}
