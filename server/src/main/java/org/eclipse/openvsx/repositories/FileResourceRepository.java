/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;

public interface FileResourceRepository extends Repository<FileResource, Long> {

    Streamable<FileResource> findByExtension(ExtensionVersion extVersion);

    Streamable<FileResource> findByExtensionExtensionNamespace(Namespace namespace);

    Streamable<FileResource> findByStorageType(String storageType);

    FileResource findFirstByExtensionAndNameIgnoreCaseOrderByType(ExtensionVersion extVersion, String name);

    FileResource findByExtensionAndType(ExtensionVersion extVersion, String type);

    FileResource findByExtensionAndTypeAndNameIgnoreCase(ExtensionVersion extVersion, String type, String name);

    Streamable<FileResource> findByTypeAndStorageTypeAndNameIgnoreCaseIn(String type, String storageType, Collection<String> names);

    Streamable<FileResource> findByExtensionAndTypeIn(ExtensionVersion extVersion, Collection<String> types);

    void deleteByExtensionAndType(ExtensionVersion extVersion, String type);

    void deleteByType(String type);

    Streamable<FileResource> findByType(String type);
}