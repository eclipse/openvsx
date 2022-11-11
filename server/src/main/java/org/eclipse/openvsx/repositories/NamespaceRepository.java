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
import org.eclipse.openvsx.entities.Namespace;

import java.time.LocalDateTime;

public interface NamespaceRepository extends Repository<Namespace, Long> {

    Namespace findByNameIgnoreCase(String name);

    Namespace findByPublicId(String publicId);

    @Query("from Namespace n where not exists (from NamespaceMembership nm where nm.namespace = n)")
    Streamable<Namespace> findOrphans();

    long count();

    <S extends Namespace> S save(S entity);

    void delete(Namespace entity);
}