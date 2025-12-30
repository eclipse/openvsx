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
import org.eclipse.openvsx.json.TargetPlatformVersionJson;
import org.eclipse.openvsx.json.VersionTargetPlatformsJson;
import org.eclipse.openvsx.util.ExtensionId;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.web.SitemapRow;
import org.springframework.data.domain.*;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.eclipse.openvsx.entities.FileResource.*;

@Component
public class RepositoryService {

    private static final int MAX_VERSIONS = 100;
    private static final Sort VERSIONS_SORT = Sort.by(Sort.Direction.DESC, "semver.major", "semver.minor", "semver.patch")
            .and(Sort.by(Sort.Direction.ASC, "semver.isPreRelease"))
            .and(Sort.by(Sort.Direction.DESC, "universalTargetPlatform"))
            .and(Sort.by(Sort.Direction.ASC, "targetPlatform"))
            .and(Sort.by(Sort.Direction.DESC, "timestamp"));

    private final NamespaceRepository namespaceRepo;
    private final NamespaceJooqRepository namespaceJooqRepo;
    private final ExtensionRepository extensionRepo;
    private final ExtensionVersionRepository extensionVersionRepo;
    private final FileResourceRepository fileResourceRepo;
    private final ExtensionReviewRepository extensionReviewRepo;
    private final UserDataRepository userDataRepo;
    private final NamespaceMembershipRepository membershipRepo;
    private final PersonalAccessTokenRepository tokenRepo;
    private final PersonalAccessTokenJooqRepository tokenJooqRepo;
    private final PersistedLogRepository persistedLogRepo;
    private final DownloadCountProcessedItemRepository downloadCountRepo;
    private final ExtensionJooqRepository extensionJooqRepo;
    private final ExtensionVersionJooqRepository extensionVersionJooqRepo;
    private final FileResourceJooqRepository fileResourceJooqRepo;
    private final ExtensionReviewJooqRepository extensionReviewJooqRepo;
    private final NamespaceMembershipJooqRepository membershipJooqRepo;
    private final AdminStatisticsRepository adminStatisticsRepo;
    private final AdminStatisticCalculationsRepository adminStatisticCalculationsRepo;
    private final MigrationItemRepository migrationItemRepo;
    private final MigrationItemJooqRepository migrationItemJooqRepo;
    private final SignatureKeyPairRepository signatureKeyPairRepo;
    private final SignatureKeyPairJooqRepository signatureKeyPairJooqRepo;

    public RepositoryService(
            NamespaceRepository namespaceRepo,
            NamespaceJooqRepository namespaceJooqRepo,
            ExtensionRepository extensionRepo,
            ExtensionVersionRepository extensionVersionRepo,
            FileResourceRepository fileResourceRepo,
            ExtensionReviewRepository extensionReviewRepo,
            UserDataRepository userDataRepo,
            NamespaceMembershipRepository membershipRepo,
            PersonalAccessTokenRepository tokenRepo,
            PersonalAccessTokenJooqRepository tokenJooqRepo,
            PersistedLogRepository persistedLogRepo,
            DownloadCountProcessedItemRepository downloadCountRepo,
            ExtensionJooqRepository extensionJooqRepo,
            ExtensionVersionJooqRepository extensionVersionJooqRepo,
            FileResourceJooqRepository fileResourceJooqRepo,
            ExtensionReviewJooqRepository extensionReviewJooqRepo,
            NamespaceMembershipJooqRepository membershipJooqRepo,
            AdminStatisticsRepository adminStatisticsRepo,
            AdminStatisticCalculationsRepository adminStatisticCalculationsRepo,
            MigrationItemRepository migrationItemRepo,
            MigrationItemJooqRepository migrationItemJooqRepo,
            SignatureKeyPairRepository signatureKeyPairRepo,
            SignatureKeyPairJooqRepository signatureKeyPairJooqRepo
    ) {
        this.namespaceRepo = namespaceRepo;
        this.namespaceJooqRepo = namespaceJooqRepo;
        this.extensionRepo = extensionRepo;
        this.extensionVersionRepo = extensionVersionRepo;
        this.fileResourceRepo = fileResourceRepo;
        this.extensionReviewRepo = extensionReviewRepo;
        this.userDataRepo = userDataRepo;
        this.membershipRepo = membershipRepo;
        this.tokenRepo = tokenRepo;
        this.tokenJooqRepo = tokenJooqRepo;
        this.persistedLogRepo = persistedLogRepo;
        this.downloadCountRepo = downloadCountRepo;
        this.extensionJooqRepo = extensionJooqRepo;
        this.extensionVersionJooqRepo = extensionVersionJooqRepo;
        this.fileResourceJooqRepo = fileResourceJooqRepo;
        this.extensionReviewJooqRepo = extensionReviewJooqRepo;
        this.membershipJooqRepo = membershipJooqRepo;
        this.adminStatisticsRepo = adminStatisticsRepo;
        this.adminStatisticCalculationsRepo = adminStatisticCalculationsRepo;
        this.migrationItemRepo = migrationItemRepo;
        this.migrationItemJooqRepo = migrationItemJooqRepo;
        this.signatureKeyPairRepo = signatureKeyPairRepo;
        this.signatureKeyPairJooqRepo = signatureKeyPairJooqRepo;
    }

