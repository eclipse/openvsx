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

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.*;

@Component
public class FileResourceJooqRepository {

    @Autowired
    DSLContext dsl;

    public List<FileResource> findAll(Collection<Long> extensionIds, Collection<String> types) {
        return dsl.select(FILE_RESOURCE.ID, FILE_RESOURCE.EXTENSION_ID, FILE_RESOURCE.NAME, FILE_RESOURCE.TYPE)
                .from(FILE_RESOURCE)
                .where(FILE_RESOURCE.EXTENSION_ID.in(extensionIds))
                .and(FILE_RESOURCE.TYPE.in(types))
                .fetch()
                .map(this::toFileResource);
    }

    public List<FileResource> findAllResources(long extVersionId, String prefix) {
        return dsl.select(FILE_RESOURCE.ID, FILE_RESOURCE.EXTENSION_ID, FILE_RESOURCE.NAME, FILE_RESOURCE.TYPE, FILE_RESOURCE.STORAGE_TYPE, FILE_RESOURCE.CONTENT)
                .from(FILE_RESOURCE)
                .where(FILE_RESOURCE.TYPE.eq(FileResource.RESOURCE))
                .and(FILE_RESOURCE.EXTENSION_ID.eq(extVersionId))
                .and(FILE_RESOURCE.NAME.startsWith(prefix))
                .fetch()
                .map(record -> {
                    var fileResource = toFileResource(record);
                    fileResource.setStorageType(record.get(FILE_RESOURCE.STORAGE_TYPE));
                    fileResource.setContent(record.get(FILE_RESOURCE.CONTENT));

                    return fileResource;
                });
    }

    private FileResource toFileResource(Record record) {
        var fileResource = new FileResource();
        fileResource.setId(record.get(FILE_RESOURCE.ID));
        fileResource.setName(record.get(FILE_RESOURCE.NAME));
        fileResource.setType(record.get(FILE_RESOURCE.TYPE));

        var extVersion = new ExtensionVersion();
        extVersion.setId(record.get(FILE_RESOURCE.EXTENSION_ID));
        fileResource.setExtension(extVersion);

        return fileResource;
    }
}
