/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class ExtractResourcesMigrationItem {

    @Id
    @GeneratedValue
    long id;

    @OneToOne
    ExtensionVersion extension;

    boolean migrationScheduled;

    public long getId() {
        return id;
    }

    public ExtensionVersion getExtension() {
        return extension;
    }

    public void setExtension(ExtensionVersion extension) {
        this.extension = extension;
    }

    public boolean isMigrationScheduled() {
        return migrationScheduled;
    }

    public void setMigrationScheduled(boolean migrationScheduled) {
        this.migrationScheduled = migrationScheduled;
    }
}
