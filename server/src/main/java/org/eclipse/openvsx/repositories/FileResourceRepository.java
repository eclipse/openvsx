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

    void deleteByExtension(ExtensionVersion extVersion);

    Streamable<FileResource> findByExtensionExtensionNamespace(Namespace namespace);

    Streamable<FileResource> findByStorageType(String storageType);

    FileResource findByExtensionAndType(ExtensionVersion extVersion, String type);

    Streamable<FileResource> findByTypeAndStorageTypeAndNameIgnoreCaseIn(String type, String storageType, Collection<String> names);

    void deleteByType(String type);

    Streamable<FileResource> findByType(String type);

    Streamable<FileResource> findByExtension(ExtensionVersion extVersion);
}