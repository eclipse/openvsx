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

import static org.eclipse.openvsx.entities.FileResource.*;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.eclipse.openvsx.dto.*;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.QueryParamJson;
import org.eclipse.openvsx.json.QueryResultJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.CollectionUtil;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalRegistryService implements IExtensionRegistry {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    ExtensionService extensions;

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
    public ExtensionJson getExtension(String namespace, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive())
            throw new NotFoundException();
        return toExtensionVersionJson(extension.getLatest(), true);
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName, String version) {
        var extVersion = findVersion(namespace, extensionName, version);
        if (extVersion == null || !extVersion.isActive())
            throw new NotFoundException();
        return toExtensionVersionJson(extVersion, true);
    }

    private ExtensionVersion findVersion(String namespace, String extensionName, String version) {
        if ("latest".equals(version)) {
            var extension = repositories.findExtension(extensionName, namespace);
            if (extension == null || !extension.isActive())
                return null;
            return extension.getLatest();
        } else if ("pre-release".equals(version)) {
            var extension = repositories.findExtension(extensionName, namespace);
            if (extension == null || !extension.isActive())
                return null;
            return extension.getLatestPreRelease();
        } else {
            return repositories.findVersion(version, extensionName, namespace);
        }
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extensionName, String version, String fileName) {
        var extVersion = findVersion(namespace, extensionName, version);
        if (extVersion == null || !extVersion.isActive())
            throw new NotFoundException();
        var resource = repositories.findFileByName(extVersion, fileName);
        if (resource == null)
            throw new NotFoundException();
        if (resource.getType().equals(DOWNLOAD))
            storageUtil.increaseDownloadCount(extVersion, resource);
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
    public SearchResultJson search(ISearchService.Options options) {
        var json = new SearchResultJson();
        var size = options.requestedSize;
        if (size <= 0 || !search.isEnabled()) {
            json.extensions = Collections.emptyList();
            return json;
        }

        var offset = options.requestedOffset;
        var pageRequest = PageRequest.of(offset / size, size);
        var searchHits = search.search(options, pageRequest);
        json.extensions = toSearchEntries(searchHits, size, offset % size, options);
        json.offset = offset;
        json.totalSize = (int) searchHits.getTotalHits();
        if (json.extensions.size() < size && searchHits.getTotalHits() > offset + size) {
            // This is necessary when offset % size > 0
            var remainder = search.search(options, pageRequest.next());
            json.extensions.addAll(toSearchEntries(remainder, size - json.extensions.size(), 0, options));
        }
        return json;
    }

    private List<SearchEntryJson> toSearchEntries(SearchHits<ExtensionSearch> hits, int size, int offset, ISearchService.Options options) {
        var serverUrl = UrlUtil.getBaseUrl();
        var content = hits.getSearchHits();
        if (offset > 0 || size < content.size())
            return CollectionUtil.map(
                    Iterables.limit(Iterables.skip(content, offset), size),
                    hit -> toSearchEntry(hit, serverUrl, options));
        else
            return CollectionUtil.map(content, hit -> toSearchEntry(hit, serverUrl, options));
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

        // Add extension by UUID (public_id)
        if (!Strings.isNullOrEmpty(param.extensionUuid)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByExtensionPublicId(param.extensionUuid));
        }
        // Add extensions by namespace UUID (public_id)
        if (!Strings.isNullOrEmpty(param.namespaceUuid)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByNamespacePublicId(param.namespaceUuid));
        }
        // Add a specific version of an extension
        if (!Strings.isNullOrEmpty(param.namespaceName) && !Strings.isNullOrEmpty(param.extensionName)
                && !Strings.isNullOrEmpty(param.extensionVersion) && !param.includeAllVersions) {
            var extensionVersion = repositories.findActiveExtensionVersionDTOByVersion(param.extensionVersion, param.extensionName, param.namespaceName);
            if(extensionVersion != null) {
                extensionVersions.add(extensionVersion);
            }
        // Add extension by namespace and name
        } else if (!Strings.isNullOrEmpty(param.namespaceName) && !Strings.isNullOrEmpty(param.extensionName)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByExtensionName(param.extensionName, param.namespaceName));
        // Add extensions by namespace
        } else if (!Strings.isNullOrEmpty(param.namespaceName)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByNamespaceName(param.namespaceName));
        // Add extensions by name
        } else if (!Strings.isNullOrEmpty(param.extensionName)) {
            extensionVersions.addAll(repositories.findActiveExtensionVersionDTOsByExtensionName(param.extensionName));
        }

        extensionVersions = extensionVersions.stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getId))
                .values().stream()
                .map(l -> l.get(0))
                .filter(ev -> addToResult(ev, param))
                .collect(Collectors.toList());

        var extensionVersionsByExtensionId = extensionVersions.stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtensionId));

        var reviewCounts = getReviewCounts(extensionVersionsByExtensionId);
        var versionsByExtensionId = extensionVersionsByExtensionId.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), toVersions(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if(Strings.isNullOrEmpty(param.extensionVersion) && !param.includeAllVersions) {
            var latestIds = extensionVersions.stream()
                    .map(ExtensionVersionDTO::getExtension)
                    .map(ExtensionDTO::getLatestId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            extensionVersions = extensionVersions.stream()
                    .filter(ev -> latestIds.contains(ev.getId()))
                    .collect(Collectors.toList());
        }

        var fileResourcesByExtensionVersionId = getFileResources(extensionVersions);
        var membershipsByNamespaceId = getMemberships(extensionVersions);

        var result = new QueryResultJson();
        result.extensions = extensionVersions.stream()
                .sorted(getExtensionVersionComparator())
                .map(ev -> {
                    var reviewCount = reviewCounts.getOrDefault(ev.getExtensionId(), 0L);
                    var versions = versionsByExtensionId.get(ev.getExtensionId());
                    var fileResources = fileResourcesByExtensionVersionId.getOrDefault(ev.getId(), Collections.emptyList());
                    return toExtensionVersionJson(ev, reviewCount, versions, fileResources, membershipsByNamespaceId);
                })
                .collect(Collectors.toList());

        return result;
    }

    private Map<Long, Long> getReviewCounts(Map<Long, List<ExtensionVersionDTO>> extensionVersionsByExtensionId) {
        if(extensionVersionsByExtensionId.isEmpty()) {
            return Collections.emptyMap();
        }

        return repositories.findAllActiveReviewCountsByExtensionId(extensionVersionsByExtensionId.keySet()).stream()
                .collect(Collectors.toMap(ExtensionReviewCountDTO::getExtensiondId, ExtensionReviewCountDTO::getReviewCount));
    }

    private Map<Long, List<FileResourceDTO>> getFileResources(List<ExtensionVersionDTO> extensionVersions) {
        if(extensionVersions.isEmpty()) {
            return Collections.emptyMap();
        }

        var fileTypes = List.of(DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG);
        var extensionVersionIds = extensionVersions.stream()
                .map(ExtensionVersionDTO::getId)
                .collect(Collectors.toList());

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
                .collect(Collectors.toList());

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

    private List<String> toVersions(List<ExtensionVersionDTO> extensionVersions) {
        return extensionVersions.stream()
                .map(ExtensionVersionDTO::getVersion)
                .map(SemanticVersion::new)
                .sorted(Comparator.reverseOrder())
                .map(SemanticVersion::toString)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson createNamespace(NamespaceJson json, String tokenValue) {
        var namespaceIssue = validator.validateNamespace(json.name);
        if (namespaceIssue.isPresent()) {
            throw new ErrorResultException(namespaceIssue.get().toString());
        }
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            throw new ErrorResultException("Invalid access token.");
        }
        eclipse.checkPublisherAgreement(token.getUser());
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
        membership.setUser(token.getUser());
        membership.setRole(NamespaceMembership.ROLE_CONTRIBUTOR);
        entityManager.persist(membership);

        return ResultJson.success("Created namespace " + namespace.getName());
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ExtensionJson publish(InputStream content, String tokenValue) throws ErrorResultException {
        var token = users.useAccessToken(tokenValue);
        if (token == null || token.getUser() == null) {
            throw new ErrorResultException("Invalid access token.");
        }

        // Check whether the user has a valid publisher agreement
        eclipse.checkPublisherAgreement(token.getUser());

        var extVersion = extensions.publishVersion(content, token);
        return toExtensionVersionJson(extVersion, true);
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

    private SearchEntryJson toSearchEntry(SearchHit<ExtensionSearch> searchHit, String serverUrl, ISearchService.Options options) {
        var searchItem = searchHit.getContent();
        var extension = entityManager.find(Extension.class, searchItem.id);
        if (extension == null || !extension.isActive()){
            extension = new Extension();
            extension.setId(searchItem.id);
            search.removeSearchEntry(extension);
            return null;
        }
        var extVer = extension.getLatest();
        var entry = extVer.toSearchEntryJson();
        entry.url = createApiUrl(serverUrl, "api", entry.namespace, entry.name);
        entry.files = Maps.newLinkedHashMapWithExpectedSize(2);
        storageUtil.addFileUrls(extVer, serverUrl, entry.files, DOWNLOAD, ICON);
        if (options.includeAllVersions) {
            var allVersions = Lists.newArrayList(repositories.findActiveVersions(extension));
            Collections.sort(allVersions, ExtensionVersion.SORT_COMPARATOR);
            entry.allVersions = CollectionUtil.map(allVersions, ev -> toVersionReference(ev, entry, serverUrl));
        }
        return entry;
    }

    private SearchEntryJson.VersionReference toVersionReference(ExtensionVersion extVersion, SearchEntryJson entry, String serverUrl) {
        var json = new SearchEntryJson.VersionReference();
        json.version = extVersion.getVersion();
        json.engines = extVersion.getEnginesMap();
        json.url = createApiUrl(serverUrl, "api", entry.namespace, entry.name, extVersion.getVersion());
        json.files = Maps.newLinkedHashMapWithExpectedSize(1);
        storageUtil.addFileUrls(extVersion, serverUrl, json.files, DOWNLOAD);
        return json;
    }

    public ExtensionJson toExtensionVersionJson(ExtensionVersion extVersion, boolean onlyActive) {
        var extension = extVersion.getExtension();
        var json = extVersion.toExtensionJson();
        json.preview = extension.getLatest().isPreview();
        json.versionAlias = new ArrayList<>(2);
        if (extVersion == extension.getLatest())
            json.versionAlias.add("latest");
        if (extVersion == extension.getLatestPreRelease())
            json.versionAlias.add("pre-release");
        json.verified = isVerified(extVersion);
        json.namespaceAccess = "restricted";
        json.unrelatedPublisher = !json.verified;
        json.reviewCount = repositories.countActiveReviews(extension);
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");

        var versionStrings = onlyActive ? repositories.getActiveVersionStrings(extension) : repositories.getVersionStrings(extension);
        var allVersions = CollectionUtil.map(versionStrings, v -> new SemanticVersion(v));
        Collections.sort(allVersions, Collections.reverseOrder());
        json.allVersions = Maps.newLinkedHashMapWithExpectedSize(allVersions.size() + 2);
        if (extension.getLatest() != null)
            json.allVersions.put("latest", createApiUrl(serverUrl, "api", json.namespace, json.name, "latest"));
        if (extension.getLatestPreRelease() != null)
            json.allVersions.put("pre-release", createApiUrl(serverUrl, "api", json.namespace, json.name, "pre-release"));
        for (var version : allVersions) {
            String url = createApiUrl(serverUrl, "api", json.namespace, json.name, version.toString());
            json.allVersions.put(version.toString(), url);
        }
    
        json.files = Maps.newLinkedHashMapWithExpectedSize(6);
        storageUtil.addFileUrls(extVersion, serverUrl, json.files,
                DOWNLOAD, MANIFEST, ICON, FileResource.README,
                FileResource.LICENSE, FileResource.CHANGELOG);
    
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
            long reviewCount,
            List<String> versions,
            List<FileResourceDTO> resources,
            Map<Long, List<NamespaceMembershipDTO>> membershipsByNamespaceId
    ) {
        var extension = extVersion.getExtension();
        var json = extVersion.toExtensionJson();
        json.preview = extension.isPreview();
        json.versionAlias = new ArrayList<>(2);
        if (extension.getLatestId() != null && extVersion.getId() == extension.getLatestId())
            json.versionAlias.add("latest");
        if (extension.getLatestPreReleaseId() != null && extVersion.getId() == extension.getLatestPreReleaseId())
            json.versionAlias.add("pre-release");
        json.verified = isVerified(extVersion, membershipsByNamespaceId);
        json.namespaceAccess = "restricted";
        json.unrelatedPublisher = !json.verified;
        json.reviewCount = reviewCount;
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");

        var allVersions = new ArrayList<String>();
        if (extension.getLatestId() != null) {
            allVersions.add("latest");
        }
        if (extension.getLatestPreReleaseId() != null) {
            allVersions.add("pre-release");
        }
        if(versions != null) {
            allVersions.addAll(versions);
        }
        json.allVersions = Maps.newLinkedHashMapWithExpectedSize(allVersions.size());
        var versionBaseUrl = createApiUrl(serverUrl, "api", json.namespace, json.name);
        for(var version : allVersions) {
            json.allVersions.put(version, createApiUrl(versionBaseUrl, version));
        }

        json.files = Maps.newLinkedHashMapWithExpectedSize(6);
        var versionUrl = UrlUtil.createApiUrl(versionBaseUrl, json.version);
        for (var resource : resources) {
            var fileUrl = UrlUtil.createApiUrl(versionUrl, "file", resource.getName());
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
        if (extVersion.getPublishedWith() == null)
            return false;
        var user = extVersion.getPublishedWith().getUser();
        var namespace = extVersion.getExtension().getNamespace();
        return repositories.countMemberships(namespace, NamespaceMembership.ROLE_OWNER) > 0
                && repositories.countMemberships(user, namespace) > 0;
    }

    private boolean isVerified(ExtensionVersionDTO extVersion, Map<Long, List<NamespaceMembershipDTO>> membershipsByNamespaceId) {
        if (extVersion.getPublishedWith() == null)
            return false;
        var user = extVersion.getPublishedWith().getUser();
        var namespace = extVersion.getExtension().getNamespace().getId();

        var memberships = membershipsByNamespaceId.get(namespace);
        return memberships.stream().anyMatch(m -> m.getRole().equalsIgnoreCase(NamespaceMembership.ROLE_OWNER))
                && memberships.stream().anyMatch(m -> m.getUserId() == user.getId());
    }
}