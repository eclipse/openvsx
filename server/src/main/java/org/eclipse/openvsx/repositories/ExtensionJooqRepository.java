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

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.eclipse.openvsx.jooq.Tables.*;

@Component
public class ExtensionJooqRepository {

    @Autowired
    DSLContext dsl;

    public List<Extension> findAllActiveById(Collection<Long> ids) {
        var query = findAllActive();
        query.addConditions(EXTENSION.ID.in(ids));
        return fetch(query);
    }

    public List<Extension> findAllActiveByPublicId(Collection<String> publicIds, String... namespacesToExclude) {
        var conditions = new ArrayList<Condition>();
        conditions.add(EXTENSION.PUBLIC_ID.in(publicIds));
        for(var namespaceToExclude : namespacesToExclude) {
            conditions.add(NAMESPACE.NAME.notEqual(namespaceToExclude));
        }

        var query = findAllActive();
        query.addConditions(conditions);
        return fetch(query);
    }

    public Extension findActiveByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespaceName) {
        var query = findAllActive();
        query.addConditions(
                DSL.upper(EXTENSION.NAME).eq(DSL.upper(name)),
                DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName))
        );

        var record = query.fetchOne();
        return record != null ? toExtension(record) : null;
    }

    private SelectQuery<Record> findAllActive() {
        var query = dsl.selectQuery();
        query.addSelect(
                EXTENSION.ID,
                EXTENSION.PUBLIC_ID,
                EXTENSION.NAME,
                EXTENSION.AVERAGE_RATING,
                EXTENSION.REVIEW_COUNT,
                EXTENSION.DOWNLOAD_COUNT,
                EXTENSION.PUBLISHED_DATE,
                EXTENSION.LAST_UPDATED_DATE,
                NAMESPACE.ID,
                NAMESPACE.PUBLIC_ID,
                NAMESPACE.NAME
        );

        query.addFrom(EXTENSION);
        query.addJoin(NAMESPACE, NAMESPACE.ID.eq(EXTENSION.NAMESPACE_ID));
        query.addConditions(EXTENSION.ACTIVE.eq(true));
        return query;
    }

    private List<Extension> fetch(SelectQuery<Record> query) {
        return query.fetch().map(this::toExtension);
    }

    private Extension toExtension(Record record) {
        var extension = new Extension();
        extension.setId(record.get(EXTENSION.ID));
        extension.setPublicId(record.get(EXTENSION.PUBLIC_ID));
        extension.setName(record.get(EXTENSION.NAME));
        extension.setAverageRating(record.get(EXTENSION.AVERAGE_RATING));
        extension.setReviewCount(record.get(EXTENSION.REVIEW_COUNT));
        extension.setDownloadCount(record.get(EXTENSION.DOWNLOAD_COUNT));
        extension.setPublishedDate(record.get(EXTENSION.PUBLISHED_DATE));
        extension.setLastUpdatedDate(record.get(EXTENSION.LAST_UPDATED_DATE));

        var namespace = new Namespace();
        namespace.setId(record.get(NAMESPACE.ID));
        namespace.setPublicId(record.get(NAMESPACE.PUBLIC_ID));
        namespace.setName(record.get(NAMESPACE.NAME));
        extension.setNamespace(namespace);

        return extension;
    }
}
