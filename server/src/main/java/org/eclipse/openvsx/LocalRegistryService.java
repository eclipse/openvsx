/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import com.google.common.collect.Maps;
import io.micrometer.observation.annotation.Observed;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.openvsx.cache.CacheService.*;
import static org.eclipse.openvsx.entities.FileResource.*;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;
import static org.eclipse.openvsx.util.UrlUtil.createApiVersionUrl;

@Component
public class LocalRegistryService implements IExtensionRegistry {

    protected final Logger logger = LoggerFactory.getLogger(LocalRegistryService.class);

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final ExtensionService extensions;
    private final VersionService versions;
    private final UserService users;
    private final SearchUtilService search;
    private final ExtensionValidator validator;
    private final StorageUtilService storageUtil;
    private final EclipseService eclipse;
    private final CacheService cache;
    private final ExtensionVersionIntegrityService integrityService;

    public LocalRegistryService(
            EntityManager entityManager,
            RepositoryService repositories,
            ExtensionService extensions,
            VersionService versions,
            UserService users,
            SearchUtilService search,
            ExtensionValidator validator,
            StorageUtilService storageUtil,
            EclipseService eclipse,
            CacheService cache,
            ExtensionVersionIntegrityService integrityService
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.extensions = extensions;
        this.versions = versions;
        this.users = users;
        this.search = search;
        this.validator = validator;
        this.storageUtil = storageUtil;
        this.eclipse = eclipse;
        this.cache = cache;
        this.integrityService = integrityService;
    }

    @Value("${ovsx.registry.version:}")
    String registryVersion;

