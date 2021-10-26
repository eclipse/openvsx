/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.dto.FileResourceDTO;
import org.eclipse.openvsx.entities.FileResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;

public interface FileResourceDTORepository extends Repository<FileResource, Long> {

    @Query("select new org.eclipse.openvsx.dto.FileResourceDTO(r.id, r.extension.id, r.name, r.type) " +
            "from FileResource r " +
            "where r.extension.id in(?1) and r.type in(?2)")
    Streamable<FileResourceDTO> findAllByExtensionIdAndType(Collection<Long> extensionIds, Collection<String> types);
}