    public Namespace findNamespace(String name) {
        return namespaceRepo.findByNameIgnoreCase(name);
    }

    public String findNamespaceName(String name) {
        return namespaceJooqRepo.findNameByNameIgnoreCase(name);
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

    public Streamable<Extension> findActiveExtensions(Namespace namespace) {
        return extensionRepo.findByNamespaceAndActiveTrueOrderByNameAsc(namespace);
    }

    public Streamable<Extension> findExtensions(Collection<Long> extensionIds) {
        return extensionRepo.findByIdIn(extensionIds);
    }

    public Streamable<Extension> findExtensions(Namespace namespace) {
        return extensionRepo.findByNamespace(namespace);
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

    public int getMaxExtensionDownloadCount() {
        return extensionRepo.getMaxDownloadCount();
    }

    public ExtensionVersion findVersion(String version, String targetPlatform, Extension extension) {
        return extensionVersionRepo.findByVersionAndTargetPlatformAndExtension(version, targetPlatform, extension);
    }

    public ExtensionVersion findVersion(String version, String targetPlatform, String extensionName, String namespace) {
        return extensionVersionRepo.findByVersionAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(version, targetPlatform, extensionName, namespace);
    }

    public ExtensionVersion findVersion(UserData user, String version, String targetPlatform, String extensionName, String namespace) {
        return extensionVersionRepo.findByPublishedWithUserAndVersionAndTargetPlatformAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(user, version, targetPlatform, extensionName, namespace);
    }

    public Streamable<ExtensionVersion> findVersions(Extension extension) {
         return extensionVersionRepo.findByExtension(extension);
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

    public Page<String> findActiveVersionStringsSorted(String namespace, String extension, String targetPlatform, PageRequest page) {
        return extensionVersionJooqRepo.findActiveVersionStringsSorted(namespace, extension, targetPlatform, page);
    }

    public List<String> findVersionStringsSorted(Extension extension, String targetPlatform, boolean onlyActive) {
        return extensionVersionJooqRepo.findVersionStringsSorted(extension.getId(), targetPlatform, onlyActive, MAX_VERSIONS);
    }

    public Map<Long, List<String>> findActiveVersionStringsSorted(Collection<Long> extensionIds, String targetPlatform) {
        return extensionVersionJooqRepo.findActiveVersionStringsSorted(extensionIds, targetPlatform, MAX_VERSIONS);
    }

    public List<ExtensionVersion> findActiveVersionReferencesSorted(Collection<Long> extensionIds) {
        return extensionVersionJooqRepo.findActiveVersionReferencesSorted(extensionIds, MAX_VERSIONS);
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

    public Streamable<ExtensionVersion> findVersionsByUser(UserData user, boolean active) {
        return extensionVersionRepo.findByPublishedWithUserAndActive(user, active);
    }

    public LocalDateTime getOldestExtensionTimestamp() {
        return extensionVersionRepo.getOldestTimestamp();
    }

    public Streamable<FileResource> findFiles(ExtensionVersion extVersion) {
        return fileResourceRepo.findByExtension(extVersion);
    }

    public void deleteFiles(ExtensionVersion extVersion) {
        fileResourceRepo.deleteByExtension(extVersion);
    }

    public Streamable<FileResource> findFilesByStorageType(String storageType) {
        return fileResourceRepo.findByStorageType(storageType);
    }

    public FileResource findFileByName(String namespace, String extension, String targetPlatform, String version, String name) {
        return fileResourceJooqRepo.findByName(namespace, extension, targetPlatform, version, name);
    }

    public Streamable<FileResource> findDownloadsByStorageTypeAndName(String storageType, Collection<String> names) {
        return fileResourceRepo.findByTypeAndStorageTypeAndNameIgnoreCaseIn(DOWNLOAD, storageType, names);
    }

    public Streamable<FileResource> findFilesByType(String type) {
        return fileResourceRepo.findByType(type);
    }

    public FileResource findFileByType(ExtensionVersion extVersion, String type) {
        return fileResourceRepo.findByExtensionAndType(extVersion, type);
    }

    public FileResource findFileByType(String namespace, String extension, String targetPlatform, String version, String type) {
        return fileResourceJooqRepo.findByType(namespace, extension, targetPlatform, version, type);
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

    public Page<UserData> findUsersByLoginNameStartingWith(String loginNameStart, int limit) {
        return userDataRepo.findByLoginNameStartingWith(loginNameStart, Pageable.ofSize(limit));
    }

    public long countUsers() {
        return userDataRepo.count();
    }

    public NamespaceMembership findMembership(UserData user, Namespace namespace) {
        return membershipRepo.findByUserAndNamespace(user, namespace);
    }

    public boolean hasMembership(UserData user, Namespace namespace) {
        return membershipJooqRepo.hasMembership(user, namespace);
    }

    public boolean isVerified(Namespace namespace, UserData user) {
        return membershipJooqRepo.isVerified(namespace, user);
    }

    public Streamable<NamespaceMembership> findMemberships(Namespace namespace, String role) {
        return membershipRepo.findByNamespaceAndRoleIgnoreCase(namespace, role);
    }

    public boolean hasMemberships(Namespace namespace, String role) {
        return membershipJooqRepo.hasRole(namespace, role);
    }

    public Streamable<NamespaceMembership> findMemberships(UserData user) {
        return membershipRepo.findByUserOrderByNamespaceName(user);
    }

    public Streamable<NamespaceMembership> findMemberships(Namespace namespace) {
        return membershipRepo.findByNamespace(namespace);
    }

    public List<NamespaceMembership> findMemberships(String namespaceName) {
        return membershipJooqRepo.findByNamespaceName(namespaceName);
    }

    public Streamable<PersonalAccessToken> findAccessTokens(UserData user) {
        return tokenRepo.findByUser(user);
    }

    public Streamable<PersonalAccessToken> findAllAccessTokens() {
        return tokenRepo.findAll();
    }

    public Streamable<PersonalAccessToken> findActiveAccessTokens(UserData user) {
        return tokenRepo.findByUserAndActiveTrue(user);
    }

    public long countActiveAccessTokens(UserData user) {
        return tokenRepo.countByUserAndActiveTrue(user);
    }

    public PersonalAccessToken findAccessToken(String value) {
        return  tokenRepo.findByValue(value);
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

    public List<String> findAllSucceededDownloadCountProcessedItemsByStorageTypeAndNameIn(String storageType, List<String> names) {
        return downloadCountRepo.findAllSucceededDownloadCountProcessedItemsByStorageTypeAndNameIn(storageType, names);
    }

    public List<String> findAllFailedDownloadCountProcessedItemsByStorageTypeAndNameIn(String storageType, List<String> names) {
        return downloadCountRepo.findAllFailedDownloadCountProcessedItemsByStorageTypeAndNameIn(storageType, names);
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

    public List<FileResource> findFileResourcesByExtensionVersionIdAndType(Collection<Long> extensionVersionIds, Collection<String> types) {
        return fileResourceJooqRepo.findAll(extensionVersionIds, types);
    }

    public List<NamespaceMembership> findNamespaceMemberships(Collection<Long> namespaceIds) {
        return membershipJooqRepo.findAllByNamespaceId(namespaceIds);
    }

    public AdminStatistics findAdminStatisticsByYearAndMonth(int year, int month) {
        return adminStatisticsRepo.findByYearAndMonth(year, month);
    }

    public long countActiveExtensions() {
        return adminStatisticCalculationsRepo.countActiveExtensions();
    }

    public long countActiveExtensionPublishers() {
        return adminStatisticCalculationsRepo.countActiveExtensionPublishers();
    }

    public Map<Integer,Integer> countActiveExtensionPublishersGroupedByExtensionsPublished() {
        return adminStatisticCalculationsRepo.countActiveExtensionPublishersGroupedByExtensionsPublished();
    }

    public Map<Integer,Integer> countActiveExtensionsGroupedByExtensionReviewRating() {
        return adminStatisticCalculationsRepo.countActiveExtensionsGroupedByExtensionReviewRating();
    }

    public double averageNumberOfActiveReviewsPerActiveExtension() {
        return adminStatisticCalculationsRepo.averageNumberOfActiveReviewsPerActiveExtension();
    }

    public long countPublishersThatClaimedNamespaceOwnership() {
        return adminStatisticCalculationsRepo.countPublishersThatClaimedNamespaceOwnership();
    }

    public long downloadsTotal() {
        return adminStatisticCalculationsRepo.downloadsTotal();
    }

    public Map<String, Integer> topMostActivePublishingUsers(int limit) {
        return adminStatisticCalculationsRepo.topMostActivePublishingUsers(limit);
    }

    public Map<String, Integer> topNamespaceExtensions(int limit) {
        return adminStatisticCalculationsRepo.topNamespaceExtensions(limit);
    }

    public Map<String, Integer> topNamespaceExtensionVersions(int limit) {
        return adminStatisticCalculationsRepo.topNamespaceExtensionVersions(limit);
    }

    public Map<String, Long> topMostDownloadedExtensions(int limit) {
        return adminStatisticCalculationsRepo.topMostDownloadedExtensions(limit);
    }

    public Streamable<ExtensionVersion> findTargetPlatformVersions(String version, String extensionName, String namespaceName) {
        return extensionVersionRepo.findByVersionAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(version, extensionName, namespaceName);
    }

    public int countVersions(String namespaceName, String extensionName) {
        return extensionVersionJooqRepo.count(namespaceName, extensionName);
    }

    public Slice<MigrationItem> findNotMigratedItems(Pageable page) {
        return migrationItemRepo.findByMigrationScheduledFalseOrderById(page);
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

    public List<Extension> findAllPublicIds() {
        return extensionJooqRepo.findAllPublicIds();
    }

    public Extension findPublicId(String namespace, String extension) {
        return extensionJooqRepo.findPublicId(namespace, extension);
    }

    public Extension findPublicId(String publicId) {
        return extensionJooqRepo.findPublicId(publicId);
    }

    public Extension findNamespacePublicId(String publicId) {
        return extensionJooqRepo.findNamespacePublicId(publicId);
    }

    public void updateExtensionPublicIds(Map<Long, String> publicIds) {
        extensionJooqRepo.updatePublicIds(publicIds);
    }

    public void updateNamespacePublicIds(Map<Long, String> publicIds) {
        namespaceJooqRepo.updatePublicIds(publicIds);
    }

    public boolean extensionPublicIdExists(String publicId) {
        return extensionJooqRepo.publicIdExists(publicId);
    }

    public boolean namespacePublicIdExists(String publicId) {
        return namespaceJooqRepo.publicIdExists(publicId);
    }

    public List<SitemapRow> fetchSitemapRows() {
        return extensionJooqRepo.fetchSitemapRows();
    }

    public List<VersionTargetPlatformsJson> findTargetPlatformsGroupedByVersion(Extension extension) {
        return extensionVersionJooqRepo.findTargetPlatformsGroupedByVersion(extension);
    }

    public List<VersionTargetPlatformsJson> findTargetPlatformsGroupedByVersion(Extension extension, UserData user) {
        return extensionVersionJooqRepo.findTargetPlatformsGroupedByVersion(extension, user);
    }

    public List<ExtensionVersion> findVersionsForUrls(Extension extension, String targetPlatform, String version) {
        return extensionVersionJooqRepo.findVersionsForUrls(extension, targetPlatform, version);
    }

    public List<Extension> findActiveExtensionsForUrls(Namespace namespace) {
        return extensionJooqRepo.findActiveExtensionsForUrls(namespace);
    }

    public ExtensionVersion findExtensionVersion(String namespace, String extension, String targetPlatform, String version) {
        return extensionVersionJooqRepo.find(namespace, extension, targetPlatform, version);
    }

    public ExtensionVersion findLatestVersionForAllUrls(Extension extension, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        return extensionVersionJooqRepo.findLatestForAllUrls(extension, targetPlatform, onlyPreRelease, onlyActive);
    }

    public ExtensionVersion findLatestVersion(Extension extension, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        return extensionVersionJooqRepo.findLatest(extension, targetPlatform, onlyPreRelease, onlyActive);
    }

    public ExtensionVersion findLatestVersion(String namespaceName, String extensionName, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        return extensionVersionJooqRepo.findLatest(namespaceName, extensionName, targetPlatform, onlyPreRelease, onlyActive);
    }

    public List<ExtensionVersion> findLatestVersions(Namespace namespace) {
        return extensionVersionJooqRepo.findLatest(namespace);
    }

    public List<ExtensionVersion> findLatestVersions(Collection<Long> extensionIds) {
        return extensionVersionJooqRepo.findLatest(extensionIds);
    }

    public Map<Long, Boolean> findLatestVersionsIsPreview(Collection<Long> extensionIds) {
        return extensionVersionJooqRepo.findLatestIsPreview(extensionIds);
    }

    public List<ExtensionVersion> findLatestVersions(UserData user) {
        return extensionVersionJooqRepo.findLatest(user);
    }

    public ExtensionVersion findLatestVersion(UserData user, String namespace, String extension) {
        return extensionVersionJooqRepo.findLatest(user, namespace, extension);
    }

    public List<String> findExtensionTargetPlatforms(Extension extension) {
        return extensionVersionJooqRepo.findDistinctTargetPlatforms(extension);
    }

    public void deactivateKeyPairs() {
        signatureKeyPairRepo.updateActiveSetFalse();
    }

    public int deactivateAccessTokens(UserData user) {
        return tokenRepo.updateActiveSetFalse(user);
    }

    public List<String> findActiveExtensionNames(Namespace namespace) {
        return extensionJooqRepo.findActiveExtensionNames(namespace);
    }

    public List<NamespaceMembership> findMembershipsForOwner(UserData user, String namespaceName) {
        return membershipJooqRepo.findMembershipsForOwner(user, namespaceName);
    }

    public boolean isNamespaceOwner(UserData user, Namespace namespace) {
        return membershipJooqRepo.isOwner(user, namespace);
    }

    public boolean namespaceExists(String namespaceName) {
        return namespaceJooqRepo.exists(namespaceName);
    }

    public boolean hasSameVersion(ExtensionVersion extVersion) {
        return extensionVersionJooqRepo.hasSameVersion(extVersion);
    }

    public boolean hasActiveReview(Extension extension, UserData user) {
        return extensionReviewJooqRepo.hasActiveReview(extension, user);
    }

    public boolean hasAccessToken(String value) {
        return tokenJooqRepo.hasToken(value);
    }

    public boolean canPublishInNamespace(UserData user, Namespace namespace) {
        return membershipJooqRepo.canPublish(user, namespace);
    }

    public String findSignatureKeyPairPublicId(String namespace, String extension, String targetPlatform, String version) {
        return signatureKeyPairJooqRepo.findPublicId(namespace, extension, targetPlatform, version);
    }

    public String findFirstUnresolvedDependency(List<ExtensionId> dependencies) {
        return extensionJooqRepo.findFirstUnresolvedDependency(dependencies);
    }

    public PersonalAccessToken findAccessToken(UserData user, String description) {
        return tokenRepo.findByUserAndDescriptionAndActiveTrue(user, description);
    }

    public NamespaceMembership findFirstMembership(String namespaceName) {
        return membershipRepo.findFirstByNamespaceNameIgnoreCase(namespaceName);
    }

    public ExtensionVersion findLatestReplacement(long extensionId, String targetPlatform, boolean onlyPreRelease, boolean onlyActive) {
        return extensionVersionJooqRepo.findLatestReplacement(extensionId, targetPlatform, onlyPreRelease, onlyActive);
    }

    public boolean hasExtension(String namespace, String extension) {
        return extensionJooqRepo.hasExtension(namespace, extension);
    }

    public Streamable<Extension> findDeprecatedExtensions(Extension replacement) {
        return extensionRepo.findByReplacement(replacement);
    }

    public List<MigrationItem> findRemoveFileResourceTypeResourceMigrationItems(int offset, int limit) {
        return migrationItemJooqRepo.findRemoveFileResourceTypeResourceMigrationItems(offset, limit);
    }

    public boolean isDeleteAllVersions(String namespaceName, String extensionName, List<TargetPlatformVersionJson> targetVersions, UserData user) {
        return extensionVersionJooqRepo.isDeleteAllVersions(namespaceName, extensionName, targetVersions, user);
    }

    public Streamable<PersonalAccessToken> findAccessTokensCreatedBefore(LocalDateTime timestamp) {
        return tokenRepo.findByCreatedTimestampLessThanEqualAndActiveTrue(timestamp);
    }

    public void expireAccessTokens(LocalDateTime timestamp) {
        tokenRepo.expireAccessTokens(timestamp);
    }
}