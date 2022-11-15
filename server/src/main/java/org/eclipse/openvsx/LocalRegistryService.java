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

import static org.eclipse.openvsx.cache.CacheService.CACHE_EXTENSION_JSON;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_EXTENSION_JSON;
import static org.eclipse.openvsx.entities.FileResource.*;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.dto.*;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalRegistryService implements IExtensionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRegistryService.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    ExtensionService extensions;

    @Autowired
    VersionService versions;

    @Autowired
    UserService users;

    @Autowired
    SearchUtilService search;

    @Autowired
    ExtensionValidator validator;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    EclipseService eclipse;

    @Autowired
    CacheService cache;

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
    @Transactional
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform) {
        return getExtension(namespace, extensionName, targetPlatform, "latest");
    }

    @Override
    @Transactional
    @Cacheable(value = CACHE_EXTENSION_JSON, keyGenerator = GENERATOR_EXTENSION_JSON)
    public ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform, String version) {
        var extVersion = findExtensionVersion(namespace, extensionName, targetPlatform, version);
        var json = toExtensionVersionJson(extVersion, targetPlatform, true);
        json.downloads = getDownloads(extVersion.getExtension(), targetPlatform, extVersion.getVersion());
        return json;
    }

    private Map<String, String> getDownloads(Extension extension, String targetPlatform, String version) {
        var downloadsStream = extension.getVersions().stream()
                .filter(ev -> ev.getVersion().equals(version));
        if(targetPlatform != null) {
            downloadsStream = downloadsStream.filter(ev -> ev.getTargetPlatform().equals(targetPlatform));
        }

        var extVersions = downloadsStream.collect(Collectors.toList());
        var fileUrls = storageUtil.getFileUrls(extVersions, UrlUtil.getBaseUrl(), DOWNLOAD);
        return extVersions.stream()
                .map(ev -> {
                    var files = fileUrls.get(ev.getId());
                    var download = files != null ? files.get(DOWNLOAD) : null;
                    if(download == null) {
                        var e = ev.getExtension();
                        LOGGER.warn("Could not find download for: {}.{}-{}@{}", e.getNamespace().getName(), e.getName(), ev.getVersion(), ev.getTargetPlatform());
                        return null;
                    } else {
                        return new AbstractMap.SimpleEntry<>(ev.getTargetPlatform(), download);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ExtensionVersion findExtensionVersion(String namespace, String extensionName, String targetPlatform, String version) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive())
            throw new NotFoundException();

        ExtensionVersion extVersion;
        if("latest".equals(version)) {
            extVersion = versions.getLatest(extension, targetPlatform, false, true);
        } else if("pre-release".equals(version)) {
            extVersion = versions.getLatest(extension, targetPlatform, true, true);
        } else {
            var stream = extension.getVersions().stream()
                    .filter(ev -> ev.getVersion().equals(version))
                    .filter(ExtensionVersion::isActive);

            stream = targetPlatform != null
                    ? stream.filter(ev -> ev.getTargetPlatform().equals(targetPlatform))
                    : stream.sorted(Comparator.<ExtensionVersion, Boolean>comparing(TargetPlatform::isUniversal).thenComparing(ExtensionVersion::getTargetPlatform));

            extVersion = stream.findFirst().orElse(null);
        }

        if (extVersion == null) {
            throw new NotFoundException();
        }

        return extVersion;
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> getFile(String namespace, String extensionName, String targetPlatform, String version, String fileName) {
        var extVersion = findExtensionVersion(namespace, extensionName, targetPlatform, version);
        var resource = repositories.findFileByName(extVersion, fileName);
        if (resource == null)
            throw new NotFoundException();
        if (resource.getType().equals(DOWNLOAD))
            storageUtil.increaseDownloadCount(resource);
        if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var headers = storageUtil.getFileResponseHeaders(fileName);
            return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(storageUtil.getLocation(resource))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .build();
        }
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
    @Transactional
    public SearchResultJson search(ISearchService.Options options) {
        var json = new SearchResultJson();
        var size = options.requestedSize;
        if (size <= 0 || !search.isEnabled()) {
            json.extensions = Collections.emptyList();
            return json;
        }

        var searchHits = search.search(options);
        json.extensions = toSearchEntries(searchHits, options);
        json.offset = options.requestedOffset;
        json.totalSize = (int) searchHits.getTotalHits();
        return json;
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        if (!Strings.isNullOrEmpty(param.extensionId)) {
            var split = param.extensionId.split("\\.");
            if (split.length != 2 || split[0].isEmpty() || split[1].isEmpty())
                throw new ErrorResultException("The 'extensionId' parameter must have the format 'namespace.extension'.");
            if (!Strings.isNullOrEmpty(param.namespaceName) && !param.namespaceName.equals(split[0]))
                throw new ErrorResultException("Conflicting parameters 'extensionId' and 'namespaceName'");
            if (!Strings.isNullOrEmpty(param.extensionName) && !param.extensionName.equals(split[1]))
                throw new ErrorResultException("Conflicting parameters 'extensionId' and 'extensionName'");
            param.namespaceName = split[0];
            param.extensionName = split[1];
        }

        List<ExtensionVersionDTO> extensionVersions = new ArrayList<>();
        var targetPlatform = TargetPlatform.isValid(param.targetPlatform) ? param.targetPlatform : null;

        // Add extension by UUID (public_id)
        if (!Strings.isNullOrEmpty(param.extensionUuid)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByExtensionPublicId(targetPlatform, param.extensionUuid));
        }
        // Add extensions by namespace UUID (public_id)
        if (!Strings.isNullOrEmpty(param.namespaceUuid)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByNamespacePublicId(targetPlatform, param.namespaceUuid));
        }

        // Add extension by namespace and name
        if (!Strings.isNullOrEmpty(param.namespaceName) && !Strings.isNullOrEmpty(param.extensionName)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByExtensionName(targetPlatform, param.extensionName, param.namespaceName));
        // Add extensions by namespace
        } else if (!Strings.isNullOrEmpty(param.namespaceName)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByNamespaceName(targetPlatform, param.namespaceName));
        // Add extensions by name
        } else if (!Strings.isNullOrEmpty(param.extensionName)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByExtensionName(targetPlatform, param.extensionName));
        }

        extensionVersions = extensionVersions.stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getId))
                .values()
                .stream()
                .map(l -> l.get(0))
                .collect(Collectors.toList());

        var extensionIds = extensionVersions.stream()
                .map(ExtensionVersionDTO::getExtensionId)
                .collect(Collectors.toSet());

        var reviewCounts = getReviewCounts(extensionIds);
        var versionStrings = getVersionStrings(extensionVersions);
        var latestVersions = getLatestVersions(extensionVersions);
        var latestPreReleases = getLatestVersions(extensionVersions, true);
        var previewsByExtensionId = getPreviews(extensionIds);
        var fileResourcesByExtensionVersionId = getFileResources(extensionVersions);
        var membershipsByNamespaceId = getMemberships(extensionVersions);

        // Add a specific version of an extension
        if (!Strings.isNullOrEmpty(param.namespaceName) && !Strings.isNullOrEmpty(param.extensionName)
                && !Strings.isNullOrEmpty(param.extensionVersion) && !param.includeAllVersions) {
            extensionVersions = extensionVersions.stream()
                    .filter(ev -> ev.getVersion().equals(param.extensionVersion))
                    .filter(ev -> ev.getExtension().getName().equals(param.extensionName))
                    .filter(ev -> ev.getExtension().getNamespace().equals(param.namespaceName))
                    .collect(Collectors.toList());
        }
        // Only add latest version of an extension
        if(Strings.isNullOrEmpty(param.extensionVersion) && !param.includeAllVersions) {
            extensionVersions = new ArrayList<>(latestVersions.values());
        }

        var result = new QueryResultJson();
        result.extensions = extensionVersions.stream()
                .filter(ev -> addToResult(ev, param))
                .sorted(getExtensionVersionComparator())
                .map(ev -> {
                    var latest = latestVersions.get(getLatestVersionKey(ev));
                    var latestPreRelease = latestPreReleases.get(getLatestVersionKey(ev));
                    var reviewCount = reviewCounts.getOrDefault(ev.getExtensionId(), 0);
                    var preview = previewsByExtensionId.get(ev.getExtensionId());
                    var versions = versionStrings.get(ev.getExtensionId());
                    var fileResources = fileResourcesByExtensionVersionId.getOrDefault(ev.getId(), Collections.emptyList());
                    return toExtensionVersionJson(ev, latest, latestPreRelease, reviewCount, preview, versions, fileResources, membershipsByNamespaceId);
                })
                .collect(Collectors.toList());

        return result;
    }

    private Map<Long, Integer> getReviewCounts(Collection<Long> extensionIds) {
        return !extensionIds.isEmpty()
                ? repositories.findAllActiveReviewCountsByExtensionId(extensionIds)
                : Collections.emptyMap();
    }

    private Map<Long, Set<String>> getVersionStrings(List<ExtensionVersionDTO> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        return extensionVersions.stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtensionId, Collectors.mapping(ExtensionVersionDTO::getVersion, Collectors.toSet())));
    }

    private Map<String, ExtensionVersionDTO> getLatestVersions(List<ExtensionVersionDTO> extensionVersions) {
        return getLatestVersions(extensionVersions, false);
    }

    private Map<String, ExtensionVersionDTO> getLatestVersions(List<ExtensionVersionDTO> extensionVersions, boolean onlyPreRelease) {
        var latestStream = extensionVersions.stream();
        if(onlyPreRelease) {
            latestStream = latestStream.filter(ExtensionVersionDTO::isPreRelease);
        }

        return latestStream.collect(Collectors.groupingBy(this::getLatestVersionKey))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, true))
                .collect(Collectors.toMap(this::getLatestVersionKey, ev -> ev));
    }

    private String getLatestVersionKey(ExtensionVersionDTO extVersion) {
        return extVersion.getExtensionId() + "@" + extVersion.getTargetPlatform();
    }

    private Map<Long, Boolean> getPreviews(Set<Long> extensionIds) {
        return repositories.findAllActiveExtensionVersionDTOs(extensionIds, null).stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtensionId))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, false))
                .collect(Collectors.toMap(ExtensionVersionDTO::getExtensionId, ExtensionVersionDTO::isPreview));
    }

    private Map<Long, List<FileResourceDTO>> getFileResources(List<ExtensionVersionDTO> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        var fileTypes = List.of(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG);
        var extensionVersionIds = extensionVersions.stream()
                .map(ExtensionVersionDTO::getId)
                .collect(Collectors.toSet());

        return repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(extensionVersionIds, fileTypes).stream()
                .collect(Collectors.groupingBy(FileResourceDTO::getExtensionVersionId));
    }

    private Map<Long, List<NamespaceMembershipDTO>> getMemberships(List<ExtensionVersionDTO> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        var namespaceIds = extensionVersions.stream()
                .map(ExtensionVersionDTO::getExtension)
                .map(ExtensionDTO::getNamespace)
                .map(NamespaceDTO::getId)
                .collect(Collectors.toSet());

        return repositories.findAllNamespaceMembershipDTOs(namespaceIds).stream()
                .collect(Collectors.groupingBy(NamespaceMembershipDTO::getNamespaceId));
    }

    private Comparator<ExtensionVersionDTO> getExtensionVersionComparator() {
        // comparators combine comparator ExtensionVersion.SORT_COMPARATOR and order by of RepositoryService.findActiveExtensions
        var versionComparator = Comparator.<ExtensionVersionDTO, SemanticVersion>comparing(ExtensionVersionDTO::getSemanticVersion)
                .thenComparing(ExtensionVersionDTO::getTimestamp)
                .reversed();

        return Comparator.<ExtensionVersionDTO, String>comparing(ev -> ev.getExtension().getName())
                .thenComparing(versionComparator);
    }

    private boolean addToResult(ExtensionVersionDTO extVersion, QueryParamJson param) {
        if (mismatch(extVersion.getVersion(), param.extensionVersion))
            return false;
        var extension = extVersion.getExtension();
        if (mismatch(extension.getName(), param.extensionName))
            return false;
        var namespace = extension.getNamespace();
        if (mismatch(namespace.getName(), param.namespaceName))
            return false;
        if (mismatch(extension.getPublicId(), param.extensionUuid) || mismatch(namespace.getPublicId(), param.namespaceUuid))
            return false;

        return true;
    }

    private static boolean mismatch(String s1, String s2) {
        return s1 != null && s2 != null
                && !s1.isEmpty() && !s2.isEmpty()
                && !s1.equalsIgnoreCase(s2);
    }

    public PersonalAccessToken getAccessToken(String tokenValue) {
        var token = users.useAccessToken(tokenValue);
        if (token == null || token.getUser() == null) {
            throw new ErrorResultException("Invalid access token.", HttpStatus.UNAUTHORIZED);
        }

        return token;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson createNamespace(NamespaceJson json, String tokenValue) {
        var token = getAccessToken(tokenValue);
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

    @Retryable(value = { DataIntegrityViolationException.class })
    @Transactional(rollbackOn = ErrorResultException.class)
    public ExtensionJson publish(InputStream content, UserData user) throws ErrorResultException {
        var token = new PersonalAccessToken();
        token.setDescription("One time use publish token");
        token.setValue(users.generateTokenValue());
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setAccessedTimestamp(token.getCreatedTimestamp());
        token.setUser(user);
        token.setActive(true);
        entityManager.persist(token);

        var json = publish(content, token.getValue());
        token.setActive(false);
        return json;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ExtensionJson publish(InputStream content, String tokenValue) throws ErrorResultException {
        var token = getAccessToken(tokenValue);

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

            json.warning = "A " + existingRelease + " already exists for " +
                    extension.getNamespace().getName() + "." + extension.getName() + "-" + extVersion.getVersion() + ".\n" +
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
            return ResultJson.error("Extension not found: " + namespace + "." + extensionName);
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
        extension.setAverageRating(computeAverageRating(extension));
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        return ResultJson.success("Added review for " + extension.getNamespace().getName() + "." + extension.getName());
    }

    @Transactional(rollbackOn = ResponseStatusException.class)
    public ResultJson deleteReview(String namespace, String extensionName) {
        var user = users.findLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive()) {
            return ResultJson.error("Extension not found: " + namespace + "." + extensionName);
        }
        var activeReviews = repositories.findActiveReviews(extension, user);
        if (activeReviews.isEmpty()) {
            return ResultJson.error("You have not submitted any review yet.");
        }

        for (var extReview : activeReviews) {
            extReview.setActive(false);
        }
        extension.setAverageRating(computeAverageRating(extension));
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        return ResultJson.success("Deleted review for " + extension.getNamespace().getName() + "." + extension.getName());
    }

    private Double computeAverageRating(Extension extension) {
        var activeReviews = repositories.findActiveReviews(extension);
        if (activeReviews.isEmpty()) {
            return null;
        }
        long sum = 0;
        long count = 0;
        for (var review : activeReviews) {
            sum += review.getRating();
            count++;
        }
        return (double) sum / count;
    }

    private Extension getExtension(SearchHit<ExtensionSearch> searchHit) {
        var searchItem = searchHit.getContent();
        var extension = entityManager.find(Extension.class, searchItem.id);
        if (extension == null || !extension.isActive()) {
            extension = new Extension();
            extension.setId(searchItem.id);
            search.removeSearchEntry(extension);
            return null;
        }

        return extension;
    }

    private List<SearchEntryJson> toSearchEntries(SearchHits<ExtensionSearch> searchHits, ISearchService.Options options) {
        var serverUrl = UrlUtil.getBaseUrl();
        var extensions = searchHits.stream()
                .map(this::getExtension)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        var latestVersions = extensions.stream()
                .map(e -> {
                    var latest = versions.getLatest(e, null, false, true);
                    return new AbstractMap.SimpleEntry<>(e.getId(), latest);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var searchEntries = latestVersions.entrySet().stream()
                .map(e -> {
                    var entry = e.getValue().toSearchEntryJson();
                    entry.url = createApiUrl(serverUrl, "api", entry.namespace, entry.name);
                    return new AbstractMap.SimpleEntry<>(e.getKey(), entry);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var fileUrls = storageUtil.getFileUrls(latestVersions.values(), serverUrl, DOWNLOAD, ICON);
        searchEntries.forEach((extensionId, searchEntry) -> searchEntry.files = fileUrls.get(latestVersions.get(extensionId).getId()));
        if (options.includeAllVersions) {
            var allActiveVersions = repositories.findActiveVersions(extensions).toList();
            var activeVersionsByExtensionId = allActiveVersions.stream()
                    .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()));

            var versionUrls = storageUtil.getFileUrls(allActiveVersions, serverUrl, DOWNLOAD);
            for(var extension : extensions) {
                var activeVersions = activeVersionsByExtensionId.get(extension.getId());
                var searchEntry = searchEntries.get(extension.getId());
                searchEntry.allVersions = getAllVersionReferences(activeVersions, versionUrls, serverUrl);
            }
        }

        return new ArrayList<>(searchEntries.values());
    }

    private List<SearchEntryJson.VersionReference> getAllVersionReferences(
            List<ExtensionVersion> extVersions,
            Map<Long, Map<String, String>> versionUrls,
            String serverUrl
    ) {
        Collections.sort(extVersions, ExtensionVersion.SORT_COMPARATOR);
        return extVersions.stream().map(extVersion -> {
            var ref = new SearchEntryJson.VersionReference();
            ref.version = extVersion.getVersion();
            ref.engines = extVersion.getEnginesMap();
            ref.url = UrlUtil.createApiVersionUrl(serverUrl, extVersion);
            ref.files = versionUrls.get(extVersion.getId());
            return ref;
        }).collect(Collectors.toList());
    }

    public ExtensionJson toExtensionVersionJson(ExtensionVersion extVersion, String targetPlatform, boolean onlyActive) {
        var extension = extVersion.getExtension();
        var latest = versions.getLatest(extension, targetPlatform, false, onlyActive);
        var latestPreRelease = versions.getLatest(extension, targetPlatform, true, onlyActive);

        var json = extVersion.toExtensionJson();
        json.preview = latest != null ? latest.isPreview() : false;
        json.versionAlias = new ArrayList<>(2);
        if (extVersion.equals(latest))
            json.versionAlias.add("latest");
        if (extVersion.equals(latestPreRelease))
            json.versionAlias.add("pre-release");
        json.verified = isVerified(extVersion);
        json.namespaceAccess = "restricted";
        json.unrelatedPublisher = !json.verified;
        json.reviewCount = repositories.countActiveReviews(extension);
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");

        json.allVersions = Maps.newLinkedHashMap();
        var versionBaseUrl = UrlUtil.createApiVersionBaseUrl(serverUrl, json.namespace, json.name, targetPlatform);
        if (latest != null)
            json.allVersions.put("latest", createApiUrl(versionBaseUrl,"latest"));
        if (latestPreRelease != null)
            json.allVersions.put("pre-release", createApiUrl(versionBaseUrl, "pre-release"));

        if(extension.getVersions() != null) {
            var allVersionsStream = extension.getVersions().stream();
            if (targetPlatform != null) {
                allVersionsStream = allVersionsStream.filter(ev -> ev.getTargetPlatform().equals(targetPlatform));
            }
            if (onlyActive) {
                allVersionsStream = allVersionsStream.filter(ExtensionVersion::isActive);
            }

            allVersionsStream
                    .collect(Collectors.groupingBy(v -> v.getVersion()))
                    .entrySet()
                    .stream()
                    .map(e -> e.getValue().get(0))
                    .sorted(Comparator.<ExtensionVersion, SemanticVersion>comparing(ev -> ev.getSemanticVersion()).reversed())
                    .map(v -> new AbstractMap.SimpleEntry<>(v.getVersion(), createApiUrl(versionBaseUrl, v.getVersion())))
                    .forEach(e -> json.allVersions.put(e.getKey(), e.getValue()));
        }

        var fileUrls = storageUtil.getFileUrls(List.of(extVersion), serverUrl, DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG);
        json.files = fileUrls.get(extVersion.getId());
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
            ExtensionVersionDTO extVersion,
            ExtensionVersionDTO latest,
            ExtensionVersionDTO latestPreRelease,
            long reviewCount,
            boolean preview,
            Set<String> versions,
            List<FileResourceDTO> resources,
            Map<Long, List<NamespaceMembershipDTO>> membershipsByNamespaceId
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
            json.versionAlias.add("latest");
        }
        if (extVersion.equals(latestPreRelease)) {
            json.versionAlias.add("pre-release");
        }

        var allVersions = new ArrayList<String>();
        if(latest != null) {
            allVersions.add("latest");
        }
        if(latestPreRelease != null) {
            allVersions.add("pre-release");
        }
        if(versions != null && !versions.isEmpty()) {
            allVersions.addAll(versions);
        }

        json.allVersions = Maps.newLinkedHashMapWithExpectedSize(allVersions.size());
        var versionBaseUrl = UrlUtil.createApiVersionBaseUrl(serverUrl, json.namespace, json.name, json.targetPlatform);
        for(var version : allVersions) {
            json.allVersions.put(version, createApiUrl(versionBaseUrl, version));
        }

        json.files = Maps.newLinkedHashMapWithExpectedSize(6);
        var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, json.namespace, json.name, json.targetPlatform, json.version);
        for (var resource : resources) {
            var fileUrl = UrlUtil.createApiFileUrl(fileBaseUrl, resource.getName());
            json.files.put(resource.getType(), fileUrl);
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
        return repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER) > 0
                && repositories.countMemberships(user, namespace) > 0;
    }

    private boolean isVerified(ExtensionVersionDTO extVersion, Map<Long, List<NamespaceMembershipDTO>> membershipsByNamespaceId) {
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
                && memberships.stream().anyMatch(m -> m.getUserId() == user.getId());
    }
}