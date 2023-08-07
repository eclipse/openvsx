/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
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
import org.eclipse.openvsx.util.NamingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD_SIG;

@Component
public class RepositoryService {

    private static final int MAX_VERSIONS = 100;
    private static final Sort VERSIONS_SORT = Sort.by(Sort.Direction.DESC, "semver.major", "semver.minor", "semver.patch")
            .and(Sort.by(Sort.Direction.ASC, "semver.isPreRelease"))
            .and(Sort.by(Sort.Direction.DESC, "universalTargetPlatform"))
            .and(Sort.by(Sort.Direction.ASC, "targetPlatform"))
            .and(Sort.by(Sort.Direction.DESC, "timestamp"));

    @Autowired NamespaceRepository namespaceRepo;
    @Autowired ExtensionRepository extensionRepo;
    @Autowired ExtensionVersionRepository extensionVersionRepo;
    @Autowired FileResourceRepository fileResourceRepo;
    @Autowired ExtensionReviewRepository extensionReviewRepo;
    @Autowired UserDataRepository userDataRepo;
    @Autowired NamespaceMembershipRepository membershipRepo;
    @Autowired PersonalAccessTokenRepository tokenRepo;
    @Autowired PersistedLogRepository persistedLogRepo;
    @Autowired AzureDownloadCountProcessedItemRepository downloadCountRepo;
    @Autowired ExtensionJooqRepository extensionJooqRepo;
    @Autowired ExtensionVersionJooqRepository extensionVersionJooqRepo;
    @Autowired FileResourceJooqRepository fileResourceJooqRepo;
    @Autowired NamespaceMembershipJooqRepository membershipJooqRepo;
    @Autowired AdminStatisticsRepository adminStatisticsRepo;
    @Autowired AdminStatisticCalculationsRepository adminStatisticCalculationsRepo;
    @Autowired MigrationItemRepository migrationItemRepo;
    @Autowired SignatureKeyPairRepository signatureKeyPairRepo;

    public Namespace findNamespace(String name) {
        return namespaceRepo.findByNameIgnoreCase(name);
    }

    public Namespace findNamespaceByPublicId(String publicId) {
        return namespaceRepo.findByPublicId(publicId);
    }

    public Streamable<Namespace> findOrphanNamespaces() {
        return namespaceRepo.findOrphans();
    }

    public long countNamespaces() {
        return namespaceRepo.count();
    }

    public Extension findExtension(String name, Namespace namespace) {
        return extensionRepo.findByNameIgnoreCaseAndNamespace(name, namespace);
    }

    public Extension findExtension(String name, String namespace) {
        return extensionRepo.findByNameIgnoreCaseAndNamespaceNameIgnoreCase(name, namespace);
    }

    public Extension findExtensionByPublicId(String publicId) {
        return extensionRepo.findByPublicId(publicId);
    }

    public Streamable<Extension> findActiveExtensions(Namespace namespace) {
        return extensionRepo.findByNamespaceAndActiveTrueOrderByNameAsc(namespace);
    }

    public Streamable<Extension> findExtensions(Collection<Long> extensionIds) {
        return extensionRepo.findByIdIn(extensionIds);
    }

    public Streamable<Extension> findExtensions(Namespace namespace) {
        return extensionRepo.findByNamespace(namespace);
    }

    public Streamable<Extension> findExtensions(String name) {
        return extensionRepo.findByNameIgnoreCase(name);
    }

    public Streamable<Extension> findAllActiveExtensions() {
        return extensionRepo.findByActiveTrue();
    }

    public Streamable<Extension> findAllNotMatchingByExtensionId(List<String> extensionIds) {
        return extensionRepo.findAllNotMatchingByExtensionId(extensionIds);
    }

    public long countExtensions() {
        return extensionRepo.count();
    }

    public long countExtensions(String name, String namespace) {
        return extensionRepo.countByNameIgnoreCaseAndNamespaceNameIgnoreCase(name, namespace);
    }

    public int getMaxExtensionDownloadCount() {
        return extensionRepo.getMaxDownloadCount();
    }

