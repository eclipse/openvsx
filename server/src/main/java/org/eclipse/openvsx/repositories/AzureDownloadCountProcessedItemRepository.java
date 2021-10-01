/********************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.AzureDownloadCountProcessedItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface AzureDownloadCountProcessedItemRepository extends Repository<AzureDownloadCountProcessedItem, Long> {

    @Query("select dc.name from AzureDownloadCountProcessedItem dc where dc.success = true and dc.name in(?1)")
    List<String> findAllSucceededAzureDownloadCountProcessedItemsByNameIn(List<String> names);
}
