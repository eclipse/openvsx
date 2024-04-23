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
import org.eclipse.openvsx.web.SitemapRow;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.jooq.Tables.EXTENSION;
import static org.eclipse.openvsx.jooq.Tables.NAMESPACE;

@Component
public class ExtensionJooqRepository {

    private final DSLContext dsl;

    public ExtensionJooqRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

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

    public List<Extension> findAllPublicIds() {
        return findPublicId().fetch().map(this::toPublicId);
    }

    public Extension findPublicId(String namespace, String extension) {
        var query = findPublicId();
        query.addConditions(
                DSL.upper(EXTENSION.NAME).eq(DSL.upper(extension)),
                DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespace))
        );

        var record = query.fetchOne();
        return record != null ? toPublicId(record) : null;
    }

    public Extension findPublicId(String publicId) {
        var query = findPublicId();
        query.addConditions(EXTENSION.PUBLIC_ID.eq(publicId));

        var record = query.fetchOne();
        return record != null ? toPublicId(record) : null;
    }

    public Extension findNamespacePublicId(String publicId) {
        var query = findPublicId();
        query.addConditions(NAMESPACE.PUBLIC_ID.eq(publicId));
        query.addLimit(1);

        var record = query.fetchOne();
        return record != null ? toPublicId(record) : null;
    }

    private SelectQuery<Record> findPublicId() {
        var query = dsl.selectQuery();
        query.addSelect(
                EXTENSION.ID,
                EXTENSION.PUBLIC_ID,
                EXTENSION.NAME,
                NAMESPACE.ID,
                NAMESPACE.PUBLIC_ID,
                NAMESPACE.NAME
        );

        query.addFrom(EXTENSION);
        query.addJoin(NAMESPACE, NAMESPACE.ID.eq(EXTENSION.NAMESPACE_ID));
        return query;
    }

    private Extension toPublicId(Record record) {
        var namespace = new Namespace();
        namespace.setId(record.get(NAMESPACE.ID));
        namespace.setPublicId(record.get(NAMESPACE.PUBLIC_ID));
        namespace.setName(record.get(NAMESPACE.NAME));

        var extension = new Extension();
        extension.setId(record.get(EXTENSION.ID));
        extension.setPublicId(record.get(EXTENSION.PUBLIC_ID));
        extension.setName(record.get(EXTENSION.NAME));
        extension.setNamespace(namespace);
        return extension;
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
                NAMESPACE.NAME,
                NAMESPACE.DISPLAY_NAME
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
        namespace.setDisplayName(record.get(NAMESPACE.DISPLAY_NAME));
        extension.setNamespace(namespace);

        return extension;
    }

    public void updatePublicId(long id, String publicId) {
        dsl.update(EXTENSION)
                .set(EXTENSION.PUBLIC_ID, publicId)
                .where(EXTENSION.ID.eq(id))
                .execute();
    }

    public void updatePublicIds(Map<Long, String> publicIds) {
        if(publicIds.isEmpty()) {
            return;
        }

        var extension = EXTENSION.as("e");
        var rows = publicIds.entrySet().stream()
                .map(e -> DSL.row(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        var updates = DSL.values(rows.toArray(Row2[]::new)).as("u", "id", "public_id");
        dsl.update(extension)
                .set(extension.PUBLIC_ID, updates.field("public_id", String.class))
                .from(updates)
                .where(updates.field("id", Long.class).eq(extension.ID))
                .execute();
    }

    public boolean publicIdExists(String publicId) {
        return dsl.selectOne()
                .from(EXTENSION)
                .where(EXTENSION.PUBLIC_ID.eq(publicId))
                .fetch()
                .isNotEmpty();
    }

    public List<SitemapRow> fetchSitemapRows() {
        var LAST_UPDATED = DSL.toChar(EXTENSION.LAST_UPDATED_DATE, "YYYY-MM-DD");
        return dsl.select(
                    NAMESPACE.NAME,
                    EXTENSION.NAME,
                    LAST_UPDATED
                )
                .from(NAMESPACE)
                .join(EXTENSION).on(EXTENSION.NAMESPACE_ID.eq(NAMESPACE.ID))
                .where(EXTENSION.ACTIVE.eq(true))
                .fetch()
                .map((record) -> {
                    return new SitemapRow(
                            record.get(NAMESPACE.NAME),
                            record.get(EXTENSION.NAME),
                            record.get(LAST_UPDATED)
                    );
                });
    }
}
