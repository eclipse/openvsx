/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.eclipse.openvsx.dto.*;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import static org.eclipse.openvsx.adapter.ExtensionQueryParam.Criterion.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryParam.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Extension.FLAG_PREVIEW;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.ExtensionFile.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Property.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Statistic.*;
import static org.eclipse.openvsx.entities.FileResource.*;

@Component
public class LocalVSCodeService implements IVSCodeService {

    private static final String BUILT_IN_EXTENSION_NAMESPACE = "vscode";

    @Autowired
    RepositoryService repositories;

    @Autowired
    VersionService versions;

    @Autowired
    SearchUtilService search;

    @Autowired
    StorageUtilService storageUtil;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @Override
    public ExtensionQueryResult extensionQuery(ExtensionQueryParam param, int defaultPageSize) {
        String targetPlatform;
        String queryString = null;
        String category = null;
        int pageNumber;
        int pageSize;
        String sortOrder;
        String sortBy;
        Set<String> extensionIds;
        Set<String> extensionNames;
        if (param.filters == null || param.filters.isEmpty()) {
            pageNumber = 0;
            pageSize = defaultPageSize;
            sortBy = "relevance";
            sortOrder = "desc";
            targetPlatform = null;
            extensionIds = Collections.emptySet();
            extensionNames = Collections.emptySet();
        } else {
            var filter = param.filters.get(0);
            extensionIds = new HashSet<>(filter.findCriteria(FILTER_EXTENSION_ID));
            extensionNames = new HashSet<>(filter.findCriteria(FILTER_EXTENSION_NAME));

            queryString = filter.findCriterion(FILTER_SEARCH_TEXT);
            if (queryString == null)
                queryString = filter.findCriterion(FILTER_TAG);

            category = filter.findCriterion(FILTER_CATEGORY);
            var targetCriterion = filter.findCriterion(FILTER_TARGET);
            targetPlatform = TargetPlatform.isValid(targetCriterion) ? targetCriterion : null;

            pageNumber = Math.max(0, filter.pageNumber - 1);
            pageSize = filter.pageSize > 0 ? filter.pageSize : defaultPageSize;
            sortOrder = getSortOrder(filter.sortOrder);
            sortBy = getSortBy(filter.sortBy);
        }

        Long totalCount = null;
        List<ExtensionDTO> extensionsList;
        if (!extensionIds.isEmpty()) {
            extensionsList = repositories.findAllActiveExtensionDTOsByPublicId(extensionIds, BUILT_IN_EXTENSION_NAMESPACE);
        } else if (!extensionNames.isEmpty()) {
            extensionsList = extensionNames.stream()
                    .map(name -> name.split("\\."))
                    .filter(split -> split.length == 2)
                    .filter(split -> !isBuiltInExtensionNamespace(split[0]))
                    .map(split -> {
                        var name = split[1];
                        var namespaceName = split[0];
                        return repositories.findActiveExtensionDTO(name, namespaceName);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else if (!search.isEnabled()) {
            extensionsList = Collections.emptyList();
        } else {
            try {
                var pageOffset = pageNumber * pageSize;
                var searchOptions = new SearchUtilService.Options(queryString, category, targetPlatform, pageSize,
                        pageOffset, sortOrder, sortBy, false, BUILT_IN_EXTENSION_NAMESPACE);

                var searchResult = search.search(searchOptions);
                totalCount = searchResult.getTotalHits();
                var ids = searchResult.getSearchHits().stream()
                        .map(hit -> hit.getContent().id)
                        .collect(Collectors.toList());

                var extensionsMap = repositories.findAllActiveExtensionDTOsById(ids).stream()
                        .collect(Collectors.toMap(e -> e.getId(), e -> e));

                // keep the same order as search results
                extensionsList = ids.stream()
                        .map(extensionsMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (ErrorResultException exc) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exc.getMessage(), exc);
            }
        }
        if(totalCount == null) {
            totalCount = (long) extensionsList.size();
        }

        var flags = param.flags;
        var extensionsMap = extensionsList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        List<ExtensionVersionDTO> allActiveExtensionVersions = repositories.findAllActiveExtensionVersionDTOs(extensionsMap.keySet(), targetPlatform);

        List<ExtensionVersionDTO> extensionVersions;
        if (test(flags, FLAG_INCLUDE_LATEST_VERSION_ONLY)) {
            extensionVersions = allActiveExtensionVersions.stream()
                    .collect(Collectors.groupingBy(ev -> ev.getExtensionId() + "@" + ev.getTargetPlatform()))
                    .values()
                    .stream()
                    .map(list -> versions.getLatest(list, true))
                    .collect(Collectors.toList());
        } else if (test(flags, FLAG_INCLUDE_VERSIONS) || test(flags, FLAG_INCLUDE_VERSION_PROPERTIES)) {
            extensionVersions = allActiveExtensionVersions;
        } else {
            extensionVersions = Collections.emptyList();
        }

        // similar to ExtensionVersion.SORT_COMPARATOR, difference is that it compares by extension id first
        var comparator = Comparator.<ExtensionVersionDTO, Long>comparing(ev -> ev.getExtensionId())
                .thenComparing(ExtensionVersionDTO::getSemanticVersion)
                .thenComparing(ExtensionVersionDTO::getTimestamp)
                .reversed();

        var extensionVersionsMap = extensionVersions.stream()
                .map(ev -> {
                    ev.setExtension(extensionsMap.get(ev.getExtensionId()));
                    return ev;
                })
                .sorted(comparator)
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtensionId));

        Map<Long, List<FileResourceDTO>> fileResources;
        if (test(flags, FLAG_INCLUDE_FILES) && !extensionVersionsMap.isEmpty()) {
            var types = List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG);
            var idsMap = extensionVersionsMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(ev -> ev.getId(), ev -> ev));

            fileResources = repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(idsMap.keySet(), types).stream()
                    .map(r -> {
                        r.setExtensionVersion(idsMap.get(r.getExtensionVersionId()));
                        return r;
                    })
                    .collect(Collectors.groupingBy(FileResourceDTO::getExtensionVersionId));
        } else {
            fileResources = Collections.emptyMap();
        }

