/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.repositories;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.eclipse.openvsx.AbstractPostgresContainerTest;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TargetPlatformVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code isDeleteAllActiveVersions} builds its "all active versions" count query by chaining
 * {@code .and(...)} directly off {@code .join(...).on(...)}, which attaches those predicates to the
 * JOIN's ON clause rather than a WHERE clause - unlike the "actual" count query right below it, which
 * uses {@code .where(...)}. For the inner joins used here that placement doesn't change the result, but
 * it is a latent trap (e.g. if a join here were ever widened to a LEFT JOIN). These tests pin down the
 * method's current, correct behaviour - including that the "all" count is properly scoped to the given
 * namespace/extension and excludes inactive versions - so a future change to the join structure is
 * caught if it ever changes the result.
 */
@SpringBootTest
@Transactional
class ExtensionVersionJooqRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    ExtensionVersionJooqRepository repo;

    @Autowired
    EntityManager em;

    private PersonalAccessToken token;

    @BeforeEach
    void setUp() {
        var owner = new UserData();
        owner.setLoginName("jooq-repo-test-owner");
        em.persist(owner);

        token = new PersonalAccessToken();
        token.setUser(owner);
        token.setValue("jooq-repo-test-owner-token");
        token.setCreatedTimestamp(LocalDateTime.now());
        token.setActive(true);
        em.persist(token);
        em.flush();
    }

    @Test
    void returnsTrueWhenDeletingTheOnlyActiveVersion() {
        var extension = persistExtension("ns1", "ext1");
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);

        var deletesAll = repo.isDeleteAllActiveVersions(
                "ns1",
                "ext1",
                TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0"));

        assertThat(deletesAll).isTrue();
    }

    @Test
    void returnsFalseWhenOtherActiveVersionsRemain() {
        var extension = persistExtension("ns2", "ext2");
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);
        persistVersion(extension, "2.0.0", TargetPlatform.NAME_UNIVERSAL, true);

        var deletesAll = repo.isDeleteAllActiveVersions(
                "ns2",
                "ext2",
                TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0"));

        assertThat(deletesAll)
                .as("deleting only one of two active versions must not report 'delete all'")
                .isFalse();
    }

    @Test
    void returnsTrueWhenDeletingAllActiveVersionsAcrossTargetPlatforms() {
        var extension = persistExtension("ns3", "ext3");
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_WIN32_X64, true);

        var deletesAll = repo.isDeleteAllActiveVersions(
                "ns3",
                "ext3",
                TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0"),
                TargetPlatformVersion.of(TargetPlatform.NAME_WIN32_X64, "1.0.0"));

        assertThat(deletesAll).isTrue();
    }

    @Test
    void ignoresAlreadyInactiveVersionsWhenCountingAll() {
        var extension = persistExtension("ns4", "ext4");
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);
        persistVersion(extension, "0.9.0", TargetPlatform.NAME_UNIVERSAL, false);

        var deletesAll = repo.isDeleteAllActiveVersions(
                "ns4",
                "ext4",
                TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0"));

        assertThat(deletesAll)
                .as("an already-inactive version must not count toward the 'all active versions' total")
                .isTrue();
    }

    @Test
    void doesNotCountActiveVersionsOfOtherExtensionsOrNamespaces() {
        var ns5 = persistNamespace("ns5");
        var target = persistExtension(ns5, "ext5");
        persistVersion(target, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);

        // Same extension name in a different namespace, and a different extension in the same
        // namespace - neither must leak into the "all" count for ns5/ext5.
        var otherNamespace = persistNamespace("other-ns");
        var sameNameOtherNamespace = persistExtension(otherNamespace, "ext5");
        persistVersion(sameNameOtherNamespace, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);

        var otherExtensionSameNamespace = persistExtension(ns5, "other-ext");
        persistVersion(otherExtensionSameNamespace, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);

        var deletesAll = repo.isDeleteAllActiveVersions(
                "ns5",
                "ext5",
                TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0"));

        assertThat(deletesAll)
                .as("active versions of other namespaces/extensions must not inflate the 'all' count")
                .isTrue();
    }

    @Test
    void returnsFalseWhenNoTargetVersionsGiven() {
        var extension = persistExtension("ns6", "ext6");
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);

        assertThat(repo.isDeleteAllActiveVersions("ns6", "ext6")).isFalse();
    }

    private Namespace persistNamespace(String namespaceName) {
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        em.persist(namespace);
        return namespace;
    }

    private Extension persistExtension(String namespaceName, String extensionName) {
        return persistExtension(persistNamespace(namespaceName), extensionName);
    }

    private Extension persistExtension(Namespace namespace, String extensionName) {
        var extension = new Extension();
        extension.setName(extensionName);
        extension.setNamespace(namespace);
        extension.setActive(true);
        extension.setDownloadable(true);
        extension.setPublishedDate(LocalDateTime.now());
        extension.setLastUpdatedDate(LocalDateTime.now());
        em.persist(extension);
        return extension;
    }

    private void persistVersion(Extension extension, String version, String targetPlatform, boolean active) {
        var extVersion = new ExtensionVersion();
        extVersion.setExtension(extension);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(targetPlatform);
        extVersion.setActive(active);
        extVersion.setPublishedWith(token);
        em.persist(extVersion);

        // ExtensionVersionJooqRepository queries run over the transaction's raw JDBC connection,
        // bypassing the persistence context, so pending inserts must be flushed before they become
        // visible to it.
        em.flush();
    }
}
