package org.eclipse.openvsx.repositories;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Run the DB queries and assert no DB error, just to ensure that the queries
 * are consistent with the schema.
 */
class RepositoryServiceSmokeTest extends PostgresDbRepositoryIntegrationTest {

    private static final List<String> STRING_LIST = List.of("id1", "id2");

    private static final List<Long> LONG_LIST = List.of(1L, 2L);

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager em;

    @Test
    @Transactional
    void test_execute_queries() {

        // some queries require attached entities:
        Extension extension = new Extension();
        Namespace namespace = new Namespace();
        extension.setNamespace(namespace);
        UserData userData = new UserData();
        ExtensionVersion extVersion = new ExtensionVersion();
        extVersion.setTargetPlatform("targetPlatform");
        PersonalAccessToken personalAccessToken = new PersonalAccessToken();
        Stream.of(extension, namespace, userData, extVersion, personalAccessToken).forEach(em::persist);
        em.flush();

        // record executed queries
        var methodsToBeCalled = Stream.of(repositories.getClass().getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .collect(toList());
        repositories = Mockito.spy(repositories);

        assertAll(
                () -> repositories.averageNumberOfActiveReviewsPerActiveExtension(NOW),
                () -> repositories.countActiveExtensionPublishers(NOW),
                () -> repositories.countActiveExtensionPublishersGroupedByExtensionsPublished(
                        NOW),
                () -> repositories.countActiveExtensions(NOW),
                () -> repositories.countActiveExtensionsGroupedByExtensionReviewRating(
                        NOW),
                () -> repositories.countActiveReviews(null),
                () -> repositories.countExtensions(),
                () -> repositories.countExtensions("name", "namespace"),
                () -> repositories.countMemberships(namespace, "role"),
                () -> repositories.countMemberships(userData, namespace),
                () -> repositories.countNamespaces(),
                () -> repositories.countPublishersThatClaimedNamespaceOwnership(NOW),
                () -> repositories.countUsers(),
                () -> repositories.downloadsBetween(NOW, NOW),
                () -> repositories.downloadsUntil(NOW),
                () -> repositories.findAccessToken("value"),
                () -> repositories.findAccessToken(1L),
                () -> repositories.findAccessTokens(userData),
                () -> repositories.findActiveExtensionDTO("name", "namespaceName"),
                () -> repositories.findActiveExtensionVersionDTOsByExtensionName("targetPlatform", "extensionName"),
                () -> repositories.findActiveExtensionVersionDTOsByExtensionName("targetPlatform", "extensionName",
                        "namespaceName"),
                () -> repositories.findActiveExtensionVersionDTOsByExtensionPublicId("targetPlatform",
                        "extensionPublicId"),
                () -> repositories.findActiveExtensionVersionDTOsByNamespaceName("targetPlatform", "namespaceName"),
                () -> repositories.findActiveExtensionVersionDTOsByNamespacePublicId("targetPlatform",
                        "namespacePublicId"),
                () -> repositories.findActiveExtensions(namespace),
                () -> repositories.findActiveReviews(extension),
                () -> repositories.findActiveReviews(extension, userData),
                () -> repositories.findActiveVersions(extension),
                () -> repositories.findAdminStatisticsByYearAndMonth(1997, 01),
                () -> repositories.findAllActiveExtensionDTOsById(LONG_LIST),
                () -> repositories.findAllActiveExtensionDTOsByPublicId(STRING_LIST),
                () -> repositories.findAllActiveExtensionVersionDTOs(LONG_LIST, "targetPlatform"),
                () -> repositories.findAllActiveExtensions(),
                () -> repositories.findAllActiveReviewCountsByExtensionId(LONG_LIST),
                () -> repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(LONG_LIST, STRING_LIST),
                () -> repositories.findAllNamespaceMembershipDTOs(LONG_LIST),
                () -> repositories.findAllPersistedLogs(),
                () -> repositories.findAllResourceFileResourceDTOs("namespaceName", "extensionName", "version",
                        "prefix"),
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
                () -> repositories.findFilesByType(extVersion, STRING_LIST),
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
                () -> repositories.findVersionsByAccessToken(personalAccessToken),
                () -> repositories.findVersionsByAccessToken(personalAccessToken, true),
                () -> repositories.getMaxExtensionDownloadCount(),
                () -> repositories.getOldestExtensionTimestamp());

        // check that we did not miss anything
        // (remember to add new queries also to this test)
        var invocations = Mockito.mockingDetails(repositories).getInvocations().stream()
                .map(Invocation::getMethod)
                .collect(toList());
        assertThat(invocations).containsAll(methodsToBeCalled);
    }

}
