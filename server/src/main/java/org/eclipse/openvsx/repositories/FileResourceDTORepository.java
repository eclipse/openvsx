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

import static org.eclipse.openvsx.jooq.Tables.*;

@Component
public class FileResourceDTORepository {

    @Autowired
    DSLContext dsl;

    public List<FileResourceDTO> findAll(Collection<Long> extensionIds, Collection<String> types) {
        return dsl.select(FILE_RESOURCE.ID, FILE_RESOURCE.EXTENSION_ID, FILE_RESOURCE.NAME, FILE_RESOURCE.TYPE)
                .from(FILE_RESOURCE)
                .where(FILE_RESOURCE.EXTENSION_ID.in(extensionIds))
                .and(FILE_RESOURCE.TYPE.in(types))
                .fetchInto(FileResourceDTO.class);
    }

    public List<FileResourceDTO> findAllResources(String namespaceName, String extensionName, String version, String prefix) {
        return dsl.select(FILE_RESOURCE.ID, FILE_RESOURCE.EXTENSION_ID, FILE_RESOURCE.NAME, FILE_RESOURCE.TYPE, FILE_RESOURCE.STORAGE_TYPE, FILE_RESOURCE.CONTENT)
                .from(FILE_RESOURCE)
                .join(EXTENSION_VERSION).on(EXTENSION_VERSION.ID.eq(FILE_RESOURCE.EXTENSION_ID))
                .join(EXTENSION).on(EXTENSION.ID.eq(EXTENSION_VERSION.EXTENSION_ID))
                .join(NAMESPACE).on(NAMESPACE.ID.eq(EXTENSION.NAMESPACE_ID))
                .where(NAMESPACE.NAME.eq(namespaceName))
                .and(EXTENSION.NAME.eq(extensionName))
                .and(EXTENSION_VERSION.VERSION.eq(version))
                .and(FILE_RESOURCE.TYPE.eq(FileResource.RESOURCE))
                .and(FILE_RESOURCE.NAME.startsWith(prefix))
                .fetchInto(FileResourceDTO.class);
    }
}