    public ExtensionVersion findVersion(String version, String targetPlatform, Extension extension) {
        return extensionVersionRepo.findByVersionAndTargetPlatformAndExtension(version, targetPlatform, extension);
    }

    public ExtensionVersion findVersion(String version, String targetPlatform, String extensionName, String namespace) {
        return extensionVersionRepo.findByVersionAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(version, targetPlatform, extensionName, namespace);
    }

    public Streamable<ExtensionVersion> findVersions(Extension extension) {
         return extensionVersionRepo.findByExtension(extension);
    }

    public Streamable<ExtensionVersion> findVersions(String version, Extension extension) {
        return extensionVersionRepo.findByVersionAndExtension(version, extension);
    }

    public Streamable<ExtensionVersion> findActiveVersions(Extension extension) {
         return extensionVersionRepo.findByExtensionAndActiveTrue(extension);
    }

    public Page<ExtensionVersion> findActiveVersionsSorted(String namespace, String extension, PageRequest page) {
        return extensionVersionRepo.findByExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(extension, namespace, page.withSort(VERSIONS_SORT));
    }

    public Page<ExtensionVersion> findActiveVersionsSorted(String namespace, String extension, String targetPlatform, PageRequest page) {
        return extensionVersionRepo.findByTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(targetPlatform, extension, namespace, page.withSort(VERSIONS_SORT));
    }

    public Page<String> findActiveVersionStringsSorted(String namespace, String extension, PageRequest page) {
        return extensionVersionJooqRepo.findActiveVersionStringsSorted(namespace, extension, null, page);
    }

    public Page<String> findActiveVersionStringsSorted(String namespace, String extension, String targetPlatform, PageRequest page) {
        return extensionVersionJooqRepo.findActiveVersionStringsSorted(namespace, extension, targetPlatform, page);
    }

    public List<String> findVersionStringsSorted(Extension extension, String targetPlatform, boolean onlyActive) {
        return extensionVersionJooqRepo.findVersionStringsSorted(extension.getId(), targetPlatform, onlyActive, MAX_VERSIONS);
    }

    public Map<Long, List<String>> findActiveVersionStringsSorted(Collection<Long> extensionIds, String targetPlatform) {
        return extensionVersionJooqRepo.findActiveVersionStringsSorted(extensionIds, targetPlatform, MAX_VERSIONS);
    }

    public List<ExtensionVersion> findActiveVersionReferencesSorted(Collection<Extension> extensions) {
        return extensionVersionJooqRepo.findActiveVersionReferencesSorted(extensions, MAX_VERSIONS);
    }

    public Streamable<ExtensionVersion> findBundledExtensionsReference(Extension extension) {
        return extensionVersionRepo.findByBundledExtensions(NamingUtil.toExtensionId(extension));
    }

    public Streamable<ExtensionVersion> findDependenciesReference(Extension extension) {
        return extensionVersionRepo.findByDependencies(NamingUtil.toExtensionId(extension));
    }

    public Streamable<Extension> findExtensions(UserData user) {
        return extensionRepo.findDistinctByVersionsPublishedWithUser(user);
    }

    public Streamable<ExtensionVersion> findVersionsByAccessToken(PersonalAccessToken publishedWith, boolean active) {
        return extensionVersionRepo.findByPublishedWithAndActive(publishedWith, active);
    }

    public LocalDateTime getOldestExtensionTimestamp() {
        return extensionVersionRepo.getOldestTimestamp();
    }

    public Streamable<FileResource> findFiles(ExtensionVersion extVersion) {
        return fileResourceRepo.findByExtension(extVersion);
    }

    public Streamable<FileResource> findFilesByStorageType(String storageType) {
        return fileResourceRepo.findByStorageType(storageType);
    }

    public FileResource findFileByName(ExtensionVersion extVersion, String name) {
        return fileResourceRepo.findFirstByExtensionAndNameIgnoreCaseOrderByType(extVersion, name);
    }

