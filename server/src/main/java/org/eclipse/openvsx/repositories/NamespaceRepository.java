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

import org.eclipse.openvsx.entities.Namespace;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;

import java.util.List;

public interface NamespaceRepository extends Repository<Namespace, Long> {

    Namespace findByNameIgnoreCase(String name);

    /**
     * Returns a list of namespaces whose name or displayName matches the given displayName case-insensitive.
     *
     * @param displayName the displayName to check for existing conflicts
     * @param excludeNamespace the namespace to exclude
     * @return a list of namespaces or an empty list of there are no conflicts
     */
    @Query("from Namespace n where (lower(n.displayName) = lower(:displayName) or lower(n.name) = lower(:displayName)) and n <> :namespace")
    List<Namespace> findConflictingNamespaces(String displayName, @Param("namespace") Namespace excludeNamespace);

    Namespace findByPublicId(String publicId);

    @Query("from Namespace n where not exists (from NamespaceMembership nm where nm.namespace = n)")
    Streamable<Namespace> findOrphans();

    long count();
}
