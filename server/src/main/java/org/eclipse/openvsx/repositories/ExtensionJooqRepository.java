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
import org.eclipse.openvsx.statistics.MembershipDownloadCount;
import org.eclipse.openvsx.util.ExtensionId;
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

import static org.eclipse.openvsx.jooq.Tables.*;

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
            conditions.add(NAMESPACE.NAME.notEqualIgnoreCase(namespaceToExclude));
        }

        var query = findAllActive();
        query.addConditions(conditions);
        return fetch(query);
    }

    public Extension findActiveByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespaceName) {
        var query = findAllActive();
        query.addConditions(
                EXTENSION.NAME.equalIgnoreCase(name),
                NAMESPACE.NAME.equalIgnoreCase(namespaceName)
        );

        return query.fetchOne(this::toExtension);
    }

    public List<Extension> findAllPublicIds() {
        return findPublicId().fetch().map(this::toPublicId);
    }

    public Extension findPublicId(String namespace, String extension) {
        var query = findPublicId();
        query.addConditions(
                EXTENSION.NAME.equalIgnoreCase(extension),
                NAMESPACE.NAME.equalIgnoreCase(namespace)
        );

        return query.fetchOne(this::toPublicId);
    }

    public Extension findPublicId(String publicId) {
        var query = findPublicId();
        query.addConditions(EXTENSION.PUBLIC_ID.eq(publicId));
        return query.fetchOne(this::toPublicId);
    }

    public Extension findNamespacePublicId(String publicId) {
        var query = findPublicId();
        query.addConditions(NAMESPACE.PUBLIC_ID.eq(publicId));
        query.addLimit(1);
        return query.fetchOne(this::toPublicId);
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

    private Extension toPublicId(Record row) {
        var namespace = new Namespace();
        namespace.setId(row.get(NAMESPACE.ID));
        namespace.setPublicId(row.get(NAMESPACE.PUBLIC_ID));
        namespace.setName(row.get(NAMESPACE.NAME));

        var extension = new Extension();
        extension.setId(row.get(EXTENSION.ID));
        extension.setPublicId(row.get(EXTENSION.PUBLIC_ID));
        extension.setName(row.get(EXTENSION.NAME));
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

    private Extension toExtension(Record row) {
        var extension = new Extension();
        extension.setId(row.get(EXTENSION.ID));
        extension.setPublicId(row.get(EXTENSION.PUBLIC_ID));
        extension.setName(row.get(EXTENSION.NAME));
        extension.setAverageRating(row.get(EXTENSION.AVERAGE_RATING));
        extension.setReviewCount(row.get(EXTENSION.REVIEW_COUNT));
        extension.setDownloadCount(row.get(EXTENSION.DOWNLOAD_COUNT));
        extension.setPublishedDate(row.get(EXTENSION.PUBLISHED_DATE));
        extension.setLastUpdatedDate(row.get(EXTENSION.LAST_UPDATED_DATE));

        var namespace = new Namespace();
        namespace.setId(row.get(NAMESPACE.ID));
        namespace.setPublicId(row.get(NAMESPACE.PUBLIC_ID));
        namespace.setName(row.get(NAMESPACE.NAME));
        namespace.setDisplayName(row.get(NAMESPACE.DISPLAY_NAME));
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
                .map(row -> new SitemapRow(
                        row.get(NAMESPACE.NAME),
                        row.get(EXTENSION.NAME),
                        row.get(LAST_UPDATED)
                ));
    }

    public List<String> findActiveExtensionNames(Namespace namespace) {
        return dsl.select(EXTENSION.NAME)
                .from(EXTENSION)
                .where(EXTENSION.NAMESPACE_ID.eq(namespace.getId()))
                .and(EXTENSION.ACTIVE.eq(true))
                .orderBy(EXTENSION.NAME.asc())
                .fetch(EXTENSION.NAME);
    }

    public String findFirstUnresolvedDependency(List<ExtensionId> dependencies) {
        if(dependencies.isEmpty()) {
            return null;
        }

        var ids = DSL.values(dependencies.stream().map(d -> DSL.row(d.namespace(), d.extension())).toArray(Row2[]::new)).as("ids", "namespace", "extension");
        var namespace = ids.field("namespace", String.class);
        var extension = ids.field("extension", String.class);
        var unresolvedDependency = DSL.concat(namespace, DSL.value("."), extension).as("unresolved_dependency");
        return dsl.select(unresolvedDependency)
                .from(ids)
                .leftJoin(NAMESPACE).on(NAMESPACE.NAME.equalIgnoreCase(namespace))
                .leftJoin(EXTENSION).on(EXTENSION.NAME.equalIgnoreCase(extension))
                .where(NAMESPACE.NAME.isNull()).or(EXTENSION.NAME.isNull())
                .limit(1)
                .fetchOne(unresolvedDependency);
    }

    public List<Extension> findActiveExtensionsForUrls(Namespace namespace) {
        return dsl.select(EXTENSION.ID, EXTENSION.NAME)
                .from(EXTENSION)
                .where(EXTENSION.NAMESPACE_ID.eq(namespace.getId()))
                .and(EXTENSION.ACTIVE.eq(true))
                .fetch(row -> {
                    var extension = new Extension();
                    extension.setId(row.get(EXTENSION.ID));
                    extension.setName(row.get(EXTENSION.NAME));
                    extension.setNamespace(namespace);
                    return extension;
                });
    }

    public boolean hasExtension(String namespace, String extension) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(NAMESPACE)
                        .join(EXTENSION).on(EXTENSION.NAMESPACE_ID.eq(NAMESPACE.ID))
                        .where(NAMESPACE.NAME.equalIgnoreCase(namespace))
                        .and(EXTENSION.NAME.equalIgnoreCase(extension))
        );
    }

    public List<MembershipDownloadCount> findMembershipDownloads(int offset, int limit) {
        return dsl.select(USER_DATA.ID, NAMESPACE.NAME, EXTENSION.NAME, EXTENSION.DOWNLOAD_COUNT)
                .from(USER_DATA)
                .join(NAMESPACE_MEMBERSHIP).on(NAMESPACE_MEMBERSHIP.USER_DATA.eq(USER_DATA.ID))
                .join(NAMESPACE).on(NAMESPACE.ID.eq(NAMESPACE_MEMBERSHIP.NAMESPACE))
                .join(EXTENSION).on(EXTENSION.NAMESPACE_ID.eq(NAMESPACE.ID))
                .where(USER_DATA.PROVIDER.eq("github"))
                .and(EXTENSION.ACTIVE.eq(true))
                .orderBy(USER_DATA.ID, NAMESPACE.NAME, EXTENSION.NAME)
                .offset(offset)
                .limit(limit)
                .fetch(row -> new MembershipDownloadCount(
                        row.get(USER_DATA.ID),
                        row.get(NAMESPACE.NAME),
                        row.get(EXTENSION.NAME),
                        row.get(EXTENSION.DOWNLOAD_COUNT))
                );
    }

    public List<MembershipDownloadCount> findMembershipDownloads(String loginName) {
        return dsl.select(USER_DATA.ID, NAMESPACE.NAME, EXTENSION.NAME, EXTENSION.DOWNLOAD_COUNT)
                .from(USER_DATA)
                .join(NAMESPACE_MEMBERSHIP).on(NAMESPACE_MEMBERSHIP.USER_DATA.eq(USER_DATA.ID))
                .join(NAMESPACE).on(NAMESPACE.ID.eq(NAMESPACE_MEMBERSHIP.NAMESPACE))
                .join(EXTENSION).on(EXTENSION.NAMESPACE_ID.eq(NAMESPACE.ID))
                .where(USER_DATA.PROVIDER.eq("github"))
                .and(USER_DATA.LOGIN_NAME.eq(loginName))
                .and(EXTENSION.ACTIVE.eq(true))
                .fetch(row -> new MembershipDownloadCount(
                        row.get(USER_DATA.ID),
                        row.get(NAMESPACE.NAME),
                        row.get(EXTENSION.NAME),
                        row.get(EXTENSION.DOWNLOAD_COUNT))
                );
    }
}
