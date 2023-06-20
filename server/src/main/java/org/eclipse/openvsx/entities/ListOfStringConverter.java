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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class ListOfStringConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> data) {
        return (data == null || data.isEmpty()) ? null : Joiner.on(',').join(data);
    }

    @Override
    public List<String> convertToEntityAttribute(String raw) {
        return (raw == null) ? Lists.newArrayList() : Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(raw));
    }

}
