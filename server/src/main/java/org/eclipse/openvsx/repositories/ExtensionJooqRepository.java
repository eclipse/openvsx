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
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.jooq.Tables.*;

@Component
public class ExtensionJooqRepository {

    @Autowired
    DSLContext dsl;

    public List<Extension> findAllActiveById(Collection<Long> ids) {
        return fetch(findAllActive().and(EXTENSION.ID.in(ids)));
    }

    public List<Extension> findAllActiveByPublicId(Collection<String> publicIds, String... namespacesToExclude) {
        var query = findAllActive().and(EXTENSION.PUBLIC_ID.in(publicIds));
        for(var namespaceToExclude : namespacesToExclude) {
            query = query.and(NAMESPACE.NAME.notEqual(namespaceToExclude));
        }

        return fetch(query);
    }

    public Extension findActiveByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespaceName) {
        var record = findAllActive()
                .and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(name)))
                .and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName)))
                .fetchOne();

        return record != null ? toExtension(record) : null;
    }

    public Map<Long, Integer> findAllActiveReviewCountsById(Collection<Long> ids) {
        var count = DSL.count(EXTENSION_REVIEW.ID).as("count");
        return dsl.select(EXTENSION_REVIEW.EXTENSION_ID, count)
                .from(EXTENSION_REVIEW)
                .where(EXTENSION_REVIEW.ACTIVE.eq(true))
                .and(EXTENSION_REVIEW.EXTENSION_ID.in(ids))
                .groupBy(EXTENSION_REVIEW.EXTENSION_ID)
                .fetch()
                .stream()
                .collect(Collectors.toMap(r -> r.get(EXTENSION_REVIEW.EXTENSION_ID), r -> r.get(count)));
    }

    private SelectConditionStep<?> findAllActive() {
        return dsl.select(
                    EXTENSION.ID,
                    EXTENSION.PUBLIC_ID,
                    EXTENSION.NAME,
                    EXTENSION.AVERAGE_RATING,
                    EXTENSION.DOWNLOAD_COUNT,
                    EXTENSION.PUBLISHED_DATE,
                    EXTENSION.LAST_UPDATED_DATE,
                    NAMESPACE.ID,
                    NAMESPACE.PUBLIC_ID,
                    NAMESPACE.NAME
                )
                .from(EXTENSION)
                .join(NAMESPACE).on(NAMESPACE.ID.eq(EXTENSION.NAMESPACE_ID))
                .where(EXTENSION.ACTIVE.eq(true));
    }

    private List<Extension> fetch(SelectConditionStep<?> query) {
        return query.fetch().map(this::toExtension);
    }

    private Extension toExtension(Record record) {
        var extension = new Extension();
        extension.setId(record.get(EXTENSION.ID));
        extension.setPublicId(record.get(EXTENSION.PUBLIC_ID));
        extension.setName(record.get(EXTENSION.NAME));
        extension.setAverageRating(record.get(EXTENSION.AVERAGE_RATING));
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
