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

import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;

public interface ExtensionVersionDTORepository extends Repository<ExtensionVersion, Long> {

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "ev.extension.id," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.repository," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "where ev.active = true and ev.extension.id in(?1)")
    Streamable<ExtensionVersionDTO> findAllActiveByExtensionId(Collection<Long> extensionIds);

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "n.id," +
            "n.publicId," +
            "n.name," +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.latest.id," +
            "e.preview.id," +
            "e.averageRating," +
            "e.downloadCount," +
            "u.id," +
            "u.loginName," +
            "u.fullName," +
            "u.avatarUrl," +
            "u.providerUrl," +
            "u.provider," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.license," +
            "ev.homepage," +
            "ev.repository," +
            "ev.bugs," +
            "ev.markdown," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.qna," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "join ev.extension e " +
            "join e.namespace n " +
            "left join ev.publishedWith pw " +
            "join pw.user u " +
            "where ev.active = true and e.publicId = ?1"
    )
    Streamable<ExtensionVersionDTO> findAllActiveByExtensionPublicId(String extensionPublicId);

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "n.id," +
            "n.publicId," +
            "n.name," +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.latest.id," +
            "e.preview.id," +
            "e.averageRating," +
            "e.downloadCount," +
            "u.id," +
            "u.loginName," +
            "u.fullName," +
            "u.avatarUrl," +
            "u.providerUrl," +
            "u.provider," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.license," +
            "ev.homepage," +
            "ev.repository," +
            "ev.bugs," +
            "ev.markdown," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.qna," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "join ev.extension e " +
            "join e.namespace n " +
            "left join ev.publishedWith pw " +
            "join pw.user u " +
            "where ev.active = true and n.publicId = ?1"
    )
    Streamable<ExtensionVersionDTO> findAllActiveByNamespacePublicId(String namespacePublicId);

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "n.id," +
            "n.publicId," +
            "n.name," +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.latest.id," +
            "e.preview.id," +
            "e.averageRating," +
            "e.downloadCount," +
            "u.id," +
            "u.loginName," +
            "u.fullName," +
            "u.avatarUrl," +
            "u.providerUrl," +
            "u.provider," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.license," +
            "ev.homepage," +
            "ev.repository," +
            "ev.bugs," +
            "ev.markdown," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.qna," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "join ev.extension e " +
            "join e.namespace n " +
            "left join ev.publishedWith pw " +
            "join pw.user u " +
            "where ev.active = true and ev.version = ?1 and upper(e.name) = upper(?2) and upper(n.name) = upper(?3)"
    )
    ExtensionVersionDTO findActiveByVersionAndExtensionNameAndNamespaceName(String extensionVersion, String extensionName, String namespaceName);

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "n.id," +
            "n.publicId," +
            "n.name," +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.latest.id," +
            "e.preview.id," +
            "e.averageRating," +
            "e.downloadCount," +
            "u.id," +
            "u.loginName," +
            "u.fullName," +
            "u.avatarUrl," +
            "u.providerUrl," +
            "u.provider," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.license," +
            "ev.homepage," +
            "ev.repository," +
            "ev.bugs," +
            "ev.markdown," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.qna," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "join ev.extension e " +
            "join e.namespace n " +
            "left join ev.publishedWith pw " +
            "join pw.user u " +
            "where ev.active = true and upper(e.name) = upper(?1) and upper(n.name) = upper(?2)"
    )
    Streamable<ExtensionVersionDTO> findAllActiveByExtensionNameAndNamespaceName(String extensionName, String namespaceName);

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "n.id," +
            "n.publicId," +
            "n.name," +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.latest.id," +
            "e.preview.id," +
            "e.averageRating," +
            "e.downloadCount," +
            "u.id," +
            "u.loginName," +
            "u.fullName," +
            "u.avatarUrl," +
            "u.providerUrl," +
            "u.provider," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.license," +
            "ev.homepage," +
            "ev.repository," +
            "ev.bugs," +
            "ev.markdown," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.qna," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "join ev.extension e " +
            "join e.namespace n " +
            "left join ev.publishedWith pw " +
            "join pw.user u " +
            "where ev.active = true and upper(n.name) = upper(?1)"
    )
    Streamable<ExtensionVersionDTO> findAllActiveByNamespaceName(String namespaceName);

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "n.id," +
            "n.publicId," +
            "n.name," +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.latest.id," +
            "e.preview.id," +
            "e.averageRating," +
            "e.downloadCount," +
            "u.id," +
            "u.loginName," +
            "u.fullName," +
            "u.avatarUrl," +
            "u.providerUrl," +
            "u.provider," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.license," +
            "ev.homepage," +
            "ev.repository," +
            "ev.bugs," +
            "ev.markdown," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.qna," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "join ev.extension e " +
            "join e.namespace n " +
            "left join ev.publishedWith pw " +
            "join pw.user u " +
            "where ev.active = true and upper(e.name) = upper(?1)"
    )
    Streamable<ExtensionVersionDTO> findAllActiveByExtensionName(String extensionName);
}
