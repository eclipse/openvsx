/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.entities;

import jakarta.persistence.*;

@Entity
public class MigrationItem {

    @Id
    @GeneratedValue(generator = "migrationItemSeq")
    @SequenceGenerator(name = "migrationItemSeq", sequenceName = "migration_item_seq")
    long id;

    String migrationScript;

    long entityId;

    boolean migrationScheduled;

    public long getId() {
        return id;
    }

    public String getMigrationScript() {
        return migrationScript;
    }

    public void setMigrationScript(String migrationScript) {
        this.migrationScript = migrationScript;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public boolean isMigrationScheduled() {
        return migrationScheduled;
    }

    public void setMigrationScheduled(boolean migrationScheduled) {
        this.migrationScheduled = migrationScheduled;
    }
}
