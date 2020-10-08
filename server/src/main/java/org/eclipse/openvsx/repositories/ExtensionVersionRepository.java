/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.PersonalAccessToken;

public interface ExtensionVersionRepository extends Repository<ExtensionVersion, Long> {

    Streamable<ExtensionVersion> findByExtension(Extension extension);

    Streamable<ExtensionVersion> findByExtensionAndPreview(Extension extension, boolean preview);

    ExtensionVersion findByVersionAndExtension(String version, Extension extension);

    ExtensionVersion findByVersionAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(String version, String extensionName, String namespace);

    Streamable<ExtensionVersion> findByBundledExtensions(Extension extension);

    Streamable<ExtensionVersion> findByDependencies(Extension extension);

    Streamable<ExtensionVersion> findByLicense(String license);

    Streamable<ExtensionVersion> findByPublishedWith(PersonalAccessToken publishedWith);

    Streamable<ExtensionVersion> findAll();

    @Query("select ev.version from ExtensionVersion ev where ev.extension = ?1 order by ev.timestamp desc")
    Streamable<String> getVersionStrings(Extension extension);

    @Query("select min(ev.timestamp) from ExtensionVersion ev")
    LocalDateTime getOldestTimestamp();

}