    @Override
    public NamespaceJson getNamespace(String namespaceName) {
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null)
            throw new NotFoundException();
        var json = new NamespaceJson();
        json.name = namespace.getName();
        json.extensions = new LinkedHashMap<>();
        var serverUrl = UrlUtil.getBaseUrl();
        for (var ext : repositories.findActiveExtensions(namespace)) {
            String url = createApiUrl(serverUrl, "api", namespace.getName(), ext.getName());
            json.extensions.put(ext.getName(), url);
        }
        json.verified = repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER) > 0;
        json.access = "restricted";
        return json;
    }

    @Override
    @Cacheable(value = CACHE_EXTENSION_JSON, keyGenerator = GENERATOR_EXTENSION_JSON)
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform) {
        return getExtension(namespace, extensionName, targetPlatform, VersionAlias.LATEST);
    }

    @Override
    @Cacheable(value = CACHE_EXTENSION_JSON, keyGenerator = GENERATOR_EXTENSION_JSON)
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform, String version) {
        var extVersion = findExtensionVersion(namespace, extensionName, targetPlatform, version);
        var json = toExtensionVersionJson(extVersion, targetPlatform, true);
        json.downloads = getDownloads(extVersion.getExtension(), targetPlatform, extVersion.getVersion());
        return json;
    }

    @Override
    public VersionsJson getVersions(String namespace, String extension, String targetPlatform, int size, int offset) {
        var pageRequest = PageRequest.of((offset/size), size);
        var page = repositories.findActiveVersionStringsSorted(namespace, extension, targetPlatform, pageRequest);

        var json = new VersionsJson();
        json.offset = (int) page.getPageable().getOffset();
        json.totalSize = (int) page.getTotalElements();
        var namespaceLowerCase = namespace.toLowerCase();
        var extensionLowerCase = extension.toLowerCase();
        json.versions = page.get()
                .collect(Collectors.toMap(
                        version -> version,
                        version -> UrlUtil.createApiVersionUrl(UrlUtil.getBaseUrl(), namespaceLowerCase, extensionLowerCase, targetPlatform, version),
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));

        return json;
    }

    @Override
    public VersionReferencesJson getVersionReferences(String namespace, String extension, String targetPlatform, int size, int offset) {
        var pageRequest = PageRequest.of((offset/size), size);
        var page = targetPlatform == null
                ? repositories.findActiveVersionsSorted(namespace, extension, pageRequest)
                : repositories.findActiveVersionsSorted(namespace, extension, targetPlatform, pageRequest);

        var fileUrls = storageUtil.getFileUrls(page.getContent(), UrlUtil.getBaseUrl(), withFileTypes(DOWNLOAD));

        var json = new VersionReferencesJson();
        json.offset = (int) page.getPageable().getOffset();
        json.totalSize = (int) page.getTotalElements();
        json.versions = page.get()
                .map(extVersion -> {
                    var versionRef = new VersionReferenceJson();
                    versionRef.version = extVersion.getVersion();
                    versionRef.targetPlatform = extVersion.getTargetPlatform();
                    versionRef.engines = extVersion.getEnginesMap();
                    versionRef.url = UrlUtil.createApiVersionUrl(UrlUtil.getBaseUrl(), extVersion);
                    versionRef.files = fileUrls.get(extVersion.getId());
                    if(versionRef.files.containsKey(DOWNLOAD_SIG)) {
                        versionRef.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
                    }

                    return versionRef;
                })
                .collect(Collectors.toList());

        return json;
    }

    private Map<String, String> getDownloads(Extension extension, String targetPlatform, String version) {
        var extVersions = repositories.findVersionsForUrls(extension, targetPlatform, version);
        var fileUrls = storageUtil.getFileUrls(extVersions, UrlUtil.getBaseUrl(), DOWNLOAD);
        return extVersions.stream()
                .map(ev -> {
                    var files = fileUrls.get(ev.getId());
                    var download = files != null ? files.get(DOWNLOAD) : null;
                    if(download == null) {
                        logger.warn("Could not find download for: {}", NamingUtil.toLogFormat(ev));
                        return null;
                    } else {
                        return new AbstractMap.SimpleEntry<>(ev.getTargetPlatform(), download);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ExtensionVersion findExtensionVersion(String namespace, String extensionName, String targetPlatform, String version) {
        var extVersion = repositories.findExtensionVersion(namespace, extensionName, targetPlatform, version);
        if (extVersion == null) {
            throw new NotFoundException();
        }

        return extVersion;
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extensionName, String targetPlatform, String version, String fileName) {
        var extVersion = findExtensionVersion(namespace, extensionName, targetPlatform, version);
        var resource = isType(fileName) ? repositories.findFileByType(extVersion, fileName.toLowerCase()) : repositories.findFileByName(extVersion, fileName);
        if (resource == null)
            throw new NotFoundException();
        if (resource.getType().equals(DOWNLOAD))
            storageUtil.increaseDownloadCount(resource);

        return storageUtil.getFileResponse(resource);
    }

    public boolean isType (String fileName){
        var expectedTypes = new ArrayList<>(List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, DOWNLOAD_SHA256, CHANGELOG, VSIXMANIFEST));
        if(integrityService.isEnabled()) {
            expectedTypes.add(DOWNLOAD_SIG);
        }

        return expectedTypes.stream().anyMatch(fileName::equalsIgnoreCase);
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive())
            throw new NotFoundException();
        var list = new ReviewListJson();
        var serverUrl = UrlUtil.getBaseUrl();
        list.postUrl = createApiUrl(serverUrl, "api", extension.getNamespace().getName(), extension.getName(), "review");
        list.deleteUrl = createApiUrl(serverUrl, "api", extension.getNamespace().getName(), extension.getName(), "review", "delete");
        list.reviews = repositories.findActiveReviews(extension)
                .map(extReview -> extReview.toReviewJson())
                .toList();
        return list;
    }

    @Override
    public SearchResultJson search(ISearchService.Options options) {
        var json = new SearchResultJson();
        var size = options.requestedSize;
        if (size <= 0 || !search.isEnabled()) {
            json.extensions = Collections.emptyList();
            return json;
        }

        var searchHits = search.search(options);
        if(searchHits.hasSearchHits()) {
            json.extensions = toSearchEntries(searchHits, options);
            json.offset = options.requestedOffset;
            json.totalSize = (int) searchHits.getTotalHits();
        } else {
            json.extensions = Collections.emptyList();
        }

        return json;
    }

    @Override
    public QueryResultJson query(QueryRequest request) {
        if (!StringUtils.isEmpty(request.extensionId)) {
            var split = request.extensionId.split("\\.");
            if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty())
                throw new ErrorResultException("The 'extensionId' parameter must have the format 'namespace.extension'.");
            if (!StringUtils.isEmpty(request.namespaceName) && !request.namespaceName.equals(split[0]))
                throw new ErrorResultException("Conflicting parameters 'extensionId' and 'namespaceName'");
            if (!StringUtils.isEmpty(request.extensionName) && !request.extensionName.equals(split[1]))
                throw new ErrorResultException("Conflicting parameters 'extensionId' and 'extensionName'");
            request.namespaceName = split[0];
            request.extensionName = split[1];
            request.extensionId = null;
        }

        if(!TargetPlatform.isValid(request.targetPlatform)) {
            request.targetPlatform = null;
        }

        var extensionVersionsPage = repositories.findActiveVersions(request);
        var extensionVersions = extensionVersionsPage.getContent();
        var extensionIds = extensionVersions.stream()
                .map(ev -> ev.getExtension().getId())
                .collect(Collectors.toSet());

        var reviewCounts = getReviewCounts(extensionVersions);
        var versionStrings = getVersionStrings(extensionIds, request.targetPlatform);
        var latestVersions = getLatestVersions(extensionVersions);
        var latestPreReleases = getLatestVersions(extensionVersions, true);
        var previewsByExtensionId = getPreviews(extensionIds);
        var fileResourcesByExtensionVersionId = getFileResources(extensionVersions);
        var membershipsByNamespaceId = getMemberships(extensionVersions);

        var result = new QueryResultJson();
        result.offset = (int) extensionVersionsPage.getPageable().getOffset();
        result.totalSize = (int) extensionVersionsPage.getTotalElements();
        result.extensions = extensionVersions.stream()
                .map(ev -> {
                    var latest = latestVersions.get(getLatestVersionKey(ev));
                    var latestPreRelease = latestPreReleases.get(getLatestVersionKey(ev));
                    var reviewCount = reviewCounts.getOrDefault(ev.getExtension().getId(), 0L);
                    var preview = previewsByExtensionId.get(ev.getExtension().getId());
                    var versions = versionStrings.get(ev.getExtension().getId());
                    var fileResources = fileResourcesByExtensionVersionId.getOrDefault(ev.getId(), Collections.emptyList());
                    return toExtensionVersionJson(ev, latest, latestPreRelease, reviewCount, preview, versions, request.targetPlatform, fileResources, membershipsByNamespaceId);
                })
                .collect(Collectors.toList());

        return result;
    }

    @Override
    public QueryResultJson queryV2(QueryRequestV2 request) {
        if (!StringUtils.isEmpty(request.extensionId)) {
            var split = request.extensionId.split("\\.");
            if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty())
                throw new ErrorResultException("The 'extensionId' parameter must have the format 'namespace.extension'.");
            if (!StringUtils.isEmpty(request.namespaceName) && !request.namespaceName.equals(split[0]))
                throw new ErrorResultException("Conflicting parameters 'extensionId' and 'namespaceName'");
            if (!StringUtils.isEmpty(request.extensionName) && !request.extensionName.equals(split[1]))
                throw new ErrorResultException("Conflicting parameters 'extensionId' and 'extensionName'");
            request.namespaceName = split[0];
            request.extensionName = split[1];
            request.extensionId = null;
        }

        if(!TargetPlatform.isValid(request.targetPlatform)) {
            request.targetPlatform = null;
        }
        // Revert to default includeAllVersions value when extensionVersion is set
        if(!StringUtils.isEmpty(request.extensionVersion) && request.includeAllVersions.equals("true")) {
            request.includeAllVersions = "links";
        }

        var queryRequest = new QueryRequest();
        queryRequest.namespaceName = request.namespaceName;
        queryRequest.extensionName = request.extensionName;
        queryRequest.extensionVersion = request.extensionVersion;
        queryRequest.extensionUuid = request.extensionUuid;
        queryRequest.namespaceUuid = request.namespaceUuid;
        queryRequest.includeAllVersions = request.includeAllVersions.equals("true");
        queryRequest.targetPlatform = request.targetPlatform;
        queryRequest.size = request.size;
        queryRequest.offset = request.offset;

        var extensionVersionsPage = repositories.findActiveVersions(queryRequest);
        var extensionVersions = extensionVersionsPage.getContent();
        var extensionIds = extensionVersions.stream()
                .map(ev -> ev.getExtension().getId())
                .collect(Collectors.toSet());

        var reviewCounts = getReviewCounts(extensionVersions);
        var addAllVersions = request.includeAllVersions.equals("links");
        var versionStrings = addAllVersions ? getVersionStrings(extensionIds, request.targetPlatform) : null;
        var latestGlobalVersions = addAllVersions ? getLatestGlobalVersions(extensionVersions) : null;
        var latestGlobalPreReleases = addAllVersions ? getLatestGlobalVersions(extensionVersions, true) : null;

        var latestVersions = getLatestVersions(extensionVersions);
        var latestPreReleases = getLatestVersions(extensionVersions, true);
        var previewsByExtensionId = getPreviews(extensionIds);
        var fileResourcesByExtensionVersionId = getFileResources(extensionVersions);
        var membershipsByNamespaceId = getMemberships(extensionVersions);

        var result = new QueryResultJson();
        result.offset = (int) extensionVersionsPage.getPageable().getOffset();
        result.totalSize = (int) extensionVersionsPage.getTotalElements();
        result.extensions = extensionVersions.stream()
                .map(ev -> {
                    var latest = latestVersions.get(getLatestVersionKey(ev));
                    var latestPreRelease = latestPreReleases.get(getLatestVersionKey(ev));
                    var reviewCount = reviewCounts.getOrDefault(ev.getExtension().getId(), 0L);
                    var preview = previewsByExtensionId.get(ev.getExtension().getId());
                    var fileResources = fileResourcesByExtensionVersionId.getOrDefault(ev.getId(), Collections.emptyList());
                    var globalLatest = addAllVersions ? latestGlobalVersions.get(ev.getExtension().getId()) : null;
                    var globalLatestPreRelease = addAllVersions ? latestGlobalPreReleases.get(ev.getExtension().getId()) : null;
                    var versions = addAllVersions ? versionStrings.get(ev.getExtension().getId()) : null;

                    return toExtensionVersionJsonV2(ev, latest, latestPreRelease, globalLatest, globalLatestPreRelease, reviewCount, preview, versions, request.targetPlatform, fileResources, membershipsByNamespaceId);
                })
                .collect(Collectors.toList());

        return result;
    }

    @Override
    @Transactional
    @Cacheable(CACHE_NAMESPACE_DETAILS_JSON)
    @Observed
    public NamespaceDetailsJson getNamespaceDetails(String namespaceName) {
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new NotFoundException();
        }

        var json = namespace.toNamespaceDetailsJson();
        json.verified = repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER) > 0;
        json.logo = namespace.getLogoStorageType() != null
                ? storageUtil.getNamespaceLogoLocation(namespace).toString()
                : null;

        var serverUrl = UrlUtil.getBaseUrl();
        var extVersions = repositories.findLatestVersions(namespace);
        var fileUrls = storageUtil.getFileUrls(extVersions, serverUrl, withFileTypes(DOWNLOAD, ICON));
        json.extensions = extVersions.stream()
                .map(extVersion -> {
                    var entry = extVersion.toSearchEntryJson();
                    entry.url = createApiUrl(serverUrl, "api", entry.namespace, entry.name);
                    entry.files = fileUrls.get(extVersion.getId());
                    if(entry.files.containsKey(DOWNLOAD_SIG)) {
                        entry.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
                    }

                    return entry;
                })
                .collect(Collectors.toList());

        return json;
    }

    private String[] withFileTypes(String... types) {
        var typesList = new ArrayList<>(List.of(types));
        if(typesList.contains(DOWNLOAD)) {
            typesList.add(DOWNLOAD_SHA256);
            if(integrityService.isEnabled()) {
                typesList.add(DOWNLOAD_SIG);
            }
        }

        return typesList.toArray(String[]::new);
    }

    @Override
    public ResponseEntity<byte[]> getNamespaceLogo(String namespaceName, String fileName) {
        if(fileName == null) {
            fileName = "";
        }

        var namespace = repositories.findNamespace(namespaceName);
        if(namespace == null || !fileName.equals(namespace.getLogoName())) {
            throw new NotFoundException();
        }

        return storageUtil.getNamespaceLogo(namespace);
    }

    private Map<Long, Long> getReviewCounts(List<ExtensionVersion> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        return extensionVersions.stream()
                .map(ExtensionVersion::getExtension)
                .filter(e -> e.getReviewCount() != null)
                .collect(Collectors.toMap(Extension::getId, Extension::getReviewCount, (reviews1, reviews2) -> reviews1));
    }

    private Map<Long, List<String>> getVersionStrings(Set<Long> extensionIds, String targetPlatform) {
        if(extensionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return repositories.findActiveVersionStringsSorted(extensionIds, targetPlatform);
    }

    private Map<String, ExtensionVersion> getLatestVersions(List<ExtensionVersion> extensionVersions) {
        return getLatestVersions(extensionVersions, false);
    }

    private Map<String, ExtensionVersion> getLatestVersions(List<ExtensionVersion> extensionVersions, boolean onlyPreRelease) {
        return extensionVersions.stream()
                .collect(Collectors.groupingBy(this::getLatestVersionKey))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, true, onlyPreRelease))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(this::getLatestVersionKey, ev -> ev));
    }

    private Map<Long, ExtensionVersion> getLatestGlobalVersions(List<ExtensionVersion> extensionVersions) {
        return getLatestGlobalVersions(extensionVersions, false);
    }

    private Map<Long, ExtensionVersion> getLatestGlobalVersions(List<ExtensionVersion> extensionVersions, boolean onlyPreRelease) {
        return extensionVersions.stream()
                .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, false, onlyPreRelease))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ev -> ev.getExtension().getId(), ev -> ev));
    }

    private String getLatestVersionKey(ExtensionVersion extVersion) {
        return extVersion.getExtension().getId() + "@" + extVersion.getTargetPlatform();
    }

    private Map<Long, Boolean> getPreviews(Set<Long> extensionIds) {
        return repositories.findActiveExtensionVersions(extensionIds, null).stream()
                .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, false))
                .collect(Collectors.toMap(ev -> ev.getExtension().getId(), ExtensionVersion::isPreview));
    }

    private Map<Long, List<FileResource>> getFileResources(List<ExtensionVersion> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        var fileTypes = List.of(withFileTypes(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG, VSIXMANIFEST));
        var extensionVersionIds = extensionVersions.stream()
                .map(ExtensionVersion::getId)
                .collect(Collectors.toSet());

        return repositories.findFileResourcesByExtensionVersionIdAndType(extensionVersionIds, fileTypes).stream()
                .collect(Collectors.groupingBy(fr -> fr.getExtension().getId()));
    }

    private Map<Long, List<NamespaceMembership>> getMemberships(Collection<ExtensionVersion> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        var namespaceIds = extensionVersions.stream()
                .map(ExtensionVersion::getExtension)
                .map(Extension::getNamespace)
                .map(Namespace::getId)
                .collect(Collectors.toSet());

        return repositories.findNamespaceMemberships(namespaceIds).stream()
                .collect(Collectors.groupingBy(nm -> nm.getNamespace().getId()));
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson createNamespace(NamespaceJson json, String tokenValue) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            throw new ErrorResultException("Invalid access token.");
        }

        return createNamespace(json, token.getUser());
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson createNamespace(NamespaceJson json, UserData user) {
        var namespaceIssue = validator.validateNamespace(json.name);
        if (namespaceIssue.isPresent()) {
            throw new ErrorResultException(namespaceIssue.get().toString());
        }

        eclipse.checkPublisherAgreement(user);
        var namespace = repositories.findNamespace(json.name);
        if (namespace != null) {
            throw new ErrorResultException("Namespace already exists: " + namespace.getName());
        }

        // Create the requested namespace
        namespace = new Namespace();
        namespace.setName(json.name);
        entityManager.persist(namespace);

        // Assign the requesting user as contributor
        var membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        membership.setRole(NamespaceMembership.ROLE_CONTRIBUTOR);
        entityManager.persist(membership);

        return ResultJson.success("Created namespace " + namespace.getName());
    }

    public ResultJson verifyToken(String namespaceName, String tokenValue) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            throw new ErrorResultException("Invalid access token.");
        }

        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new NotFoundException();
        }

        var user = token.getUser();
        if (!users.hasPublishPermission(user, namespace)) {
            throw new ErrorResultException("Insufficient access rights for namespace: " + namespaceName);
        }

        return ResultJson.success("Valid token");
    }

    public ExtensionJson publish(InputStream content, UserData user) throws ErrorResultException {
        var token = users.createAccessToken(user, "One time use publish token");
        var json = publish(content, token.value);
        users.deleteAccessToken(user, token.id);
        return json;
    }

    public ExtensionJson publish(InputStream content, String tokenValue) throws ErrorResultException {
        var token = users.useAccessToken(tokenValue);
        if (token == null || token.getUser() == null) {
            throw new ErrorResultException("Invalid access token.");
        }

        // Check whether the user has a valid publisher agreement
        eclipse.checkPublisherAgreement(token.getUser());

        var extVersion = extensions.publishVersion(content, token);
        var json = toExtensionVersionJson(extVersion, null, true);
        json.success = "It can take a couple minutes before the extension version is available";

        var sameVersions = repositories.findVersions(extVersion.getVersion(), extVersion.getExtension());
        if(sameVersions.stream().anyMatch(ev -> ev.isPreRelease() != extVersion.isPreRelease())) {
            var existingRelease = extVersion.isPreRelease() ? "stable release" : "pre-release";
            var thisRelease = extVersion.isPreRelease() ? "pre-release" : "stable release";
            var extension = extVersion.getExtension();
            var semver = extVersion.getSemanticVersion();
            var newVersion = String.join(".", String.valueOf(semver.getMajor()), String.valueOf(semver.getMinor() + 1), "0");

            json.warning = "A " + existingRelease + " already exists for " + NamingUtil.toLogFormat(extension.getNamespace().getName(), extension.getName(), extVersion.getVersion()) + ".\n" +
                    "To prevent update conflicts, we recommend that this " + thisRelease + " uses " + newVersion + " as its version instead.";
        }

        return json;
    }

    @Transactional(rollbackOn = ResponseStatusException.class)
    public ResultJson postReview(ReviewJson review, String namespace, String extensionName) {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive()) {
            var extensionId = NamingUtil.toExtensionId(namespace, extensionName);
            return ResultJson.error("Extension not found: " + extensionId);
        }
        var activeReviews = repositories.findActiveReviews(extension, user);
        if (!activeReviews.isEmpty()) {
            return ResultJson.error("You must not submit more than one review for an extension.");
        }

        var extReview = new ExtensionReview();
        extReview.setExtension(extension);
        extReview.setActive(true);
        extReview.setTimestamp(TimeUtil.getCurrentUTC());
        extReview.setUser(user);
        extReview.setTitle(review.title);
        extReview.setComment(review.comment);
        extReview.setRating(review.rating);
        entityManager.persist(extReview);
        extension.setAverageRating(repositories.getAverageReviewRating(extension));
        extension.setReviewCount(repositories.countActiveReviews(extension));
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        cache.evictLatestExtensionVersion(extension);
        return ResultJson.success("Added review for " + NamingUtil.toExtensionId(extension));
    }

    @Transactional(rollbackOn = ResponseStatusException.class)
    public ResultJson deleteReview(String namespace, String extensionName) {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive()) {
            return ResultJson.error("Extension not found: " + NamingUtil.toExtensionId(namespace, extensionName));
        }
        var activeReviews = repositories.findActiveReviews(extension, user);
        if (activeReviews.isEmpty()) {
            return ResultJson.error("You have not submitted any review yet.");
        }

        for (var extReview : activeReviews) {
            extReview.setActive(false);
        }

        extension.setAverageRating(repositories.getAverageReviewRating(extension));
        extension.setReviewCount(repositories.countActiveReviews(extension));
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        cache.evictLatestExtensionVersion(extension);
        return ResultJson.success("Deleted review for " + NamingUtil.toExtensionId(extension));
    }

    private List<Extension> getExtensions(SearchHits<ExtensionSearch> searchHits) {
        var ids = searchHits.stream()
                .map(searchHit -> searchHit.getContent().id)
                .distinct()
                .collect(Collectors.toList());

        var extensions = findExtensions(ids).collect(Collectors.toList());
        var extensionIds = extensions.stream()
                .map(Extension::getId)
                .collect(Collectors.toSet());

        ids.removeAll(extensionIds);
        if(!ids.isEmpty()) {
            search.removeSearchEntries(ids);
        }

        var inactiveExtensions = extensions.stream()
                .filter(extension -> !extension.isActive())
                .collect(Collectors.toList());

        if(!inactiveExtensions.isEmpty()) {
            var inactiveIds = inactiveExtensions.stream()
                    .map(Extension::getId)
                    .collect(Collectors.toList());

            search.removeSearchEntries(inactiveIds);
            extensions.removeAll(inactiveExtensions);
        }

        return extensions;
    }

    private Stream<Extension> findExtensions(Collection<Long> ids) {
        var extById = new HashMap<Long, Extension>();
        repositories.findExtensions(ids)
                .forEach(ext -> extById.put(ext.getId(), ext));
        return ids.stream()
                .map(extById::get)
                .filter(Objects::nonNull);
    }


    private List<SearchEntryJson> toSearchEntries(SearchHits<ExtensionSearch> searchHits, ISearchService.Options options) {
        var serverUrl = UrlUtil.getBaseUrl();
        var extensions = getExtensions(searchHits);
        var latestVersions = extensions.stream()
                .map(e -> {
                    var latest = repositories.findLatestVersion(e, null, false, true);
                    return new AbstractMap.SimpleEntry<>(e.getId(), latest);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var membershipsByNamespaceId = getMemberships(latestVersions.values());
        var searchEntries = latestVersions.entrySet().stream()
                .map(e -> {
                    var entry = e.getValue().toSearchEntryJson();
                    entry.url = createApiUrl(serverUrl, "api", entry.namespace, entry.name);
                    entry.verified = isVerified(e.getValue(), membershipsByNamespaceId);
                    return new AbstractMap.SimpleEntry<>(e.getKey(), entry);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var fileUrls = storageUtil.getFileUrls(latestVersions.values(), serverUrl, withFileTypes(DOWNLOAD, ICON));
        searchEntries.forEach((extensionId, searchEntry) -> {
            var extVersion = latestVersions.get(extensionId);
            searchEntry.files = fileUrls.get(extVersion.getId());
            if(searchEntry.files.containsKey(DOWNLOAD_SIG)) {
                searchEntry.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
            }
        });
        if (options.includeAllVersions) {
            var activeVersions = repositories.findActiveVersionReferencesSorted(extensions);
            var activeVersionsByExtensionId = activeVersions.stream().collect(Collectors.groupingBy(ev -> ev.getExtension().getId()));
            var versionFileUrls = storageUtil.getFileUrls(activeVersions, serverUrl, withFileTypes(DOWNLOAD));
            for(var extension : extensions) {
                var extVersions = activeVersionsByExtensionId.get(extension.getId());
                var searchEntry = searchEntries.get(extension.getId());
                searchEntry.allVersions = getAllVersionReferences(extVersions, versionFileUrls, serverUrl);
                searchEntry.allVersionsUrl = UrlUtil.createAllVersionsUrl(searchEntry.namespace, searchEntry.name, options.targetPlatform, "version-references");
            }
        }

        return extensions.stream()
                .map(Extension::getId)
                .map(searchEntries::get)
                .collect(Collectors.toList());
    }

    private List<VersionReferenceJson> getAllVersionReferences(
            List<ExtensionVersion> extVersions,
            Map<Long, Map<String, String>> versionFileUrls,
            String serverUrl
    ) {
        return extVersions.stream().map(extVersion -> {
            var ref = new VersionReferenceJson();
            ref.version = extVersion.getVersion();
            ref.targetPlatform = extVersion.getTargetPlatform();
            ref.engines = extVersion.getEnginesMap();
            ref.url = UrlUtil.createApiVersionUrl(serverUrl, extVersion);
            ref.files = versionFileUrls.get(extVersion.getId());
            if(ref.files.containsKey(DOWNLOAD_SIG)) {
                ref.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
            }

            return ref;
        }).collect(Collectors.toList());
    }

    public ExtensionJson toExtensionVersionJson(ExtensionVersion extVersion, String targetPlatform, boolean onlyActive) {
        var extension = extVersion.getExtension();
        var latest = repositories.findLatestVersionForAllUrls(extension, targetPlatform, false, onlyActive);
        var latestPreRelease = repositories.findLatestVersionForAllUrls(extension, targetPlatform, true, onlyActive);

        var json = extVersion.toExtensionJson();
        json.preview = latest != null && latest.isPreview();
        json.versionAlias = new ArrayList<>(2);
        if (latest != null && extVersion.getVersion().equals(latest.getVersion()))
            json.versionAlias.add(VersionAlias.LATEST);
        if (latestPreRelease != null && extVersion.getVersion().equals(latestPreRelease.getVersion()))
            json.versionAlias.add(VersionAlias.PRE_RELEASE);
        json.verified = isVerified(extVersion);
        json.namespaceAccess = "restricted";
        json.unrelatedPublisher = !json.verified;
        json.reviewCount = Optional.ofNullable(extension.getReviewCount()).orElse(0L);
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");

        var allVersions = new ArrayList<String>();
        if (latest != null)
            allVersions.add(VersionAlias.LATEST);
        if (latestPreRelease != null)
            allVersions.add(VersionAlias.PRE_RELEASE);

        var versionBaseUrl = UrlUtil.createApiVersionBaseUrl(serverUrl, json.namespace, json.name, targetPlatform);
        allVersions.addAll(repositories.findVersionStringsSorted(extension, targetPlatform, onlyActive));
        json.allVersionsUrl = UrlUtil.createAllVersionsUrl(json.namespace, json.name, targetPlatform, "versions");
        json.allVersions = Maps.newLinkedHashMapWithExpectedSize(allVersions.size());
        for(var version : allVersions) {
            json.allVersions.put(version, createApiUrl(versionBaseUrl, version));
        }

        var fileUrls = storageUtil.getFileUrls(List.of(extVersion), serverUrl, withFileTypes(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG, VSIXMANIFEST));
        json.files = fileUrls.get(extVersion.getId());
        if(json.files.containsKey(DOWNLOAD_SIG)) {
            json.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
        }
        if (json.dependencies != null) {
            json.dependencies.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, "api", ref.namespace, ref.extension);
            });
        }
        if (json.bundledExtensions != null) {
            json.bundledExtensions.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, "api", ref.namespace, ref.extension);
            });
        }
        return json;
    }

    public ExtensionJson toExtensionVersionJson(
            ExtensionVersion extVersion,
            ExtensionVersion latest,
            ExtensionVersion latestPreRelease,
            long reviewCount,
            boolean preview,
            List<String> versions,
            String targetPlatformParam,
            List<FileResource> resources,
            Map<Long, List<NamespaceMembership>> membershipsByNamespaceId
    ) {
        var json = extVersion.toExtensionJson();
        json.preview = preview;
        json.verified = isVerified(extVersion, membershipsByNamespaceId);
        json.namespaceAccess = "restricted";
        json.unrelatedPublisher = !json.verified;
        json.reviewCount = reviewCount;
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");

        json.versionAlias = new ArrayList<>(2);
        if (extVersion.equals(latest)) {
            json.versionAlias.add(VersionAlias.LATEST);
        }
        if (extVersion.equals(latestPreRelease)) {
            json.versionAlias.add(VersionAlias.PRE_RELEASE);
        }

        var allVersions = new ArrayList<String>();
        if(latest != null) {
            allVersions.add(VersionAlias.LATEST);
        }
        if(latestPreRelease != null) {
            allVersions.add(VersionAlias.PRE_RELEASE);
        }
        if(versions != null && !versions.isEmpty()) {
            allVersions.addAll(versions);
        }

        json.allVersionsUrl = UrlUtil.createAllVersionsUrl(json.namespace, json.name, targetPlatformParam, "versions");
        json.allVersions = Maps.newLinkedHashMapWithExpectedSize(allVersions.size());
        var versionBaseUrl = UrlUtil.createApiVersionBaseUrl(serverUrl, json.namespace, json.name, targetPlatformParam);
        for(var version : allVersions) {
            json.allVersions.put(version, createApiUrl(versionBaseUrl, version));
        }

        json.files = Maps.newLinkedHashMapWithExpectedSize(8);
        var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, json.namespace, json.name, json.targetPlatform, json.version);
        for (var resource : resources) {
            var fileUrl = UrlUtil.createApiFileUrl(fileBaseUrl, resource.getName());
            json.files.put(resource.getType(), fileUrl);
        }
        if(json.files.containsKey(DOWNLOAD_SIG)) {
            json.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
        }

        if (json.dependencies != null) {
            json.dependencies.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, "api", ref.namespace, ref.extension);
            });
        }
        if (json.bundledExtensions != null) {
            json.bundledExtensions.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, "api", ref.namespace, ref.extension);
            });
        }
        return json;
    }

    public ExtensionJson toExtensionVersionJsonV2(
            ExtensionVersion extVersion,
            ExtensionVersion latest,
            ExtensionVersion latestPreRelease,
            ExtensionVersion globalLatest,
            ExtensionVersion globalLatestPreRelease,
            long reviewCount,
            boolean preview,
            List<String> versions,
            String targetPlatformParam,
            List<FileResource> resources,
            Map<Long, List<NamespaceMembership>> membershipsByNamespaceId
    ) {
        var json = extVersion.toExtensionJson();
        json.preview = preview;
        json.verified = isVerified(extVersion, membershipsByNamespaceId);
        json.namespaceAccess = "restricted";
        json.unrelatedPublisher = !json.verified;
        json.reviewCount = reviewCount;
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");
        json.url = createApiVersionUrl(serverUrl, json);

        json.versionAlias = new ArrayList<>(2);
        if (extVersion.equals(latest)) {
            json.versionAlias.add(VersionAlias.LATEST);
        }
        if (extVersion.equals(latestPreRelease)) {
            json.versionAlias.add(VersionAlias.PRE_RELEASE);
        }

        var allVersions = new ArrayList<String>();
        if(globalLatest != null) {
            allVersions.add(VersionAlias.LATEST);
        }
        if(globalLatestPreRelease != null) {
            allVersions.add(VersionAlias.PRE_RELEASE);
        }
        if(versions != null && !versions.isEmpty()) {
            allVersions.addAll(versions);
        }

        json.allVersionsUrl = UrlUtil.createAllVersionsUrl(json.namespace, json.name, targetPlatformParam, "versions");
        if(!allVersions.isEmpty()) {
            json.allVersions = Maps.newLinkedHashMapWithExpectedSize(allVersions.size());
            var versionBaseUrl = UrlUtil.createApiVersionBaseUrl(serverUrl, json.namespace, json.name, targetPlatformParam);
            for(var version : allVersions) {
                json.allVersions.put(version, createApiUrl(versionBaseUrl, version));
            }
        }

        json.files = Maps.newLinkedHashMapWithExpectedSize(8);
        var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, json.namespace, json.name, json.targetPlatform, json.version);
        for (var resource : resources) {
            var fileUrl = UrlUtil.createApiFileUrl(fileBaseUrl, resource.getName());
            json.files.put(resource.getType(), fileUrl);
        }
        if(json.files.containsKey(DOWNLOAD_SIG)) {
            json.files.put(PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVersion));
        }

        if (json.dependencies != null) {
            json.dependencies.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, "api", ref.namespace, ref.extension);
            });
        }
        if (json.bundledExtensions != null) {
            json.bundledExtensions.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, "api", ref.namespace, ref.extension);
            });
        }
        return json;
    }

    private boolean isVerified(ExtensionVersion extVersion) {
        if (extVersion.getPublishedWith() == null) {
            return false;
        }

        var user = extVersion.getPublishedWith().getUser();
        if(UserData.ROLE_PRIVILEGED.equals(user.getRole())) {
            return true;
        }

        var namespace = extVersion.getExtension().getNamespace();
        return repositories.isVerified(namespace, user);
    }

    private boolean isVerified(ExtensionVersion extVersion, Map<Long, List<NamespaceMembership>> membershipsByNamespaceId) {
        if (extVersion.getPublishedWith() == null) {
            return false;
        }

        var user = extVersion.getPublishedWith().getUser();
        if(UserData.ROLE_PRIVILEGED.equals(user.getRole())) {
            return true;
        }

        var namespace = extVersion.getExtension().getNamespace().getId();
        var memberships = membershipsByNamespaceId.get(namespace);
        return memberships.stream().anyMatch(m -> m.getRole().equalsIgnoreCase(NamespaceMembership.ROLE_OWNER))
                && memberships.stream().anyMatch(m -> m.getUser().getId() == user.getId());
    }

    public String getPublicKey(String publicId) {
        var keyPair = repositories.findKeyPair(publicId);
        if(keyPair == null) {
            throw new NotFoundException();
        }

        return keyPair.getPublicKeyText();
    }

    @Override
    public RegistryVersionJson getRegistryVersion() {
        if (this.registryVersion == null || this.registryVersion.isEmpty()) {
            throw new NotFoundException();
        }
        var registryVersion = new RegistryVersionJson();
        registryVersion.version = this.registryVersion;
        return registryVersion;
    }
}
