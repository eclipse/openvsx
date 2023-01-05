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

import org.eclipse.openvsx.entities.*;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.eclipse.openvsx.jooq.Tables.*;

@Component
public class ExtensionVersionJooqRepository {

    @Autowired
    DSLContext dsl;

    public List<ExtensionVersion> findAllActiveByExtensionIdAndTargetPlatform(Collection<Long> extensionIds, String targetPlatform) {
        var query = dsl.select(
                    NAMESPACE.ID,
                    NAMESPACE.NAME,
                    EXTENSION.ID,
                    EXTENSION.NAME,
                    EXTENSION_VERSION.ID,
                    EXTENSION_VERSION.VERSION,
                    EXTENSION_VERSION.TARGET_PLATFORM,
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
                    EXTENSION_VERSION.LOCALIZED_LANGUAGES,
                    EXTENSION_VERSION.DEPENDENCIES,
                    EXTENSION_VERSION.BUNDLED_EXTENSIONS
                )
                .from(EXTENSION_VERSION)
                .join(EXTENSION).on(EXTENSION.ID.eq(EXTENSION_VERSION.EXTENSION_ID))
                .join(NAMESPACE).on(NAMESPACE.ID.eq(EXTENSION.NAMESPACE_ID))
                .where(EXTENSION_VERSION.ACTIVE.eq(true))
                .and(EXTENSION_VERSION.EXTENSION_ID.in(extensionIds));

        if(targetPlatform != null) {
            query = query.and(EXTENSION_VERSION.TARGET_PLATFORM.eq(targetPlatform));
        }

        return query.fetch().map(this::toExtensionVersion);
    }

    public List<ExtensionVersion> findAllActiveByExtensionPublicId(String targetPlatform, String extensionPublicId) {
        var query = findAllActive().and(EXTENSION.PUBLIC_ID.eq(extensionPublicId));
        if(targetPlatform != null) {
            query = query.and(EXTENSION_VERSION.TARGET_PLATFORM.eq(targetPlatform));
        }

        return fetch(query);
    }

    public List<ExtensionVersion> findAllActiveByNamespacePublicId(String targetPlatform, String namespacePublicId) {
        var query = findAllActive().and(NAMESPACE.PUBLIC_ID.eq(namespacePublicId));
        if(targetPlatform != null) {
            query = query.and(EXTENSION_VERSION.TARGET_PLATFORM.eq(targetPlatform));
        }

        return fetch(query);
    }