    public FileResource findFileByTypeAndName(ExtensionVersion extVersion, String type, String name) {
        return fileResourceRepo.findByExtensionAndTypeAndNameIgnoreCase(extVersion, type, name);
    }

    public Streamable<FileResource> findDownloadsByStorageTypeAndName(String storageType, Collection<String> names) {
        return fileResourceRepo.findByTypeAndStorageTypeAndNameIgnoreCaseIn(DOWNLOAD, storageType, names);
    }

    public Streamable<FileResource> findFilesByType(String type) {
        return fileResourceRepo.findByType(type);
    }

    public FileResource findFileByType(ExtensionVersion extVersion, String type) {
        if(FileResource.RESOURCE.equals(type)) {
            throw new IllegalArgumentException("There are multiple files of type: " + FileResource.RESOURCE);
        }

        return fileResourceRepo.findByExtensionAndType(extVersion, type);
    }

    public List<FileResource> findFilesByType(Collection<ExtensionVersion> extVersions, Collection<String> types) {
        return fileResourceJooqRepo.findByType(extVersions, types);
    }

    public Streamable<ExtensionReview> findActiveReviews(Extension extension) {
        return extensionReviewRepo.findByExtensionAndActiveTrue(extension);
    }

    public Streamable<ExtensionReview> findAllReviews(Extension extension) {
        return extensionReviewRepo.findByExtension(extension);
    }

    public Streamable<ExtensionReview> findActiveReviews(Extension extension, UserData user) {
        return extensionReviewRepo.findByExtensionAndUserAndActiveTrue(extension, user);
    }

    public long countActiveReviews(Extension extension) {
        return extensionReviewRepo.countByExtensionAndActiveTrue(extension);
    }

    public UserData findUserByLoginName(String provider, String loginName) {
        return userDataRepo.findByProviderAndLoginName(provider, loginName);
    }

    public Streamable<UserData> findUsersByLoginNameStartingWith(String loginNameStart) {
        return userDataRepo.findByLoginNameStartingWith(loginNameStart);
    }

    public Streamable<UserData> findAllUsers() {
        return userDataRepo.findAll();
    }

    public long countUsers() {
        return userDataRepo.count();
    }

    public NamespaceMembership findMembership(UserData user, Namespace namespace) {
        return membershipRepo.findByUserAndNamespace(user, namespace);
    }

    public boolean isVerified(Namespace namespace, UserData user) {
        return membershipJooqRepo.isVerified(namespace, user);
    }

    public Streamable<NamespaceMembership> findMemberships(Namespace namespace, String role) {
        return membershipRepo.findByNamespaceAndRoleIgnoreCase(namespace, role);
    }

    public long countMemberships(Namespace namespace, String role) {
        return membershipRepo.countByNamespaceAndRoleIgnoreCase(namespace, role);
    }

    public Streamable<NamespaceMembership> findMemberships(UserData user, String role) {
        return membershipRepo.findByUserAndRoleIgnoreCaseOrderByNamespaceName(user, role);
    }

    public Streamable<NamespaceMembership> findMemberships(Namespace namespace) {
        return membershipRepo.findByNamespace(namespace);
    }

    public Streamable<PersonalAccessToken> findAccessTokens(UserData user) {
        return tokenRepo.findByUser(user);
    }

    public long countActiveAccessTokens(UserData user) {
        return tokenRepo.countByUserAndActiveTrue(user);
    }

    public PersonalAccessToken findAccessToken(String value) {
        return tokenRepo.findByValue(value);
    }

    public PersonalAccessToken findAccessToken(long id) {
        return tokenRepo.findById(id);
    }

    public Streamable<PersistedLog> findAllPersistedLogs() {
        return persistedLogRepo.findByOrderByTimestampAsc();
    }

    public Streamable<PersistedLog> findPersistedLogsAfter(LocalDateTime dateTime) {
        return persistedLogRepo.findByTimestampAfterOrderByTimestampAsc(dateTime);
    }

