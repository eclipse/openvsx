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

import org.eclipse.openvsx.entities.UserData;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.PersonalAccessToken;

public interface ExtensionVersionRepository extends Repository<ExtensionVersion, Long> {

    Streamable<ExtensionVersion> findByExtension(Extension extension);

    Streamable<ExtensionVersion> findByExtensionAndActiveTrue(Extension extension);

    Streamable<ExtensionVersion> findByVersionAndExtension(String version, Extension extension);

    ExtensionVersion findByVersionAndTargetPlatformAndExtension(String version, String targetPlatform, Extension extension);

    ExtensionVersion findByVersionAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(String version, String targetPlatform, String extensionName, String namespace);

    Streamable<ExtensionVersion> findByVersionAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(String version, String extensionName, String namespace);

    Streamable<ExtensionVersion> findByPublishedWithUser(UserData user);

    Streamable<ExtensionVersion> findByPublishedWith(PersonalAccessToken publishedWith);

    Streamable<ExtensionVersion> findByPublishedWithAndActive(PersonalAccessToken publishedWith, boolean active);

    @Query("select ev from ExtensionVersion ev where concat(',', ev.bundledExtensions, ',') like concat('%,', ?1, ',%')")
    Streamable<ExtensionVersion> findByBundledExtensions(String extensionId);

    @Query("select ev from ExtensionVersion ev where concat(',', ev.dependencies, ',') like concat('%,', ?1, ',%')")
    Streamable<ExtensionVersion> findByDependencies(String extensionId);

    @Query("select min(ev.timestamp) from ExtensionVersion ev")
    LocalDateTime getOldestTimestamp();
}