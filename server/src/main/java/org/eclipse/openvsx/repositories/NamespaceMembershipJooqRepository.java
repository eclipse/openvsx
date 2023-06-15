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

import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.UserData;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.NAMESPACE_MEMBERSHIP;

@Component
public class NamespaceMembershipJooqRepository {

    @Autowired
    DSLContext dsl;

    public List<NamespaceMembership> findAllByNamespaceId(Collection<Long> namespaceIds) {
        return dsl.select(
                    NAMESPACE_MEMBERSHIP.ID,
                    NAMESPACE_MEMBERSHIP.ROLE,
                    NAMESPACE_MEMBERSHIP.NAMESPACE,
                    NAMESPACE_MEMBERSHIP.USER_DATA
                )
                .from(NAMESPACE_MEMBERSHIP)
                .where(NAMESPACE_MEMBERSHIP.NAMESPACE.in(namespaceIds))
                .fetch()
                .map(this::toNamespaceMembership);
    }

    private NamespaceMembership toNamespaceMembership(Record record) {
        var namespaceMembership = new NamespaceMembership();
        namespaceMembership.setId(record.get(NAMESPACE_MEMBERSHIP.ID));
        namespaceMembership.setRole(record.get(NAMESPACE_MEMBERSHIP.ROLE));

        var namespace = new Namespace();
        namespace.setId(record.get(NAMESPACE_MEMBERSHIP.NAMESPACE));
        namespaceMembership.setNamespace(namespace);

        var user = new UserData();
        user.setId(record.get(NAMESPACE_MEMBERSHIP.USER_DATA));
        namespaceMembership.setUser(user);

        return namespaceMembership;
    }

    public boolean isVerified(Namespace namespace, UserData user) {
        var nm = NAMESPACE_MEMBERSHIP.as("nm");
        var onm = NAMESPACE_MEMBERSHIP.as("onm");
        var result = dsl.selectOne()
                .from(nm)
                .join(onm).on(onm.NAMESPACE.eq(nm.NAMESPACE))
                .where(onm.NAMESPACE.eq(namespace.getId()))
                .and(onm.ROLE.eq(NamespaceMembership.ROLE_OWNER))
                .and(nm.USER_DATA.eq(user.getId()))
                .fetch();

        return result.isNotEmpty();
    }
}