    public List<String> findAllSucceededAzureDownloadCountProcessedItemsByNameIn(List<String> names) {
        return downloadCountRepo.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(names);
    }

    public List<Extension> findActiveExtensionsByPublicId(Collection<String> publicIds, String... namespacesToExclude) {
        return extensionJooqRepo.findAllActiveByPublicId(publicIds, namespacesToExclude);
    }

    public Extension findActiveExtension(String name, String namespaceName) {
        return extensionJooqRepo.findActiveByNameIgnoreCaseAndNamespaceNameIgnoreCase(name, namespaceName);
    }

    public List<Extension> findActiveExtensionsById(Collection<Long> ids) {
        return extensionJooqRepo.findAllActiveById(ids);
    }

    public Page<ExtensionVersion> findActiveVersions(QueryRequest request) {
        return extensionVersionJooqRepo.findActiveVersions(request);
    }

    public List<ExtensionVersion> findActiveExtensionVersions(Collection<Long> extensionIds, String targetPlatform) {
        return extensionVersionJooqRepo.findAllActiveByExtensionIdAndTargetPlatform(extensionIds, targetPlatform);
    }

    public List<ExtensionVersion> findActiveExtensionVersionsByVersion(String version, String extensionName, String namespaceName) {
        return extensionVersionJooqRepo.findAllActiveByVersionAndExtensionNameAndNamespaceName(version, extensionName, namespaceName);
    }

    public List<ExtensionVersion> findActiveExtensionVersionsByExtensionName(String targetPlatform, String extensionName, String namespaceName) {
        return extensionVersionJooqRepo.findAllActiveByExtensionNameAndNamespaceName(targetPlatform, extensionName, namespaceName);
    }

    public List<ExtensionVersion> findActiveExtensionVersionsByNamespaceName(String targetPlatform, String namespaceName) {
        return extensionVersionJooqRepo.findAllActiveByNamespaceName(targetPlatform, namespaceName);
    }

    public List<ExtensionVersion> findActiveExtensionVersionsByExtensionName(String targetPlatform, String extensionName) {
        return extensionVersionJooqRepo.findAllActiveByExtensionName(targetPlatform, extensionName);
    }

    public List<FileResource> findFileResourcesByExtensionVersionIdAndType(Collection<Long> extensionVersionIds, Collection<String> types) {
        return fileResourceJooqRepo.findAll(extensionVersionIds, types);
    }

    public List<FileResource> findResourceFileResources(long extVersionId, String prefix) {
        return fileResourceJooqRepo.findAllResources(extVersionId, prefix);
    }

    public List<NamespaceMembership> findNamespaceMemberships(Collection<Long> namespaceIds) {
        return membershipJooqRepo.findAllByNamespaceId(namespaceIds);
    }

    public AdminStatistics findAdminStatisticsByYearAndMonth(int year, int month) {
        return adminStatisticsRepo.findByYearAndMonth(year, month);
    }

    public long countActiveExtensions(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.countActiveExtensions(endExclusive);
    }

    public long countActiveExtensionPublishers(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.countActiveExtensionPublishers(endExclusive);
    }

    public Map<Integer,Integer> countActiveExtensionPublishersGroupedByExtensionsPublished(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.countActiveExtensionPublishersGroupedByExtensionsPublished(endExclusive);
    }

    public Map<Integer,Integer> countActiveExtensionsGroupedByExtensionReviewRating(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.countActiveExtensionsGroupedByExtensionReviewRating(endExclusive);
    }

    public double averageNumberOfActiveReviewsPerActiveExtension(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.averageNumberOfActiveReviewsPerActiveExtension(endExclusive);
    }

    public long countPublishersThatClaimedNamespaceOwnership(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.countPublishersThatClaimedNamespaceOwnership(endExclusive);
    }

    public long downloadsUntil(LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.downloadsSumByTimestampLessThan(endExclusive);
    }

    public long downloadsBetween(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        return adminStatisticCalculationsRepo.downloadsSumByTimestampGreaterThanEqualAndTimestampLessThan(startInclusive, endExclusive);
    }

