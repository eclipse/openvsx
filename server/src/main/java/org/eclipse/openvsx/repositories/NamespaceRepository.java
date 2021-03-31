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

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;
import org.eclipse.openvsx.entities.Namespace;

public interface NamespaceRepository extends Repository<Namespace, Long> {

    @Cacheable("findByNameIgnoreCase")
    Namespace findByNameIgnoreCase(String name);

    @Cacheable("findByPublicId")
    Namespace findByPublicId(String publicId);

    @Cacheable("findOrphans")
    @Query("from Namespace n where not exists (from NamespaceMembership nm where nm.namespace = n)")
    Streamable<Namespace> findOrphans();

    @Cacheable("Namespace.count")
    long count();

}