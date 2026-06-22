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
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import java.util.Collection;
import java.util.List;

public interface ExtensionRepository extends Repository<Extension, Long> {

    Streamable<Extension> findByNamespace(Namespace namespace);

    Streamable<Extension> findByNamespaceAndActiveTrueOrderByNameAsc(Namespace namespace);

    Extension findByNameIgnoreCaseAndNamespace(String name, Namespace namespace);

    Extension findByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespace);

    // Publish takes this lock (waiting) before adding a version. Explicit FOR UPDATE so it also
    // conflicts with the FOR KEY SHARE a version insert takes.
    @Query(value = "select e.* from extension e join namespace n on n.id = e.namespace_id"
            + " where lower(e.name) = lower(:name) and lower(n.name) = lower(:namespace) for update of e",
            nativeQuery = true)
    Extension findByNameIgnoreCaseAndNamespaceNameIgnoreCaseForUpdate(@Param("name") String name, @Param("namespace") String namespace);

    // Delete takes this NOWAIT variant so it fails fast (instead of blocking) when a publish holds
    // the lock, letting us surface a "retry" error rather than removing the extension under a publish.
    @Query(value = "select e.* from extension e join namespace n on n.id = e.namespace_id"
            + " where lower(e.name) = lower(:name) and lower(n.name) = lower(:namespace) for update of e nowait",
            nativeQuery = true)
    Extension findByNameIgnoreCaseAndNamespaceNameIgnoreCaseForUpdateNoWait(@Param("name") String name, @Param("namespace") String namespace);

    Streamable<Extension> findByActiveTrue();

    Streamable<Extension> findByIdIn(Collection<Long> extensionIds);

    Streamable<Extension> findDistinctByVersionsPublishedWithUser(UserData user);

    long count();

    @Query("select coalesce(max(e.downloadCount), 0) from Extension e")
    int getMaxDownloadCount();

    @Query("select e from Extension e where concat(e.namespace.name, '.', e.name) not in(?1)")
    Streamable<Extension> findAllNotMatchingByExtensionId(List<String> extensionIds);

    Streamable<Extension> findByReplacement(Extension replacement);
}
