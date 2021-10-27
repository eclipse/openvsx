/********************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.dto.NamespaceMembershipDTO;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;

public interface NamespaceMembershipDTORepository extends Repository<NamespaceMembership, Long> {

    @Query(
            "select new org.eclipse.openvsx.dto.NamespaceMembershipDTO(nm.id,nm.role,nm.namespace.id,nm.user.id) " +
            "from NamespaceMembership nm " +
            "where nm.namespace.id IN(?1)"
    )
    Streamable<NamespaceMembershipDTO> findAllByNamespaceId(Collection<Long> namespaceIds);
}
