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

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.eclipse.persistence.mappings.DirectCollectionMapping;
import org.eclipse.persistence.mappings.ObjectReferenceMapping;
import org.eclipse.persistence.sessions.Session;

public class JpaSessionCustomizer implements SessionCustomizer {

    @Override
    public void customize(Session session) throws Exception {
        for (var descriptor : session.getDescriptors().values()) {
            if (!descriptor.getTables().isEmpty()) {
                customizeTable(descriptor);
            }
        }
    }

    protected void customizeTable(ClassDescriptor descriptor) {
        if (descriptor.getAlias().equalsIgnoreCase(descriptor.getTableName())) {
            var tableName = toUnderscore(descriptor.getJavaClass().getSimpleName());
            descriptor.setTableName(tableName);
            for (var table : descriptor.getTables()) {
                for (var index : table.getIndexes()) {
                    index.setTargetTable(tableName);
                }
            }
        }
        for (var mapping : descriptor.getMappings()) {
            customizeMapping(mapping, descriptor);
        }
    }

    protected void customizeMapping(DatabaseMapping mapping, ClassDescriptor descriptor) {
        var field = mapping.getField();
        var attributeName = mapping.getAttributeName();
        if (field != null && field.getName().equals(attributeName.toUpperCase())) {
            field.setName(toUnderscore(attributeName));
        } else if (mapping.isObjectReferenceMapping()) {
            var orm = (ObjectReferenceMapping) mapping;
            for (var foreignField : orm.getForeignKeyFields()) {
                if (foreignField.getName().equals(attributeName.toUpperCase() + "_ID")) {
                    foreignField.setName(toUnderscore(attributeName) + "_id");
                } else {
                    System.out.println("#####" + foreignField.getName());
                }
            }
        } else if (mapping.isDirectCollectionMapping()) {
            var dcm = (DirectCollectionMapping) mapping;
            var refTable = dcm.getReferenceTable();
            if (refTable != null && refTable.getName().equals(descriptor.getAlias() + "_" + attributeName.toUpperCase())) {
                refTable.setName(descriptor.getTableName() + "_" + toUnderscore(attributeName));
            }
            var directField = dcm.getDirectField();
            if (directField != null && directField.getName().equals(attributeName.toUpperCase())) {
                directField.setName(toUnderscore(attributeName));
            }
            for (var referenceField : dcm.getReferenceKeyFields()) {
                if (referenceField.getName().equals(descriptor.getAlias() + "_ID")) {
                    referenceField.setName(descriptor.getTableName() + "_id");
                }
            }
        }
    }

    protected String toUnderscore(String s) {
        var result = new StringBuilder();
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            if (Character.isUpperCase(c) && result.length() > 0)
                result.append('_');
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }
     
}