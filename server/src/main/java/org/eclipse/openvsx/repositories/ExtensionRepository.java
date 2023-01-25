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

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.UserData;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;
import java.util.List;

public interface ExtensionRepository extends Repository<Extension, Long> {

    Streamable<Extension> findByNameIgnoreCase(String name);

    Streamable<Extension> findByNamespace(Namespace namespace);

    Streamable<Extension> findByNamespaceAndActiveTrueOrderByNameAsc(Namespace namespace);

    Extension findByNameIgnoreCaseAndNamespace(String name, Namespace namespace);

    Extension findByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespace);

    Extension findByPublicId(String publicId);

    Streamable<Extension> findByActiveTrue();

    Streamable<Extension> findByIdIn(Collection<Long> extensionIds);

    Streamable<Extension> findDistinctByVersionsPublishedWithUser(UserData user);

    long count();

    long countByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespace);

    @Query("select max(e.downloadCount) from Extension e")
    int getMaxDownloadCount();

    @Query("select e from Extension e where concat(e.namespace.name, '.', e.name) not in(?1)")
    Streamable<Extension> findAllNotMatchingByExtensionId(List<String> extensionIds);
}