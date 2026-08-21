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
import java.util.List;
import java.util.stream.Collectors;

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

    private UserData owner;

    private PersonalAccessToken token;

    @BeforeEach
    void setUp() {
        owner = new UserData();
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

    @Test
    void capsPreReleaseVersionsButKeepsAllStableVersions() {
        var extension = persistExtension("ns7", "ext7");
        persistVersion(extension, "1.0.0", TargetPlatform.NAME_UNIVERSAL, true);
        persistVersion(extension, "2.0.0", TargetPlatform.NAME_UNIVERSAL, true);
        persistPreReleaseVersions(extension, TargetPlatform.NAME_UNIVERSAL, 1, 105);

        var result = repo.findAllActiveByExtensionIdAndTargetPlatform(List.of(extension.getId()), null, 100);

        var stable = result.stream()
                .filter(ev -> !ev.isPreRelease())
                .map(ExtensionVersion::getVersion)
                .toList();
        var preRelease = result.stream()
                .filter(ExtensionVersion::isPreRelease)
                .map(ExtensionVersion::getVersion)
                .toList();

        assertThat(stable)
                .as("stable releases must never be affected by the pre-release cap")
                .containsExactlyInAnyOrder("1.0.0", "2.0.0");
        assertThat(preRelease).as("pre-releases must be capped at 100").hasSize(100);
        assertThat(preRelease)
                .as("the highest-semver pre-release ('latest') must survive the cap")
                .contains("0.0.105");
        assertThat(preRelease)
                .as("the 5 lowest-semver pre-releases must be dropped by the cap")
                .doesNotContain("0.0.1", "0.0.2", "0.0.3", "0.0.4", "0.0.5");
    }

    @Test
    void capsPreReleaseVersionsAcrossTargetPlatformsCombinedRatherThanPerPlatform() {
        var extension = persistExtension("ns8", "ext8");
        persistPreReleaseVersions(extension, TargetPlatform.NAME_LINUX_X64, 1, 60);
        persistPreReleaseVersions(extension, TargetPlatform.NAME_WIN32_X64, 61, 105);

        var result = repo.findAllActiveByExtensionIdAndTargetPlatform(List.of(extension.getId()), null, 100);
        var versions = result.stream().map(ExtensionVersion::getVersion).collect(Collectors.toSet());

        assertThat(result)
                .as("the cap counts pre-releases across all target platforms of the extension combined")
                .hasSize(100);
        for (var patch = 6; patch <= 105; patch++) {
            assertThat(versions).contains("0.0." + patch);
        }
        for (var patch = 1; patch <= 5; patch++) {
            assertThat(versions).doesNotContain("0.0." + patch);
        }
    }

    @Test
    void targetPlatformFilterIsAppliedAfterTheCrossPlatformPreReleaseCap() {
        // 99 higher-semver win32 pre-releases crowd out all but the newest of the 2 linux
        // pre-releases from the cross-platform top-100 cap, before the win32-x64 filter below is
        // even applied - so requesting linux-x64 only can see fewer than 100 versions even though
        // more than 100 exist for the extension overall.
        var extension = persistExtension("ns9", "ext9");
        persistPreReleaseVersions(extension, TargetPlatform.NAME_WIN32_X64, 1, 99, "1.0.");
        persistPreReleaseVersions(extension, TargetPlatform.NAME_LINUX_X64, 1, 2);

        var linuxOnly = repo.findAllActiveByExtensionIdAndTargetPlatform(
                List.of(extension.getId()),
                TargetPlatform.NAME_LINUX_X64,
                100);

        assertThat(linuxOnly).extracting(ExtensionVersion::getVersion).containsExactly("0.0.2");
    }

    @Test
    void honoursTheCallerSuppliedPreReleaseCapInsteadOfAFixedOne() {
        var extension = persistExtension("ns10", "ext10");
        persistPreReleaseVersions(extension, TargetPlatform.NAME_UNIVERSAL, 1, 5);

        var result = repo.findAllActiveByExtensionIdAndTargetPlatform(List.of(extension.getId()), null, 3);

        assertThat(result)
                .as("the cap is whatever the caller passes in, not a value fixed inside the repository")
                .extracting(ExtensionVersion::getVersion)
                .containsExactlyInAnyOrder("0.0.3", "0.0.4", "0.0.5");
    }

    @Test
    void tiedMajorMinorPatchPreReleasesAreStillCappedViaTheTimestampTiebreak() {
        // All 105 rows share the exact same (major, minor, patch) - the version string itself is
        // identical - and differ only in target platform and timestamp, exactly like one release
        // published across many platform builds, or a CI channel that republishes without ever
        // bumping semver. On a (major, minor, patch)-only comparison none of these rows outranks
        // any other, so none would count against the cap and all 105 would come back uncapped.
        var extension = persistExtension("ns12", "ext12");
        var base = LocalDateTime.parse("2024-01-01T00:00:00");
        for (var i = 1; i <= 105; i++) {
            persistPreReleaseVersion(extension, "0.0.1", "platform-" + i, base.plusMinutes(i));
        }
        em.flush();

        var result = repo.findAllActiveByExtensionIdAndTargetPlatform(List.of(extension.getId()), null, 100);

        assertThat(result)
                .as("rows tied on (major, minor, patch) must still be capped, via the timestamp/id tiebreak")
                .hasSize(100);
        assertThat(result)
                .as("the most recently timestamped tied row ('latest') must survive the cap")
                .anyMatch(ev -> ev.getTargetPlatform().equals("platform-105"));
        assertThat(result)
                .as("the 5 earliest-timestamped tied rows must be dropped by the cap")
                .noneMatch(
                        ev -> List.of("platform-1", "platform-2", "platform-3", "platform-4", "platform-5")
                                .contains(ev.getTargetPlatform()));
    }

    @Test
    void negativeMaxPreReleaseVersionsDisablesTheCapEntirely() {
        // A negative "count < limit" can never hold, so a naive reading of the cap would exclude
        // *every* pre-release rather than none - the opposite of "unlimited". This pins down that
        // -1 (and negative values generally) instead skip the cap condition altogether, matching
        // the "negative means unlimited" convention already used elsewhere in this codebase (e.g.
        // ovsx.data.mirror.requests-per-second).
        var extension = persistExtension("ns11", "ext11");
        persistPreReleaseVersions(extension, TargetPlatform.NAME_UNIVERSAL, 1, 105);

        var result = repo.findAllActiveByExtensionIdAndTargetPlatform(List.of(extension.getId()), null, -1);

        assertThat(result).as("a negative cap must return every active pre-release, uncapped").hasSize(105);
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
        extVersion.setPublishedBy(owner);
        em.persist(extVersion);

        // ExtensionVersionJooqRepository queries run over the transaction's raw JDBC connection,
        // bypassing the persistence context, so pending inserts must be flushed before they become
        // visible to it.
        em.flush();
    }

    /**
     * Persists active pre-release versions {@code <versionPrefix><fromPatch>} .. {@code
     * <versionPrefix><toPatch>} (inclusive), one flush for the whole batch rather than one per row.
     */
    private void persistPreReleaseVersions(
            Extension extension,
            String targetPlatform,
            int fromPatch,
            int toPatch,
            String versionPrefix
    ) {
        for (var patch = fromPatch; patch <= toPatch; patch++) {
            var extVersion = new ExtensionVersion();
            extVersion.setExtension(extension);
            extVersion.setVersion(versionPrefix + patch);
            extVersion.setTargetPlatform(targetPlatform);
            extVersion.setActive(true);
            extVersion.setPreRelease(true);
            extVersion.setPublishedBy(owner);
            em.persist(extVersion);
        }
        em.flush();
    }

    private void persistPreReleaseVersions(Extension extension, String targetPlatform, int fromPatch, int toPatch) {
        persistPreReleaseVersions(extension, targetPlatform, fromPatch, toPatch, "0.0.");
    }

    private void persistPreReleaseVersion(
            Extension extension,
            String version,
            String targetPlatform,
            LocalDateTime timestamp
    ) {
        var extVersion = new ExtensionVersion();
        extVersion.setExtension(extension);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(targetPlatform);
        extVersion.setActive(true);
        extVersion.setPreRelease(true);
        extVersion.setTimestamp(timestamp);
        extVersion.setPublishedBy(owner);
        em.persist(extVersion);
    }
}