    public Map<String, Integer> topMostActivePublishingUsers(LocalDateTime endExclusive, int limit) {
        return adminStatisticCalculationsRepo.topMostActivePublishingUsers(endExclusive, limit);
    }

    public Map<String, Integer> topNamespaceExtensions(LocalDateTime endExclusive, int limit) {
        return adminStatisticCalculationsRepo.topNamespaceExtensions(endExclusive, limit);
    }

    public Map<String, Integer> topNamespaceExtensionVersions(LocalDateTime endExclusive, int limit) {
        return adminStatisticCalculationsRepo.topNamespaceExtensionVersions(endExclusive, limit);
    }

    public Map<String, Long> topMostDownloadedExtensions(LocalDateTime endExclusive, int limit) {
        return adminStatisticCalculationsRepo.topMostDownloadedExtensions(endExclusive, limit);
    }

    public Streamable<ExtensionVersion> findTargetPlatformVersions(String version, String extensionName, String namespaceName) {
        return extensionVersionRepo.findByVersionAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(version, extensionName, namespaceName);
    }

    public Streamable<ExtensionVersion> findVersions(UserData user) {
        return extensionVersionRepo.findByPublishedWithUser(user);
    }

    public void deleteFileResources(ExtensionVersion extVersion, String type) {
        fileResourceRepo.deleteByExtensionAndType(extVersion, type);
    }

    public int countVersions(Extension extension) {
        return extensionVersionRepo.countByExtension(extension);
    }

    public Streamable<MigrationItem> findNotMigratedResources() {
        return findNotMigratedItems("V1_23__FileResource_Extract_Resources.sql");
    }

    public Streamable<MigrationItem> findNotMigratedPreReleases() {
        return findNotMigratedItems("V1_26__Extension_Set_PreRelease.sql");
    }

    public Streamable<MigrationItem> findNotMigratedRenamedDownloads() {
        return findNotMigratedItems("V1_28__MigrationItem.sql");
    }

    public Streamable<MigrationItem> findNotMigratedVsixManifests() {
        return findNotMigratedItems("V1_32__FileResource_Extract_VsixManifest.sql");
    }

    public Streamable<MigrationItem> findNotMigratedTargetPlatforms() {
        return findNotMigratedItems("V1_34__ExtensionVersion_Fix_TargetPlatform.sql");
    }

    public Streamable<MigrationItem> findNotMigratedSha256Checksums() {
        return findNotMigratedItems("V1_35__FileResource_Generate_Sha256_Checksum.sql");
    }

    private Streamable<MigrationItem> findNotMigratedItems(String migrationScript) {
        return migrationItemRepo.findByMigrationScriptAndMigrationScheduledFalseOrderById(migrationScript);
    }

    public double getAverageReviewRating() {
        return extensionReviewRepo.averageRatingAndActiveTrue();
    }

    public Double getAverageReviewRating(Extension extension) {
        return extensionReviewRepo.averageRatingAndActiveTrue(extension);
    }

    public Streamable<FileResource> findFileResources(Namespace namespace) {
        return fileResourceRepo.findByExtensionExtensionNamespace(namespace);
    }

    public SignatureKeyPair findActiveKeyPair() {
        return signatureKeyPairRepo.findByActiveTrue();
    }

    public Streamable<ExtensionVersion> findVersions() {
        return extensionVersionRepo.findAll();
    }

    public Streamable<ExtensionVersion> findVersionsWithout(SignatureKeyPair keyPair) {
        return extensionVersionRepo.findBySignatureKeyPairNotOrSignatureKeyPairIsNull(keyPair);
    }

    public void deleteDownloadSigFiles() {
        fileResourceRepo.deleteByType(DOWNLOAD_SIG);
    }

    public void deleteAllKeyPairs() {
        extensionVersionRepo.setKeyPairsNull();
        signatureKeyPairRepo.deleteAll();
    }

    public SignatureKeyPair findKeyPair(String publicId) {
        return signatureKeyPairRepo.findByPublicId(publicId);
    }
}