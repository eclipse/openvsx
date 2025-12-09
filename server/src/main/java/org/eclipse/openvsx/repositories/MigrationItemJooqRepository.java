/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.MigrationItem;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.FILE_RESOURCE;
import static org.eclipse.openvsx.jooq.Tables.MIGRATION_ITEM;

@Component
public class MigrationItemJooqRepository {

    private final DSLContext dsl;

    public MigrationItemJooqRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<MigrationItem> findRemoveFileResourceTypeResourceMigrationItems(int offset, int limit) {
        var jobName = "RemoveFileResourceTypeResourceMigration";
        return dsl.select(MIGRATION_ITEM.ID)
                .from(MIGRATION_ITEM)
                .join(FILE_RESOURCE).on(FILE_RESOURCE.ID.eq(MIGRATION_ITEM.ENTITY_ID))
                .where(MIGRATION_ITEM.JOB_NAME.eq(jobName))
                .limit(limit)
                .offset(offset)
                .fetch(row -> {
                    var item = new MigrationItem();
                    item.setJobName(jobName);
                    item.setId(row.get(MIGRATION_ITEM.ID));
                    return item;
                });
    }
}