        Map<Long, Integer> activeReviewCounts;
        if(test(flags, FLAG_INCLUDE_STATISTICS) && !extensionsList.isEmpty()) {
            var ids = extensionsList.stream().map(ExtensionDTO::getId).collect(Collectors.toList());
            activeReviewCounts = repositories.findAllActiveReviewCountsByExtensionId(ids);
        } else {
            activeReviewCounts = Collections.emptyMap();
        }

        var latestVersions = allActiveExtensionVersions.stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtensionId))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, false))
                .collect(Collectors.toMap(ExtensionVersionDTO::getExtensionId, ev -> ev));

        var extensionQueryResults = new ArrayList<ExtensionQueryResult.Extension>();
        for(var extension : extensionsList) {
            var latest = latestVersions.get(extension.getId());
            var queryExt = toQueryExtension(extension, latest, activeReviewCounts, flags);
            queryExt.versions = extensionVersionsMap.getOrDefault(extension.getId(), Collections.emptyList()).stream()
                    .map(extVer -> toQueryVersion(extVer, fileResources, flags))
                    .collect(Collectors.toList());

            extensionQueryResults.add(queryExt);
        }

        return toQueryResult(extensionQueryResults, totalCount);
    }

    private String createFileUrl(List<FileResourceDTO> singleResource, String fileBaseUrl) {
        if(singleResource == null || singleResource.isEmpty()) {
            return null;
        }

        return createFileUrl(singleResource.get(0), fileBaseUrl);
    }

    private String createFileUrl(FileResourceDTO resource, String fileBaseUrl) {
        return resource != null ? UrlUtil.createApiFileUrl(fileBaseUrl, resource.getName()) : null;
    }

    private ExtensionQueryResult toQueryResult(List<ExtensionQueryResult.Extension> extensions, long totalCount) {
        var resultItem = new ExtensionQueryResult.ResultItem();
        resultItem.extensions = extensions;

        var countMetadataItem = new ExtensionQueryResult.ResultMetadataItem();
        countMetadataItem.name = "TotalCount";
        countMetadataItem.count = totalCount;
        var countMetadata = new ExtensionQueryResult.ResultMetadata();
        countMetadata.metadataType = "ResultCount";
        countMetadata.metadataItems = List.of(countMetadataItem);
        resultItem.resultMetadata = List.of(countMetadata);

        var result = new ExtensionQueryResult();
        result.results = List.of(resultItem);
        return result;
    }

    private String getSortBy(int sortBy) {
        switch (sortBy) {
            case 4: // InstallCount
                return "downloadCount";
            case 5: // PublishedDate
                return "timestamp";
            case 6: // AverageRating
                return "averageRating";
            default:
                return "relevance";
        }
    }

    private String getSortOrder(int sortOrder) {
        switch (sortOrder) {
            case 1: // Ascending
                return "asc";
            default:
                return "desc";
        }
    }

    @Override
    @Transactional
    public ResponseEntity<byte[]> getAsset(
            String namespace, String extensionName, String version, String assetType, String targetPlatform,
            String restOfTheUrl
    ) {
        if(isBuiltInExtensionNamespace(namespace)) {
            return new ResponseEntity<>(("Built-in extension namespace '" + namespace + "' not allowed").getBytes(StandardCharsets.UTF_8), null, HttpStatus.BAD_REQUEST);
        }

        var extVersion = repositories.findVersion(version, targetPlatform, extensionName, namespace);
        if (extVersion == null || !extVersion.isActive()) {
            throw new NotFoundException();
        }

        var asset = (restOfTheUrl != null && restOfTheUrl.length() > 0) ? (assetType + "/" + restOfTheUrl) : assetType;
        var resource = getFileFromDB(extVersion, asset);
        if (resource == null) {
            throw new NotFoundException();
        }
        if (resource.getType().equals(FileResource.DOWNLOAD)) {
            storageUtil.increaseDownloadCount(extVersion, resource);
        }
        if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var headers = storageUtil.getFileResponseHeaders(resource.getName());
            return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(storageUtil.getLocation(resource))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .build();
        }
    }

    private FileResource getFileFromDB(ExtensionVersion extVersion, String assetType) {
        switch (assetType) {
            case FILE_VSIX:
                return repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
            case FILE_MANIFEST:
                return repositories.findFileByType(extVersion, FileResource.MANIFEST);
            case FILE_DETAILS:
                return repositories.findFileByType(extVersion, FileResource.README);
            case FILE_CHANGELOG:
                return repositories.findFileByType(extVersion, FileResource.CHANGELOG);
            case FILE_LICENSE:
                return repositories.findFileByType(extVersion, FileResource.LICENSE);
            case FILE_ICON:
                return repositories.findFileByType(extVersion, FileResource.ICON);
            default: {
                var name = assetType.startsWith(FILE_WEB_RESOURCES)
                        ? assetType.substring((FILE_WEB_RESOURCES.length()))
                        : null;

                return name != null && name.startsWith("extension/") // is web resource
                        ? repositories.findFileByTypeAndName(extVersion, FileResource.RESOURCE, name)
                        : null;
            }
        }
    }

    @Override
    public String getItemUrl(String namespaceName, String extensionName) {
        if(isBuiltInExtensionNamespace(namespaceName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in extension namespace '" + namespaceName + "' not allowed");
        }

        var extension = repositories.findExtension(extensionName, namespaceName);
        if (extension == null || !extension.isActive()) {
            throw new NotFoundException();
        }

        return UrlUtil.createApiUrl(webuiUrl, "extension", namespaceName, extensionName);
    }

    @Override
    public String download(String namespace, String extension, String version, String targetPlatform) {
        if(isBuiltInExtensionNamespace(namespace)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in extension namespace '" + namespace + "' not allowed");
        }

        var extVersion = repositories.findVersion(version, targetPlatform, extension, namespace);
        if (extVersion == null || !extVersion.isActive()) {
            throw new NotFoundException();
        }

        var resource = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
        if (resource == null) {
            throw new NotFoundException();
        }

        if(resource.getStorageType().equals(STORAGE_DB)) {
            var apiUrl = UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), "vscode", "asset", namespace, extension, version, FILE_VSIX);
            if(!TargetPlatform.isUniversal(targetPlatform)) {
                apiUrl = UrlUtil.addQuery(apiUrl, "targetPlatform", targetPlatform);
            }

            return apiUrl;
        } else {
            storageUtil.increaseDownloadCount(extVersion, resource);
            return storageUtil.getLocation(resource).toString();
        }
    }

    @Override
    public ResponseEntity<byte[]> browse(String namespaceName, String extensionName, String version, String path) {
        if(isBuiltInExtensionNamespace(namespaceName)) {
            return new ResponseEntity<>(("Built-in extension namespace '" + namespaceName + "' not allowed").getBytes(StandardCharsets.UTF_8), null, HttpStatus.BAD_REQUEST);
        }

        var extVersions = repositories.findActiveExtensionVersionDTOsByVersion(version, extensionName, namespaceName);
        var extVersion = extVersions.stream().max(Comparator.<ExtensionVersionDTO, Boolean>comparing(TargetPlatform::isUniversal)
                .thenComparing(ExtensionVersionDTO::getTargetPlatform))
                .orElse(null);

        if (extVersion == null) {
            return ResponseEntity.notFound().build();
        }

        var resources = repositories.findAllResourceFileResourceDTOs(extVersion.getId(), path);
        if(resources.isEmpty()) {
            throw new NotFoundException();
        } else if(resources.size() == 1 && resources.get(0).getName().equals(path)) {
            return browseFile(resources.get(0), namespaceName, extensionName, extVersion.getTargetPlatform(), version);
        } else {
            return browseDirectory(resources, namespaceName, extensionName, version, path);
        }
    }

    private ResponseEntity<byte[]> browseFile(
            FileResourceDTO resource,
            String namespaceName,
            String extensionName,
            String targetPlatform,
            String version
    ) {
        if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var headers = storageUtil.getFileResponseHeaders(resource.getName());
            return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
        } else {
            var namespace = new Namespace();
            namespace.setName(namespaceName);

            var extension = new Extension();
            extension.setName(extensionName);
            extension.setNamespace(namespace);

            var extVersion = new ExtensionVersion();
            extVersion.setVersion(version);
            extVersion.setTargetPlatform(targetPlatform);
            extVersion.setExtension(extension);

            var fileResource = new FileResource();
            fileResource.setName(resource.getName());
            fileResource.setExtension(extVersion);
            fileResource.setStorageType(resource.getStorageType());

            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(storageUtil.getLocation(fileResource))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .build();
        }
    }

    private ResponseEntity<byte[]> browseDirectory(
            List<FileResourceDTO> resources,
            String namespaceName,
            String extensionName,
            String version,
            String path
    ) {
        if(!path.isEmpty() && !path.endsWith("/")) {
            path += "/";
        }

        var urls = new HashSet<String>();
        var baseUrl = UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), "vscode", "unpkg", namespaceName, extensionName, version);
        for(var resource : resources) {
            var name = resource.getName();
            if(name.startsWith(path)) {
                var index = name.indexOf('/', path.length());
                var isDirectory = index != -1;
                if(isDirectory) {
                    name = name.substring(0, index);
                }

                var url = UrlUtil.createApiUrl(baseUrl, name.split("/"));
                if(isDirectory) {
                    url += '/';
                }

                urls.add(url);
            }
        }

        String json;
        try {
            json = new ObjectMapper().writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate JSON: " + e.getMessage());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    private ExtensionQueryResult.Extension toQueryExtension(ExtensionDTO extension, ExtensionVersionDTO latest, Map<Long, Integer> activeReviewCounts, int flags) {
        var namespace = extension.getNamespace();

        var queryExt = new ExtensionQueryResult.Extension();
        queryExt.extensionId = extension.getPublicId();
        queryExt.extensionName = extension.getName();
        queryExt.displayName = latest.getDisplayName();
        queryExt.shortDescription = latest.getDescription();
        queryExt.publisher = new ExtensionQueryResult.Publisher();
        queryExt.publisher.publisherId = namespace.getPublicId();
        queryExt.publisher.publisherName = namespace.getName();
        queryExt.publisher.displayName = namespace.getName();
        queryExt.tags = latest.getTags();
        queryExt.releaseDate = TimeUtil.toUTCString(extension.getPublishedDate());
        queryExt.publishedDate = TimeUtil.toUTCString(extension.getPublishedDate());
        queryExt.lastUpdated = TimeUtil.toUTCString(extension.getLastUpdatedDate());
        queryExt.categories = latest.getCategories();
        queryExt.flags = latest.isPreview() ? FLAG_PREVIEW : "";

        if (test(flags, FLAG_INCLUDE_STATISTICS)) {
            queryExt.statistics = Lists.newArrayList();
            var installStat = new ExtensionQueryResult.Statistic();
            installStat.statisticName = STAT_INSTALL;
            installStat.value = extension.getDownloadCount();
            queryExt.statistics.add(installStat);
            if (extension.getAverageRating() != null) {
                var avgRatingStat = new ExtensionQueryResult.Statistic();
                avgRatingStat.statisticName = STAT_AVERAGE_RATING;
                avgRatingStat.value = extension.getAverageRating();
                queryExt.statistics.add(avgRatingStat);
            }
            var ratingCountStat = new ExtensionQueryResult.Statistic();
            ratingCountStat.statisticName = STAT_RATING_COUNT;
            ratingCountStat.value = activeReviewCounts.getOrDefault(extension.getId(), 0);
            queryExt.statistics.add(ratingCountStat);
        }

        return queryExt;
    }

    private ExtensionQueryResult.ExtensionVersion toQueryVersion(
            ExtensionVersionDTO extVer,
            Map<Long, List<FileResourceDTO>> fileResources,
            int flags
    ) {
        var queryVer = new ExtensionQueryResult.ExtensionVersion();
        queryVer.version = extVer.getVersion();
        queryVer.lastUpdated = TimeUtil.toUTCString(extVer.getTimestamp());
        queryVer.targetPlatform = extVer.getTargetPlatform();
        var serverUrl = UrlUtil.getBaseUrl();
        var namespaceName = extVer.getExtension().getNamespace().getName();
        var extensionName = extVer.getExtension().getName();

        if (test(flags, FLAG_INCLUDE_ASSET_URI)) {
            queryVer.assetUri = UrlUtil.createApiUrl(serverUrl, "vscode", "asset", namespaceName, extensionName, extVer.getVersion());
            queryVer.fallbackAssetUri = queryVer.assetUri;
        }
        if (test(flags, FLAG_INCLUDE_VERSION_PROPERTIES)) {
            queryVer.properties = Lists.newArrayList();
            queryVer.addProperty(PROP_BRANDING_COLOR, extVer.getGalleryColor());
            queryVer.addProperty(PROP_BRANDING_THEME, extVer.getGalleryTheme());
            queryVer.addProperty(PROP_REPOSITORY, extVer.getRepository());
            queryVer.addProperty(PROP_ENGINE, getVscodeEngine(extVer));
            var dependencies = extVer.getDependencies().stream()
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_DEPENDENCY, dependencies);
            var bundledExtensions = extVer.getBundledExtensions().stream()
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_EXTENSION_PACK, bundledExtensions);
            queryVer.addProperty(PROP_LOCALIZED_LANGUAGES, "");
            if (extVer.isPreRelease()) {
                queryVer.addProperty(PROP_PRE_RELEASE, "true");
            }
            if (isWebExtension(extVer)) {
                queryVer.addProperty(PROP_WEB_EXTENSION, "true");
            }
        }

        if(fileResources.containsKey(extVer.getId())) {
            var resourcesByType = fileResources.get(extVer.getId()).stream()
                    .collect(Collectors.groupingBy(FileResourceDTO::getType));

            var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, namespaceName, extensionName, extVer.getTargetPlatform(), extVer.getVersion());

            queryVer.files = Lists.newArrayList();
            queryVer.addFile(FILE_MANIFEST, createFileUrl(resourcesByType.get(MANIFEST), fileBaseUrl));
            queryVer.addFile(FILE_DETAILS, createFileUrl(resourcesByType.get(README), fileBaseUrl));
            queryVer.addFile(FILE_LICENSE, createFileUrl(resourcesByType.get(LICENSE), fileBaseUrl));
            queryVer.addFile(FILE_ICON, createFileUrl(resourcesByType.get(ICON), fileBaseUrl));
            queryVer.addFile(FILE_VSIX, createFileUrl(resourcesByType.get(DOWNLOAD), fileBaseUrl));
            queryVer.addFile(FILE_CHANGELOG, createFileUrl(resourcesByType.get(CHANGELOG), fileBaseUrl));
        }

        return queryVer;
    }

    private String getVscodeEngine(ExtensionVersionDTO extVer) {
        if (extVer.getEngines() == null)
            return null;
        return extVer.getEngines().stream()
                .filter(engine -> engine.startsWith("vscode@"))
                .findFirst()
                .map(engine -> engine.substring("vscode@".length()))
                .orElse(null);
    }

    private boolean isWebExtension(ExtensionVersionDTO extVer) {
        return extVer.getExtensionKind() != null && extVer.getExtensionKind().contains("web");
    }

    private boolean test(int flags, int flag) {
        return (flags & flag) != 0;
    }

    private boolean isBuiltInExtensionNamespace(String namespaceName) {
        return namespaceName.equals(BUILT_IN_EXTENSION_NAMESPACE);
    }
}
