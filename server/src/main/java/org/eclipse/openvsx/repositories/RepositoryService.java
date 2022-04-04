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

import org.eclipse.openvsx.dto.*;
import org.eclipse.openvsx.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;

@Component
public class RepositoryService {

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
    @Autowired ExtensionDTORepository extensionDTORepo;
    @Autowired ExtensionVersionDTORepository extensionVersionDTORepo;
    @Autowired FileResourceDTORepository fileResourceDTORepo;
    @Autowired NamespaceMembershipDTORepository namespaceMembershipDTORepo;
    @Autowired AdminStatisticsRepository adminStatisticsRepo;
    @Autowired AdminStatisticCalculationsRepository adminStatisticCalculationsRepo;

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

    public Streamable<Extension> findExtensions(Namespace namespace) {
        return extensionRepo.findByNamespace(namespace);
    }

    public Streamable<Extension> findExtensions(String name) {
        return extensionRepo.findByNameIgnoreCase(name);
    }

    public Streamable<Extension> findAllActiveExtensions() {
        return extensionRepo.findByActiveTrue();
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

    public Streamable<ExtensionVersion> findBundledExtensionsReference(Extension extension) {
        return extensionVersionRepo.findByBundledExtensions(extensionId(extension));
    }

    public Streamable<ExtensionVersion> findDependenciesReference(Extension extension) {
        return extensionVersionRepo.findByDependencies(extensionId(extension));
    }

    private String extensionId(Extension extension) {
        return extension.getNamespace().getName() + "." + extension.getName();
    }

    public Streamable<ExtensionVersion> findVersionsByAccessToken(PersonalAccessToken publishedWith) {
        return extensionVersionRepo.findByPublishedWith(publishedWith);
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
        return fileResourceRepo.findByExtensionAndNameIgnoreCase(extVersion, name);
    }

    public FileResource findFileByTypeAndName(ExtensionVersion extVersion, String type, String name) {
        return fileResourceRepo.findByExtensionAndTypeAndNameIgnoreCase(extVersion, type, name);
    }

    public Streamable<FileResource> findDownloadsByStorageTypeAndName(String storageType, Collection<String> names) {
        return fileResourceRepo.findByTypeAndStorageTypeAndNameIgnoreCaseIn(DOWNLOAD, storageType, names);
    }

    public FileResource findFileByType(ExtensionVersion extVersion, String type) {
        return fileResourceRepo.findByExtensionAndType(extVersion, type);
    }

    public Streamable<FileResource> findFilesByType(ExtensionVersion extVersion, Collection<String> types) {
        return fileResourceRepo.findByExtensionAndTypeIn(extVersion, types);
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

    public long countMemberships(UserData user, Namespace namespace) {
        return membershipRepo.countByUserAndNamespace(user, namespace);
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
    public List<ExtensionDTO> findAllActiveExtensionDTOsByPublicId(Collection<String> publicIds) {
        return extensionDTORepo.findAllActiveByPublicId(publicIds);
    }

    public ExtensionDTO findActiveExtensionDTO(String name, String namespaceName) {
        return extensionDTORepo.findActiveByNameIgnoreCaseAndNamespaceNameIgnoreCase(name, namespaceName);
    }

    public List<ExtensionDTO> findAllActiveExtensionDTOsById(Collection<Long> ids) {
        return extensionDTORepo.findAllActiveById(ids);
    }

    public List<ExtensionVersionDTO> findAllActiveExtensionVersionDTOs(Collection<Long> extensionIds, String targetPlatform) {
        return extensionVersionDTORepo.findAllActiveByExtensionIdAndTargetPlatform(extensionIds, targetPlatform);
    }

    public List<ExtensionVersionDTO> findActiveExtensionVersionDTOsByExtensionPublicId(String targetPlatform, String extensionPublicId) {
        return extensionVersionDTORepo.findAllActiveByExtensionPublicId(targetPlatform, extensionPublicId);
    }

    public List<ExtensionVersionDTO> findActiveExtensionVersionDTOsByNamespacePublicId(String targetPlatform, String namespacePublicId) {
        return extensionVersionDTORepo.findAllActiveByNamespacePublicId(targetPlatform, namespacePublicId);
    }

    public List<ExtensionVersionDTO> findActiveExtensionVersionDTOsByExtensionName(String targetPlatform, String extensionName, String namespaceName) {
        return extensionVersionDTORepo.findAllActiveByExtensionNameAndNamespaceName(targetPlatform, extensionName, namespaceName);
    }

    public List<ExtensionVersionDTO> findActiveExtensionVersionDTOsByNamespaceName(String targetPlatform, String namespaceName) {
        return extensionVersionDTORepo.findAllActiveByNamespaceName(targetPlatform, namespaceName);
    }

    public List<ExtensionVersionDTO> findActiveExtensionVersionDTOsByExtensionName(String targetPlatform, String extensionName) {
        return extensionVersionDTORepo.findAllActiveByExtensionName(targetPlatform, extensionName);
    }

    public List<FileResourceDTO> findAllFileResourceDTOsByExtensionVersionIdAndType(Collection<Long> extensionVersionIds, Collection<String> types) {
        return fileResourceDTORepo.findAll(extensionVersionIds, types);
    }

    public List<FileResourceDTO> findAllResourceFileResourceDTOs(String namespaceName, String extensionName, String version, String prefix) {
        return fileResourceDTORepo.findAllResources(namespaceName, extensionName, version, prefix);
    }

    public Map<Long, Integer> findAllActiveReviewCountsByExtensionId(Collection<Long> extensionIds) {
        return extensionDTORepo.findAllActiveReviewCountsById(extensionIds);
    }

    public List<NamespaceMembershipDTO> findAllNamespaceMembershipDTOs(Collection<Long> namespaceIds) {
        return namespaceMembershipDTORepo.findAllByNamespaceId(namespaceIds);
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

    public Streamable<ExtensionVersion> findTargetPlatformVersions(String version, String extensionName, String namespaceName) {
        return extensionVersionRepo.findByVersionAndExtensionNameIgnoreCaseAndExtensionNamespaceNameIgnoreCase(version, extensionName, namespaceName);
    }

    public Streamable<ExtensionVersion> findVersions(UserData user) {
        return extensionVersionRepo.findByPublishedWithUser(user);
    }
}