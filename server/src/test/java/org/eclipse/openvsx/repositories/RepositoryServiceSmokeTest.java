/********************************************************************************
 * Copyright (c) 2022 Wladimir Hofmann and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.QueryRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Run the DB queries and assert no DB error, just to ensure that the queries
 * are consistent with the schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RepositoryServiceSmokeTest {

    private static final List<String> STRING_LIST = List.of("id1", "id2");

    private static final List<Long> LONG_LIST = List.of(1L, 2L);

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    void testExecuteQueries() {
        // some queries require attached entities:
        var extension = new Extension();
        var namespace = new Namespace();
        extension.setNamespace(namespace);
        var userData = new UserData();
        var extVersion = new ExtensionVersion();
        extVersion.setVersion("3.1.2-rc1+armhf");
        extVersion.setTargetPlatform("targetPlatform");
        var personalAccessToken = new PersonalAccessToken();
        var keyPair = new SignatureKeyPair();
        keyPair.setPrivateKey(new byte[0]);
        keyPair.setPublicKeyText("");
        Stream.of(extension, namespace, userData, extVersion, personalAccessToken, keyPair).forEach(em::persist);
        em.flush();

        var queryRequest = new QueryRequest();
        queryRequest.size = 1;

        // record executed queries
        var methodsToBeCalled = Stream.of(repositories.getClass().getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .collect(toList());
        repositories = Mockito.spy(repositories);

        assertAll(
                () -> repositories.averageNumberOfActiveReviewsPerActiveExtension(NOW),
                () -> repositories.countActiveExtensionPublishers(NOW),
                () -> repositories.countActiveExtensionPublishersGroupedByExtensionsPublished(NOW),
                () -> repositories.countActiveExtensions(NOW),
                () -> repositories.countActiveExtensionsGroupedByExtensionReviewRating(NOW),
                () -> repositories.countActiveReviews(null),
                () -> repositories.countExtensions(),
                () -> repositories.countExtensions("name", "namespace"),
                () -> repositories.countMemberships(namespace, "role"),
                () -> repositories.isVerified(namespace, userData),
                () -> repositories.countNamespaces(),
                () -> repositories.countPublishersThatClaimedNamespaceOwnership(NOW),
                () -> repositories.countUsers(),
                () -> repositories.downloadsBetween(NOW, NOW),
                () -> repositories.downloadsUntil(NOW),
                () -> repositories.findAccessToken("value"),
                () -> repositories.findAccessToken(1L),
                () -> repositories.findAccessTokens(userData),
                () -> repositories.findActiveExtensions(namespace),
                () -> repositories.findActiveReviews(extension),
                () -> repositories.findActiveReviews(extension, userData),
                () -> repositories.findActiveVersions(extension),
                () -> repositories.findAdminStatisticsByYearAndMonth(1997, 1),
                () -> repositories.findAllActiveExtensions(),
                () -> repositories.findAllPersistedLogs(),
                () -> repositories.findAllReviews(extension),
                () -> repositories.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(STRING_LIST),
                () -> repositories.findAllUsers(),
                () -> repositories.findBundledExtensionsReference(extension),
                () -> repositories.findDependenciesReference(extension),
                () -> repositories.findDownloadsByStorageTypeAndName("storageType", STRING_LIST),
                () -> repositories.findExtension("name", namespace),
                () -> repositories.findExtension("name", "namespace"),
                () -> repositories.findExtensionByPublicId("publicId"),
                () -> repositories.findExtensions(namespace),
                () -> repositories.findExtensions("name"),
                () -> repositories.findFileByName(extVersion, "name"),
                () -> repositories.findFileByType(extVersion, "type"),
                () -> repositories.findFileByTypeAndName(extVersion, "type", "name"),
                () -> repositories.findFiles(extVersion),
                () -> repositories.findFilesByStorageType("storageType"),
                () -> repositories.findMembership(userData, namespace),
                () -> repositories.findMemberships(namespace),
                () -> repositories.findMemberships(namespace, "role"),
                () -> repositories.findMemberships(userData, "role"),
                () -> repositories.findNamespace("name"),
                () -> repositories.findNamespaceByPublicId("publicId"),
                () -> repositories.findOrphanNamespaces(),
                () -> repositories.findPersistedLogsAfter(NOW),
                () -> repositories.findTargetPlatformVersions("version", "extensionName", "namespaceName"),
                () -> repositories.findUserByLoginName("provider", "loginName"),
                () -> repositories.findUsersByLoginNameStartingWith("loginNameStart"),
                () -> repositories.findVersion("version", "targetPlatform", extension),
                () -> repositories.findVersion("version", "targetPlatform", "extensionName", "namespace"),
                () -> repositories.findVersions(extension),
                () -> repositories.findVersions("version", extension),
                () -> repositories.findVersions(userData),
                () -> repositories.findVersionsByAccessToken(personalAccessToken, true),
                () -> repositories.getMaxExtensionDownloadCount(),
                () -> repositories.getOldestExtensionTimestamp(),
                () -> repositories.findExtensions(LONG_LIST),
                () -> repositories.findExtensions(userData),
                () -> repositories.findFilesByType(List.of(extVersion), STRING_LIST),
                () -> repositories.countVersions(extension),
                () -> repositories.topMostDownloadedExtensions(NOW, 1),
                () -> repositories.deleteFileResources(extVersion, "download"),
                () -> repositories.countActiveAccessTokens(userData),
                () -> repositories.findNotMigratedResources(),
                () -> repositories.findNotMigratedPreReleases(),
                () -> repositories.findNotMigratedRenamedDownloads(),
                () -> repositories.findNotMigratedVsixManifests(),
                () -> repositories.findNotMigratedTargetPlatforms(),
                () -> repositories.findNotMigratedSha256Checksums(),
                () -> repositories.topMostActivePublishingUsers(NOW, 1),
                () -> repositories.topNamespaceExtensions(NOW, 1),
                () -> repositories.topNamespaceExtensionVersions(NOW, 1),
                () -> repositories.findFileResourcesByExtensionVersionIdAndType(LONG_LIST, STRING_LIST),
                () -> repositories.findActiveExtensionVersionsByVersion("version", "extensionName", "namespaceName"),
                () -> repositories.findResourceFileResources(1L, "prefix"),
                () -> repositories.findActiveExtensionVersions(LONG_LIST, "targetPlatform"),
                () -> repositories.findActiveExtension("name", "namespaceName"),
                () -> repositories.findActiveExtensionsById(LONG_LIST),
                () -> repositories.findActiveExtensionsByPublicId(STRING_LIST, "namespaceName"),
                () -> repositories.findNamespaceMemberships(LONG_LIST),
                () -> repositories.findActiveExtensionVersionsByNamespaceName("targetPlatform", "namespaceName"),
                () -> repositories.findActiveExtensionVersionsByExtensionName("targetPlatform", "extensionName", "namespaceName"),
                () -> repositories.findActiveExtensionVersionsByExtensionName("targetPlatform", "extensionName"),
                () -> repositories.findAllNotMatchingByExtensionId(STRING_LIST),
                () -> repositories.getAverageReviewRating(null),
                () -> repositories.getAverageReviewRating(),
                () -> repositories.findFileResources(null),
                () -> repositories.findKeyPair(null),
                () -> repositories.findActiveKeyPair(),
                () -> repositories.findFilesByType(null),
                () -> repositories.findVersions(),
                () -> repositories.findVersionsWithout(keyPair),
                () -> repositories.deleteDownloadSigFiles(),
                () -> repositories.deleteAllKeyPairs(),
                () -> repositories.findActiveVersionsSorted("namespaceName", "extensionName", PageRequest.ofSize(1)),
                () -> repositories.findActiveVersionsSorted("namespaceName", "extensionName", "targetPlatform", PageRequest.ofSize(1)),
                () -> repositories.findActiveVersionStringsSorted("namespaceName", "extensionName", PageRequest.ofSize(1)),
                () -> repositories.findActiveVersionStringsSorted("namespaceName", "extensionName", "targetPlatform", PageRequest.ofSize(1)),
                () -> repositories.findVersionStringsSorted(extension, "targetPlatform", true),
                () -> repositories.findVersionStringsSorted(extension, "targetPlatform", true),
                () -> repositories.findActiveVersions(queryRequest),
                () -> repositories.findActiveVersionStringsSorted(LONG_LIST,"targetPlatform"),
                () -> repositories.findActiveVersionReferencesSorted(List.of(extension))
        );

        // check that we did not miss anything
        // (remember to add new queries also to this test)
        var invocations = Mockito.mockingDetails(repositories).getInvocations().stream()
                .map(Invocation::getMethod)
                .collect(toList());
        assertThat(invocations).containsAll(methodsToBeCalled);
    }
}
