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
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.NAMESPACE_MEMBERSHIP;

@Component
public class NamespaceMembershipDTORepository {

    @Autowired
    DSLContext dsl;

    public List<NamespaceMembershipDTO> findAllByNamespaceId(Collection<Long> namespaceIds) {
        return dsl.select(
                    NAMESPACE_MEMBERSHIP.ID,
                    NAMESPACE_MEMBERSHIP.ROLE,
                    NAMESPACE_MEMBERSHIP.NAMESPACE,
                    NAMESPACE_MEMBERSHIP.USER_DATA
                )
                .from(NAMESPACE_MEMBERSHIP)
                .where(NAMESPACE_MEMBERSHIP.NAMESPACE.in(namespaceIds))
                .fetchInto(NamespaceMembershipDTO.class);
    }
}
