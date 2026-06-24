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

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import org.eclipse.openvsx.entities.*;

public interface ExtensionVersionRepository extends Repository<ExtensionVersion, Long> {

    Streamable<ExtensionVersion> findByExtension(Extension extension);

    Streamable<ExtensionVersion> findByExtensionAndActiveTrue(Extension extension);

    ExtensionVersion findByVersionAndTargetPlatformAndExtension(
            String version,
            String targetPlatform,
            Extension extension
    );

    ExtensionVersion findByVersionAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(
            String version,
            String targetPlatform,
            String extensionName,
            String namespace
    );

    ExtensionVersion findByPublishedWithUserAndVersionAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(
            UserData user,
            String version,
            String targetPlatform,
            String extensionName,
            String namespace
    );

    Streamable<ExtensionVersion> findByVersionAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(
            String version,
            String extensionName,
            String namespace
    );

    Streamable<ExtensionVersion> findByPublishedWithAndActive(PersonalAccessToken publishedWith, boolean active);

    long countByPublishedWith(PersonalAccessToken publishedWith);

    Streamable<ExtensionVersion> findByPublishedWithUserAndActive(UserData user, boolean active);

    Streamable<ExtensionVersion> findAll();

    Streamable<ExtensionVersion> findBySignatureKeyPairNotOrSignatureKeyPairIsNull(SignatureKeyPair keyPair);

    @Query(
        "select ev from ExtensionVersion ev where concat(',', ev.bundledExtensions, ',') like concat('%,', ?1, ',%')"
    )
    Streamable<ExtensionVersion> findByBundledExtensions(String extensionId);

    @Query("select ev from ExtensionVersion ev where concat(',', ev.dependencies, ',') like concat('%,', ?1, ',%')")
    Streamable<ExtensionVersion> findByDependencies(String extensionId);

    @Query("select min(ev.timestamp) from ExtensionVersion ev")
    LocalDateTime getOldestTimestamp();

    @Modifying
    @Query("update ExtensionVersion ev set ev.signatureKeyPair = null")
    void setKeyPairsNull();

    Page<ExtensionVersion> findByActiveTrueAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(
            String extension,
            String namespace,
            Pageable page
    );

    Page<ExtensionVersion> findByActiveTrueAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(
            String targetPlatform,
            String extension,
            String namespace,
            Pageable page
    );
}
