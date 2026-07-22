/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TargetPlatformVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the soft-delete (immutable version) feature in {@link ExtensionService}.
 * <p>
 * A "deleted" extension version is soft-deleted: its row is kept as a permanent tombstone (marked
 * {@code removed} and inactive, with its files stripped from storage) so the version identity stays
 * reserved and can never be republished. Only a purge physically removes the row and frees the identity.
 * These tests exercise the end-to-end behaviour against a real database, as well as the query paths that
 * must exclude tombstones from public surfaces.
 */
@SpringBootTest(
    properties = {
        "ovsx.elasticsearch.enabled=false"
    }
)
@ActiveProfiles("test_db")
class ExtensionSoftDeleteTest {

    private static final String NAMESPACE = "soft-delete-testns";
    private static final String EXTENSION = "soft-delete-testext";
    private static final String OWNER_LOGIN = "soft-delete-owner";

    @Autowired
    ExtensionService extensionService;

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager txManager;

    @MockitoBean
    SearchUtilService search;

    @MockitoBean
    JobRequestScheduler scheduler;

    private long extensionId;
    private long ownerId;
    private long ownerTokenId;

    @BeforeEach
    void setUp() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var owner = new UserData();
            owner.setLoginName(OWNER_LOGIN);
            em.persist(owner);

            var token = new PersonalAccessToken();
            token.setUser(owner);
            token.setValue(OWNER_LOGIN + "_token");
            token.setCreatedTimestamp(LocalDateTime.now());
            token.setActive(true);
            em.persist(token);

            var namespace = new Namespace();
            namespace.setName(NAMESPACE);
            em.persist(namespace);

            var extension = new Extension();
            extension.setName(EXTENSION);
            extension.setNamespace(namespace);
            extension.setActive(true);
            em.persist(extension);
            em.flush();

