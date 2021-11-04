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
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.FILE_RESOURCE;

@Component
public class FileResourceDTORepository {

    @Autowired
    DSLContext dsl;

    public List<FileResourceDTO> findAllByExtensionIdAndType(Collection<Long> extensionIds, Collection<String> types) {
        return dsl.select(FILE_RESOURCE.ID, FILE_RESOURCE.EXTENSION_ID, FILE_RESOURCE.NAME, FILE_RESOURCE.TYPE)
                .from(FILE_RESOURCE)
                .where(FILE_RESOURCE.EXTENSION_ID.in(extensionIds))
                .and(FILE_RESOURCE.TYPE.in(types))
                .fetchInto(FileResourceDTO.class);
    }
}
