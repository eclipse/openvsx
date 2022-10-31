/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.SetPreReleaseMigrationItem;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

public interface SetPreReleaseMigrationItemRepository extends Repository<SetPreReleaseMigrationItem, Long> {

    // migrate extensions with most downloads first
    Streamable<SetPreReleaseMigrationItem> findByMigrationScheduledFalseOrderByExtensionDownloadCountDesc();
}
