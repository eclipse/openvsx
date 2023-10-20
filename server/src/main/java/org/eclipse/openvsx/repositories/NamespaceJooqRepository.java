/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.jooq.DSLContext;
import org.jooq.Row2;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.jooq.Tables.EXTENSION;
import static org.eclipse.openvsx.jooq.Tables.NAMESPACE;

@Component
public class NamespaceJooqRepository {

    @Autowired
    DSLContext dsl;

    public void updatePublicIds(Map<Long, String> publicIds) {
        if(publicIds.isEmpty()) {
            return;
        }

        var namespace = NAMESPACE.as("n");
        var rows = publicIds.entrySet().stream()
                .map(e -> DSL.row(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        var updates = DSL.values(rows.toArray(Row2[]::new)).as("u", "id", "public_id");
        dsl.update(namespace)
                .set(namespace.PUBLIC_ID, updates.field("public_id", String.class))
                .from(updates)
                .where(updates.field("id", Long.class).eq(namespace.ID))
                .execute();
    }

    public boolean publicIdExists(String publicId) {
        return dsl.selectOne()
                .from(NAMESPACE)
                .where(NAMESPACE.PUBLIC_ID.eq(publicId))
                .fetch()
                .isNotEmpty();
    }
}