            ownerId = owner.getId();
            ownerTokenId = token.getId();
            extensionId = extension.getId();
        });
    }

    private UserData owner() {
        var owner = new UserData();
        owner.setId(ownerId);
        owner.setLoginName(OWNER_LOGIN);
        return owner;
    }

    /**
     * Deleting a version while another exists soft-deletes it: the row survives as a tombstone that is
     * marked removed, inactive, and stamped with the deleting user and a timestamp.
     */
    @Test
    void deleteExtensionVersion_softDeletesKeepingTombstone() {
        persistVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);
        persistVersion("2.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);

        var targets = TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0");
        extensionService.deleteExtension(owner(), false, NAMESPACE, EXTENSION, targets);

        assertThat(versionExists("1.0.0"))
                .as("a soft-deleted version's row must be kept as an immutable tombstone")
                .isTrue();
        assertThat(versionRemoved("1.0.0"))
                .as("a soft-deleted version must be marked removed and inactive")
                .isTrue();
        assertThat(removedByOf("1.0.0"))
                .as("a soft-deleted version records who removed it")
                .isEqualTo(ownerId);
        assertThat(removedTimestampOf("1.0.0"))
                .as("a soft-deleted version records when it was removed")
                .isNotNull();
        assertThat(versionRemoved("2.0.0"))
                .as("an untouched version must stay live")
                .isFalse();
    }

    /**
     * Deleting an already-removed version is an idempotent no-op: it neither fails nor re-stamps the
     * tombstone (the original removal metadata is preserved).
     */
    @Test
    void deleteExtensionVersion_isIdempotentForAlreadyRemoved() {
        persistVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);
        persistVersion("2.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);

        var targets = TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0");
        extensionService.deleteExtension(owner(), false, NAMESPACE, EXTENSION, targets);
        var firstTimestamp = removedTimestampOf("1.0.0");

        // A second delete of the same version must not throw and must not touch the tombstone.
        extensionService.deleteExtension(owner(), false, NAMESPACE, EXTENSION, targets);

        assertThat(versionRemoved("1.0.0")).isTrue();
        assertThat(removedTimestampOf("1.0.0"))
                .as("re-deleting a tombstone must not re-stamp its removal timestamp")
                .isEqualTo(firstTimestamp);
    }

    /**
     * Purging permanently removes the version row (unlike soft-delete), freeing the identity.
     */
    @Test
    void purgeExtensionVersion_physicallyRemovesRow() {
        persistVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);
        persistVersion("2.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);

        var targets = TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0");
        extensionService.purgeExtension(owner(), false, NAMESPACE, EXTENSION, targets);

        assertThat(versionExists("1.0.0"))
                .as("a purged version's row must be physically removed")
                .isFalse();
        assertThat(versionExists("2.0.0"))
                .as("an untouched version must survive the purge")
                .isTrue();
    }

    /**
     * Soft-deleting then purging the same version leaves no row behind, freeing the identity.
     */
    @Test
    void softDeleteThenPurge_removesTombstone() {
        persistVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);
        persistVersion("2.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);

        var targets = TargetPlatformVersion.of(TargetPlatform.NAME_UNIVERSAL, "1.0.0");
        extensionService.deleteExtension(owner(), false, NAMESPACE, EXTENSION, targets);
        assertThat(versionRemoved("1.0.0")).isTrue();

        extensionService.purgeExtension(owner(), false, NAMESPACE, EXTENSION, targets);
        assertThat(versionExists("1.0.0"))
                .as("purging a tombstone must physically remove its row")
                .isFalse();
    }

    /**
     * Fix #2: the version listing used by the public {@code /versions} API must never surface tombstones.
     */
    @Test
    void findActiveVersionsSorted_excludesRemovedVersions() {
        persistVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);
        persistVersion("2.0.0", TargetPlatform.NAME_UNIVERSAL, false, true);

        var page = repositories.findActiveVersionsSorted(NAMESPACE, EXTENSION, PageRequest.of(0, 10));
        var versions = page.getContent().stream().map(ExtensionVersion::getVersion).toList();

        assertThat(versions)
                .as("the public versions listing must exclude soft-deleted versions")
                .contains("1.0.0")
                .doesNotContain("2.0.0");
    }

    /**
     * Fix #3: the download URL map must never surface tombstones (whose files are gone anyway).
     */
    @Test
    void findVersionsForUrls_excludesRemovedVersions() {
        // Same version string, two target platforms: the universal one is live, the linux one is removed.
        persistVersion("1.0.0", TargetPlatform.NAME_UNIVERSAL, true, false);
        persistVersion("1.0.0", TargetPlatform.NAME_LINUX_X64, false, true);

        var extension = repositories.findExtension(EXTENSION, NAMESPACE);
        var forUrls = repositories.findVersionsForUrls(extension, null, "1.0.0");
        var platforms = forUrls.stream().map(ExtensionVersion::getTargetPlatform).toList();

        assertThat(platforms)
                .as("the download map must only include active target platforms, never tombstones")
                .containsExactly(TargetPlatform.NAME_UNIVERSAL);
    }

    private void persistVersion(String version, String targetPlatform, boolean active, boolean removed) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var extension = em.find(Extension.class, extensionId);
            var token = em.getReference(PersonalAccessToken.class, ownerTokenId);
            var extVersion = new ExtensionVersion();
            extVersion.setVersion(version);
            extVersion.setTargetPlatform(targetPlatform);
            extVersion.setExtension(extension);
            extVersion.setPublishedWith(token);
            extVersion.setActive(active);
            extVersion.setRemoved(removed);
            if (removed) {
                extVersion.setActive(false);
                extVersion.setRemovedTimestamp(LocalDateTime.now());
                extVersion.setRemovedBy(em.getReference(UserData.class, ownerId));
            }
            em.persist(extVersion);
        });
    }

    private boolean versionExists(String version) {
        return count(
                "select ev.id from ExtensionVersion ev where ev.version = :version "
                        + "and ev.extension.namespace.name = :namespace",
                version) > 0;
    }

    private boolean versionRemoved(String version) {
        return count(
                "select ev.id from ExtensionVersion ev where ev.version = :version "
                        + "and ev.extension.namespace.name = :namespace and ev.removed = true and ev.active = false",
                version) > 0;
    }

    private Long removedByOf(String version) {
        return new TransactionTemplate(txManager).execute(
                status -> em.createQuery(
                        "select ev.removedBy.id from ExtensionVersion ev where ev.version = :version "
                                + "and ev.extension.namespace.name = :namespace",
                        Long.class)
                        .setParameter("version", version)
                        .setParameter("namespace", NAMESPACE)
                        .getSingleResult());
    }

    private LocalDateTime removedTimestampOf(String version) {
        return new TransactionTemplate(txManager).execute(
                status -> em.createQuery(
                        "select ev.removedTimestamp from ExtensionVersion ev where ev.version = :version "
                                + "and ev.extension.namespace.name = :namespace",
                        LocalDateTime.class)
                        .setParameter("version", version)
                        .setParameter("namespace", NAMESPACE)
                        .getSingleResult());
    }

    private long count(String jpql, String version) {
        return new TransactionTemplate(txManager).execute(
                status -> (long) em.createQuery(jpql)
                        .setParameter("version", version)
                        .setParameter("namespace", NAMESPACE)
                        .getResultList()
                        .size());
    }

    @AfterEach
    void tearDown() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            em.createQuery("delete from ExtensionVersion ev where ev.extension.namespace.name = :namespace")
                    .setParameter("namespace", NAMESPACE).executeUpdate();
            em.createQuery("delete from Extension e where e.namespace.name = :namespace")
                    .setParameter("namespace", NAMESPACE).executeUpdate();
            em.createQuery("delete from PersistedLog pl where pl.user.loginName = :login")
                    .setParameter("login", OWNER_LOGIN).executeUpdate();
            em.createQuery("delete from PersonalAccessToken t where t.user.loginName = :login")
                    .setParameter("login", OWNER_LOGIN).executeUpdate();
            em.createQuery("delete from Namespace n where n.name = :namespace")
                    .setParameter("namespace", NAMESPACE).executeUpdate();
            em.createQuery("delete from UserData u where u.loginName = :login")
                    .setParameter("login", OWNER_LOGIN).executeUpdate();
        });
    }
}
