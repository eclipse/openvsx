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

import org.eclipse.openvsx.dto.ExtensionVersionDTO;
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
public class ExtensionVersionDTORepository {

    @Autowired
    DSLContext dsl;

    public List<ExtensionVersionDTO> findAllActiveByExtensionId(Collection<Long> extensionIds) {
        return dsl.select(
                    EXTENSION_VERSION.EXTENSION_ID,
                    EXTENSION_VERSION.ID,
                    EXTENSION_VERSION.VERSION,
                    EXTENSION_VERSION.PREVIEW,
                    EXTENSION_VERSION.PRE_RELEASE,
                    EXTENSION_VERSION.TIMESTAMP,
                    EXTENSION_VERSION.DISPLAY_NAME,
                    EXTENSION_VERSION.DESCRIPTION,
                    EXTENSION_VERSION.ENGINES,
                    EXTENSION_VERSION.CATEGORIES,
                    EXTENSION_VERSION.TAGS,
                    EXTENSION_VERSION.EXTENSION_KIND,
                    EXTENSION_VERSION.REPOSITORY,
                    EXTENSION_VERSION.GALLERY_COLOR,
                    EXTENSION_VERSION.GALLERY_THEME,
                    EXTENSION_VERSION.DEPENDENCIES,
                    EXTENSION_VERSION.BUNDLED_EXTENSIONS
                )
                .from(EXTENSION_VERSION)
                .where(EXTENSION_VERSION.ACTIVE.eq(true))
                .and(EXTENSION_VERSION.EXTENSION_ID.in(extensionIds))
                .fetchInto(ExtensionVersionDTO.class);
    }

    List<ExtensionVersionDTO> findAllActiveByExtensionPublicId(String extensionPublicId) {
        return fetch(findAllActive().and(EXTENSION.PUBLIC_ID.eq(extensionPublicId)));
    }

    List<ExtensionVersionDTO> findAllActiveByNamespacePublicId(String namespacePublicId) {
        return fetch(findAllActive().and(NAMESPACE.PUBLIC_ID.eq(namespacePublicId)));
    }

    ExtensionVersionDTO findActiveByVersionAndExtensionNameAndNamespaceName(String extensionVersion, String extensionName, String namespaceName) {
        return findAllActive()
                .and(EXTENSION_VERSION.VERSION.eq(extensionVersion))
                .and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(extensionName)))
                .and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName)))
                .fetchOneInto(ExtensionVersionDTO.class);
    }

    List<ExtensionVersionDTO> findAllActiveByExtensionNameAndNamespaceName(String extensionName, String namespaceName) {
        var query = findAllActive()
                .and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(extensionName)))
                .and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName)));

        return fetch(query);
    }

    List<ExtensionVersionDTO> findAllActiveByNamespaceName(String namespaceName) {
        return fetch(findAllActive().and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName))));
    }

    List<ExtensionVersionDTO> findAllActiveByExtensionName(String extensionName) {
        return fetch(findAllActive().and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(extensionName))));
    }

    public Map<Long, List<String>> getVersionStrings(Collection<Long> extensionIds, boolean onlyIncludeActive) {
        var query = dsl.select(EXTENSION_VERSION.EXTENSION_ID, EXTENSION_VERSION.VERSION)
                .from(EXTENSION_VERSION)
                .join(EXTENSION).on(EXTENSION.ID.eq(EXTENSION_VERSION.EXTENSION_ID))
                .where(EXTENSION.ID.in(extensionIds));

        if(onlyIncludeActive) {
            query = query.and(EXTENSION_VERSION.ACTIVE.eq(true));
        }

        return query.orderBy(EXTENSION_VERSION.TIMESTAMP.desc())
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(EXTENSION_VERSION.EXTENSION_ID),
                        Collectors.mapping(r -> r.get(EXTENSION_VERSION.VERSION), Collectors.toList())));
    }

    private SelectConditionStep<Record> findAllActive() {
        return dsl.select(
                    NAMESPACE.ID,
                    NAMESPACE.PUBLIC_ID,
                    NAMESPACE.NAME,
                    EXTENSION.ID,
                    EXTENSION.PUBLIC_ID,
                    EXTENSION.NAME,
                    EXTENSION.LATEST_ID,
                    EXTENSION.LATEST_PRE_RELEASE_ID,
                    EXTENSION.AVERAGE_RATING,
                    EXTENSION.DOWNLOAD_COUNT,
                    USER_DATA.ID,
                    USER_DATA.LOGIN_NAME,
                    USER_DATA.FULL_NAME,
                    USER_DATA.AVATAR_URL,
                    USER_DATA.PROVIDER_URL,
                    USER_DATA.PROVIDER,
                    EXTENSION_VERSION.ID,
                    EXTENSION_VERSION.VERSION,
                    EXTENSION_VERSION.PREVIEW,
                    EXTENSION_VERSION.PRE_RELEASE,
                    EXTENSION_VERSION.TIMESTAMP,
                    EXTENSION_VERSION.DISPLAY_NAME,
                    EXTENSION_VERSION.DESCRIPTION,
                    EXTENSION_VERSION.ENGINES,
                    EXTENSION_VERSION.CATEGORIES,
                    EXTENSION_VERSION.TAGS,
                    EXTENSION_VERSION.EXTENSION_KIND,
                    EXTENSION_VERSION.LICENSE,
                    EXTENSION_VERSION.HOMEPAGE,
                    EXTENSION_VERSION.REPOSITORY,
                    EXTENSION_VERSION.BUGS,
                    EXTENSION_VERSION.MARKDOWN,
                    EXTENSION_VERSION.GALLERY_COLOR,
                    EXTENSION_VERSION.GALLERY_THEME,
                    EXTENSION_VERSION.QNA,
                    EXTENSION_VERSION.DEPENDENCIES,
                    EXTENSION_VERSION.BUNDLED_EXTENSIONS
                )
                .from(EXTENSION_VERSION)
                .join(EXTENSION).on(EXTENSION.ID.eq(EXTENSION_VERSION.EXTENSION_ID))
                .join(NAMESPACE).on(NAMESPACE.ID.eq(EXTENSION.NAMESPACE_ID))
                .leftJoin(PERSONAL_ACCESS_TOKEN).on(PERSONAL_ACCESS_TOKEN.ID.eq(EXTENSION_VERSION.PUBLISHED_WITH_ID))
                .join(USER_DATA).on(USER_DATA.ID.eq(PERSONAL_ACCESS_TOKEN.USER_DATA))
                .where(EXTENSION_VERSION.ACTIVE.eq(true));
    }

    private List<ExtensionVersionDTO> fetch(SelectConditionStep<Record> query) {
        return query.fetchInto(ExtensionVersionDTO.class);
    }
}