    public List<ExtensionVersion> findAllActiveByVersionAndExtensionNameAndNamespaceName(String version, String extensionName, String namespaceName) {
        var query = findAllActive()
                .and(EXTENSION_VERSION.VERSION.eq(version))
                .and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(extensionName)))
                .and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName)));

        return fetch(query);
    }

    public List<ExtensionVersion> findAllActiveByExtensionNameAndNamespaceName(String targetPlatform, String extensionName, String namespaceName) {
        var query = findAllActive()
                .and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(extensionName)))
                .and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName)));

        if(targetPlatform != null) {
            query = query.and(EXTENSION_VERSION.TARGET_PLATFORM.eq(targetPlatform));
        }

        return fetch(query);
    }

    public List<ExtensionVersion> findAllActiveByNamespaceName(String targetPlatform, String namespaceName) {
        var query = findAllActive().and(DSL.upper(NAMESPACE.NAME).eq(DSL.upper(namespaceName)));
        if(targetPlatform != null) {
            query = query.and(EXTENSION_VERSION.TARGET_PLATFORM.eq(targetPlatform));
        }

        return fetch(query);
    }

    List<ExtensionVersion> findAllActiveByExtensionName(String targetPlatform, String extensionName) {
        var query = findAllActive().and(DSL.upper(EXTENSION.NAME).eq(DSL.upper(extensionName)));
        if(targetPlatform != null) {
            query = query.and(EXTENSION_VERSION.TARGET_PLATFORM.eq(targetPlatform));
        }

        return fetch(query);
    }

    private SelectConditionStep<Record> findAllActive() {
        return dsl.select(
                    NAMESPACE.ID,
                    NAMESPACE.PUBLIC_ID,
                    NAMESPACE.NAME,
                    EXTENSION.ID,
                    EXTENSION.PUBLIC_ID,
                    EXTENSION.NAME,
                    EXTENSION.AVERAGE_RATING,
                    EXTENSION.DOWNLOAD_COUNT,
                    EXTENSION.PUBLISHED_DATE,
                    EXTENSION.LAST_UPDATED_DATE,
                    USER_DATA.ID,
                    USER_DATA.ROLE,
                    USER_DATA.LOGIN_NAME,
                    USER_DATA.FULL_NAME,
                    USER_DATA.AVATAR_URL,
                    USER_DATA.PROVIDER_URL,
                    USER_DATA.PROVIDER,
                    EXTENSION_VERSION.ID,
                    EXTENSION_VERSION.VERSION,
                    EXTENSION_VERSION.TARGET_PLATFORM,
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
                    EXTENSION_VERSION.LOCALIZED_LANGUAGES,
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

    private List<ExtensionVersion> fetch(SelectConditionStep<Record> query) {
        return query.fetch().map(this::toExtensionVersionFull);
    }

    private ExtensionVersion toExtensionVersionFull(Record record) {
        var extVersion = toExtensionVersionCommon(record);
        extVersion.setLicense(record.get(EXTENSION_VERSION.LICENSE));
        extVersion.setHomepage(record.get(EXTENSION_VERSION.HOMEPAGE));
        extVersion.setBugs(record.get(EXTENSION_VERSION.BUGS));
        extVersion.setMarkdown(record.get(EXTENSION_VERSION.MARKDOWN));
        extVersion.setQna(record.get(EXTENSION_VERSION.QNA));

        var extension = extVersion.getExtension();
        extension.setPublicId(record.get(EXTENSION.PUBLIC_ID));
        extension.setAverageRating(record.get(EXTENSION.AVERAGE_RATING));
        extension.setDownloadCount(record.get(EXTENSION.DOWNLOAD_COUNT));
        extension.setPublishedDate(record.get(EXTENSION.PUBLISHED_DATE));
        extension.setLastUpdatedDate(record.get(EXTENSION.LAST_UPDATED_DATE));

        var namespace = extension.getNamespace();
        namespace.setPublicId(record.get(NAMESPACE.PUBLIC_ID));

        var user = new UserData();
        user.setId(record.get(USER_DATA.ID));
        user.setLoginName(record.get(USER_DATA.LOGIN_NAME));
        user.setFullName(record.get(USER_DATA.FULL_NAME));
        user.setAvatarUrl(record.get(USER_DATA.AVATAR_URL));
        user.setProviderUrl(record.get(USER_DATA.PROVIDER_URL));
        user.setProvider(record.get(USER_DATA.PROVIDER));

        var token = new PersonalAccessToken();
        token.setUser(user);

        extVersion.setPublishedWith(token);
        extVersion.setType(ExtensionVersion.Type.REGULAR);
        return extVersion;
    }

    private ExtensionVersion toExtensionVersion(Record record) {
        var extVersion = toExtensionVersionCommon(record);
        extVersion.setType(ExtensionVersion.Type.MINIMAL);
        return extVersion;
    }

    private ExtensionVersion toExtensionVersionCommon(Record record) {
        var converter = new ListOfStringConverter();

        var extVersion = new ExtensionVersion();
        extVersion.setId(record.get(EXTENSION_VERSION.ID));
        extVersion.setVersion(record.get(EXTENSION_VERSION.VERSION));
        extVersion.setTargetPlatform(record.get(EXTENSION_VERSION.TARGET_PLATFORM));
        extVersion.setPreview(record.get(EXTENSION_VERSION.PREVIEW));
        extVersion.setPreRelease(record.get(EXTENSION_VERSION.PRE_RELEASE));
        extVersion.setTimestamp(record.get(EXTENSION_VERSION.TIMESTAMP));
        extVersion.setDisplayName(record.get(EXTENSION_VERSION.DISPLAY_NAME));
        extVersion.setDescription(record.get(EXTENSION_VERSION.DESCRIPTION));
        extVersion.setEngines(toList(record.get(EXTENSION_VERSION.ENGINES), converter));
        extVersion.setCategories(toList(record.get(EXTENSION_VERSION.CATEGORIES), converter));
        extVersion.setTags(toList(record.get(EXTENSION_VERSION.TAGS), converter));
        extVersion.setExtensionKind(toList(record.get(EXTENSION_VERSION.EXTENSION_KIND), converter));
        extVersion.setRepository(record.get(EXTENSION_VERSION.REPOSITORY));
        extVersion.setGalleryColor(record.get(EXTENSION_VERSION.GALLERY_COLOR));
        extVersion.setGalleryTheme(record.get(EXTENSION_VERSION.GALLERY_THEME));
        extVersion.setLocalizedLanguages(toList(record.get(EXTENSION_VERSION.LOCALIZED_LANGUAGES), converter));
        extVersion.setDependencies(toList(record.get(EXTENSION_VERSION.DEPENDENCIES), converter));
        extVersion.setBundledExtensions(toList(record.get(EXTENSION_VERSION.BUNDLED_EXTENSIONS), converter));

        var extension = new Extension();
        extension.setId(record.get(EXTENSION.ID));
        extension.setName(record.get(EXTENSION.NAME));
        extVersion.setExtension(extension);

        var namespace = new Namespace();
        namespace.setId(record.get(NAMESPACE.ID));
        namespace.setName(record.get(NAMESPACE.NAME));
        extension.setNamespace(namespace);

        return extVersion;
    }

    private List<String> toList(String raw, ListOfStringConverter converter) {
        return converter.convertToEntityAttribute(raw);
    }
}
