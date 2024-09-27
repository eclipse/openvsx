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
import io.micrometer.core.instrument.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.adapter.ExtensionQueryParam.Criterion.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryParam.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Extension.FLAG_PREVIEW;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.ExtensionFile.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Property.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Statistic.*;
import static org.eclipse.openvsx.entities.FileResource.*;

@Component
public class LocalVSCodeService implements IVSCodeService {

    private final RepositoryService repositories;
    private final VersionService versions;
    private final SearchUtilService search;
    private final StorageUtilService storageUtil;
    private final ExtensionVersionIntegrityService integrityService;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    public LocalVSCodeService(
            RepositoryService repositories,
            VersionService versions,
            SearchUtilService search,
            StorageUtilService storageUtil,
            ExtensionVersionIntegrityService integrityService
    ) {
        this.repositories = repositories;
        this.versions = versions;
        this.search = search;
        this.storageUtil = storageUtil;
        this.integrityService = integrityService;
    }

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
        List<Extension> extensionsList;
        if (!extensionIds.isEmpty()) {
            extensionsList = repositories.findActiveExtensionsByPublicId(extensionIds, BuiltInExtensionUtil.getBuiltInNamespace());
        } else if (!extensionNames.isEmpty()) {
            extensionsList = extensionNames.stream()
                    .map(NamingUtil::fromExtensionId)
                    .filter(Objects::nonNull)
                    .filter(extensionId -> !BuiltInExtensionUtil.isBuiltIn(extensionId.namespace()))
                    .map(extensionId -> repositories.findActiveExtension(extensionId.extension(), extensionId.namespace()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else if (!search.isEnabled()) {
            extensionsList = Collections.emptyList();
        } else {
            try {
                var pageOffset = pageNumber * pageSize;
                var searchOptions = new SearchUtilService.Options(queryString, category, targetPlatform, pageSize,
                        pageOffset, sortOrder, sortBy, false, BuiltInExtensionUtil.getBuiltInNamespace());

                var searchResult = search.search(searchOptions);
                totalCount = searchResult.getTotalHits();
                var ids = searchResult.getSearchHits().stream()
                        .map(hit -> hit.getContent().id)
                        .collect(Collectors.toList());

                var extensionsMap = repositories.findActiveExtensionsById(ids).stream()
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
        List<ExtensionVersion> allActiveExtensionVersions = repositories.findActiveExtensionVersions(extensionsMap.keySet(), targetPlatform);

        List<ExtensionVersion> extensionVersions;
        if (test(flags, FLAG_INCLUDE_LATEST_VERSION_ONLY)) {
            extensionVersions = allActiveExtensionVersions.stream()
                    .collect(Collectors.groupingBy(ev -> ev.getExtension().getId() + "@" + ev.getTargetPlatform()))
                    .values()
                    .stream()
                    .map(list -> versions.getLatest(list, true))
                    .collect(Collectors.toList());
        } else if (test(flags, FLAG_INCLUDE_VERSIONS) || test(flags, FLAG_INCLUDE_VERSION_PROPERTIES)) {
            extensionVersions = allActiveExtensionVersions;
        } else {
            extensionVersions = Collections.emptyList();
        }

        var comparator = Comparator.<ExtensionVersion, Long>comparing(ev -> ev.getExtension().getId())
                .thenComparing(ExtensionVersion.SORT_COMPARATOR);

        var extensionVersionsMap = extensionVersions.stream()
                .map(ev -> {
                    ev.setExtension(extensionsMap.get(ev.getExtension().getId()));
                    return ev;
                })
                .sorted(comparator)
                .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()));

        Map<Long, List<FileResource>> fileResources;
        if (test(flags, FLAG_INCLUDE_FILES) && !extensionVersionsMap.isEmpty()) {
            var types = new ArrayList<>(List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG, VSIXMANIFEST));
            if(integrityService.isEnabled()) {
                types.add(DOWNLOAD_SIG);
            }

            var idsMap = extensionVersionsMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(ev -> ev.getId(), ev -> ev));

            fileResources = repositories.findFileResourcesByExtensionVersionIdAndType(idsMap.keySet(), types).stream()
                    .map(r -> {
                        r.setExtension(idsMap.get(r.getExtension().getId()));
                        return r;
                    })
                    .collect(Collectors.groupingBy(fr -> fr.getExtension().getId()));
        } else {
            fileResources = Collections.emptyMap();
        }

        var latestVersions = allActiveExtensionVersions.stream()
                .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, false))
                .collect(Collectors.toMap(ev -> ev.getExtension().getId(), ev -> ev));

        var extensionQueryResults = new ArrayList<ExtensionQueryResult.Extension>();
        for(var extension : extensionsList) {
            var latest = latestVersions.get(extension.getId());
            var queryExt = toQueryExtension(extension, latest, flags);
            queryExt.versions = extensionVersionsMap.getOrDefault(extension.getId(), Collections.emptyList()).stream()
                    .map(extVer -> toQueryVersion(extVer, fileResources, flags))
                    .collect(Collectors.toList());

            extensionQueryResults.add(queryExt);
        }

        return toQueryResult(extensionQueryResults, totalCount);
    }

    private String createFileUrl(List<FileResource> singleResource, String fileBaseUrl) {
        if(singleResource == null || singleResource.isEmpty()) {
            return null;
        }

        return createFileUrl(singleResource.get(0), fileBaseUrl);
    }

    private String createFileUrl(FileResource resource, String fileBaseUrl) {
        return resource != null ? UrlUtil.createApiFileUrl(fileBaseUrl, resource.getName()) : null;
    }

    public ExtensionQueryResult toQueryResult(List<ExtensionQueryResult.Extension> extensions) {
        return toQueryResult(extensions, extensions.size());
    }

    public ExtensionQueryResult toQueryResult(List<ExtensionQueryResult.Extension> extensions, long totalCount) {
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
    public ResponseEntity<byte[]> getAsset(
            String namespace, String extensionName, String version, String assetType, String targetPlatform,
            String restOfTheUrl
    ) {
        if(BuiltInExtensionUtil.isBuiltIn(namespace)) {
            return new ResponseEntity<>(("Built-in extension namespace '" + namespace + "' not allowed").getBytes(StandardCharsets.UTF_8), null, HttpStatus.BAD_REQUEST);
        }

        var asset = (restOfTheUrl != null && !restOfTheUrl.isEmpty()) ? (assetType + "/" + restOfTheUrl) : assetType;
        if((asset.equals(FILE_PUBLIC_KEY) || asset.equals(FILE_SIGNATURE)) && !integrityService.isEnabled()) {
            throw new NotFoundException();
        }

        var tags = new ArrayList<>(List.of(
                Tag.of("namespace", namespace.toLowerCase()),
                Tag.of("extension", extensionName.toLowerCase()),
                Tag.of("target", targetPlatform.toLowerCase()),
                Tag.of("version", version.toLowerCase())
        ));
        if(asset.equals(FILE_PUBLIC_KEY)) {
            var publicId = repositories.findSignatureKeyPairPublicId(namespace, extensionName, targetPlatform, version);
            if(publicId == null) {
                throw new NotFoundException();
            } else {
                tags.add(Tag.of("type", "publicKey"));
                Metrics.counter("vscode.assets", tags).increment();
                return ResponseEntity
                        .status(HttpStatus.FOUND)
                        .location(URI.create(UrlUtil.getPublicKeyUrl(publicId)))
                        .build();
            }
        }

        var assets = Map.of(
                FILE_VSIX, DOWNLOAD,
                FILE_MANIFEST, MANIFEST,
                FILE_DETAILS, README,
                FILE_CHANGELOG, CHANGELOG,
                FILE_LICENSE, LICENSE,
                FILE_ICON, ICON,
                FILE_VSIXMANIFEST, VSIXMANIFEST,
                FILE_SIGNATURE, DOWNLOAD_SIG
        );

        FileResource resource;
        var type = assets.get(assetType);
        if(type != null) {
            resource = repositories.findFileByType(namespace, extensionName, targetPlatform, version, type);
        } else {
            var name = asset.startsWith(FILE_WEB_RESOURCES)
                    ? asset.substring((FILE_WEB_RESOURCES.length()))
                    : null;

            resource = name != null && name.startsWith("extension/") // is web resource
                    ? repositories.findFileByTypeAndName(namespace, extensionName, targetPlatform, version, FileResource.RESOURCE, name)
                    : null;
        }
        if (resource == null) {
            throw new NotFoundException();
        }
        if (resource.getType().equals(FileResource.DOWNLOAD)) {
            storageUtil.increaseDownloadCount(resource);
        }

        tags.add(Tag.of("type", resource.getType()));
        tags.add(Tag.of("filename", resource.getName()));
        Metrics.counter("vscode.assets", tags).increment();

        return storageUtil.getFileResponse(resource);
    }

    @Override
    public String getItemUrl(String namespaceName, String extensionName) {
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in extension namespace '" + namespaceName + "' not allowed");
        }

        var extension = repositories.findActiveExtension(extensionName, namespaceName);
        if (extension == null) {
            throw new NotFoundException();
        }

        return UrlUtil.createApiUrl(webuiUrl, "extension", extension.getNamespace().getName(), extension.getName());
    }

    @Override
    public String download(String namespaceName, String extensionName, String version, String targetPlatform) {
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in extension namespace '" + BuiltInExtensionUtil.getBuiltInNamespace() + "' not allowed");
        }

        var resource = repositories.findFileByType(namespaceName, extensionName, targetPlatform, version, FileResource.DOWNLOAD);
        if (resource == null) {
            throw new NotFoundException();
        }

        if(resource.getStorageType().equals(STORAGE_DB)) {
            var extVersion = resource.getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var apiUrl = UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), "vscode", "asset", namespace.getName(), extension.getName(), extVersion.getVersion(), FILE_VSIX);
            if(!TargetPlatform.isUniversal(extVersion.getTargetPlatform())) {
                apiUrl = UrlUtil.addQuery(apiUrl, "targetPlatform", extVersion.getTargetPlatform());
            }

            return apiUrl;
        } else {
            storageUtil.increaseDownloadCount(resource);
            return storageUtil.getLocation(resource).toString();
        }
    }

    @Override
    public ResponseEntity<byte[]> browse(String namespaceName, String extensionName, String version, String path) {
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            return new ResponseEntity<>(("Built-in extension namespace '" + BuiltInExtensionUtil.getBuiltInNamespace() + "' not allowed").getBytes(StandardCharsets.UTF_8), null, HttpStatus.BAD_REQUEST);
        }

        var extVersion = repositories.findActiveExtensionVersion(version, extensionName, namespaceName);
        if (extVersion == null) {
            throw new NotFoundException();
        }

        var resources = repositories.findResourceFileResources(extVersion.getId(), path);
        if(resources.isEmpty()) {
            throw new NotFoundException();
        }

        var exactMatch = resources.stream()
                .filter(r -> r.getName().equals(path))
                .findFirst()
                .orElse(null);

        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();

        Metrics.counter("vscode.unpkg", List.of(
                Tag.of("namespace", namespace.getName()),
                Tag.of("extension", extension.getName()),
                Tag.of("version", extVersion.getVersion()),
                Tag.of("file", String.valueOf(exactMatch != null)),
                Tag.of("path", path)
        )).increment();

        return exactMatch != null
                ? browseFile(exactMatch, extVersion)
                : browseDirectory(resources, namespace.getName(), extension.getName(), extVersion.getVersion(), path);
    }

    private ResponseEntity<byte[]> browseFile(
            FileResource resource,
            ExtensionVersion extVersion
    ) {
        if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var headers = storageUtil.getFileResponseHeaders(resource.getName());
            return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
        } else {
            resource.setExtension(extVersion);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(storageUtil.getLocation(resource))
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                    .build();
        }
    }

    private ResponseEntity<byte[]> browseDirectory(
            List<FileResource> resources,
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

    private ExtensionQueryResult.Extension toQueryExtension(Extension extension, ExtensionVersion latest, int flags) {
        var namespace = extension.getNamespace();

        var queryExt = new ExtensionQueryResult.Extension();
        queryExt.extensionId = extension.getPublicId();
        queryExt.extensionName = extension.getName();
        queryExt.displayName = latest.getDisplayName();
        queryExt.shortDescription = latest.getDescription();
        queryExt.publisher = new ExtensionQueryResult.Publisher();
        queryExt.publisher.publisherId = namespace.getPublicId();
        queryExt.publisher.publisherName = namespace.getName();
        queryExt.publisher.displayName = !StringUtils.isEmpty(namespace.getDisplayName()) ? namespace.getDisplayName() : namespace.getName();
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
            ratingCountStat.value = Optional.ofNullable(extension.getReviewCount()).orElse(0L);
            queryExt.statistics.add(ratingCountStat);
        }

        return queryExt;
    }

    private ExtensionQueryResult.ExtensionVersion toQueryVersion(
            ExtensionVersion extVer,
            Map<Long, List<FileResource>> fileResources,
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
            queryVer.addProperty(PROP_SPONSOR_LINK, extVer.getSponsorLink());
            queryVer.addProperty(PROP_ENGINE, getVscodeEngine(extVer));
            var dependencies = extVer.getDependencies().stream()
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_DEPENDENCY, dependencies);
            var bundledExtensions = extVer.getBundledExtensions().stream()
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_EXTENSION_PACK, bundledExtensions);
            var localizedLanguages = extVer.getLocalizedLanguages().stream()
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_LOCALIZED_LANGUAGES, localizedLanguages);
            if (extVer.isPreRelease()) {
                queryVer.addProperty(PROP_PRE_RELEASE, "true");
            }
            if (isWebExtension(extVer)) {
                queryVer.addProperty(PROP_WEB_EXTENSION, "true");
            }
        }

        if(fileResources.containsKey(extVer.getId())) {
            var resourcesByType = fileResources.get(extVer.getId()).stream()
                    .collect(Collectors.groupingBy(FileResource::getType));

            var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, namespaceName, extensionName, extVer.getTargetPlatform(), extVer.getVersion());

            queryVer.files = Lists.newArrayList();
            queryVer.addFile(FILE_MANIFEST, createFileUrl(resourcesByType.get(MANIFEST), fileBaseUrl));
            queryVer.addFile(FILE_DETAILS, createFileUrl(resourcesByType.get(README), fileBaseUrl));
            queryVer.addFile(FILE_LICENSE, createFileUrl(resourcesByType.get(LICENSE), fileBaseUrl));
            queryVer.addFile(FILE_ICON, createFileUrl(resourcesByType.get(ICON), fileBaseUrl));
            queryVer.addFile(FILE_VSIX, createFileUrl(resourcesByType.get(DOWNLOAD), fileBaseUrl));
            queryVer.addFile(FILE_CHANGELOG, createFileUrl(resourcesByType.get(CHANGELOG), fileBaseUrl));
            queryVer.addFile(FILE_VSIXMANIFEST, createFileUrl(resourcesByType.get(VSIXMANIFEST), fileBaseUrl));
            queryVer.addFile(FILE_SIGNATURE, createFileUrl(resourcesByType.get(DOWNLOAD_SIG), fileBaseUrl));
            if(resourcesByType.containsKey(DOWNLOAD_SIG)) {
                queryVer.addFile(FILE_PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVer));
            }
        }

        return queryVer;
    }

    private String getVscodeEngine(ExtensionVersion extVer) {
        if (extVer.getEngines() == null)
            return null;
        return extVer.getEngines().stream()
                .filter(engine -> engine.startsWith("vscode@"))
                .findFirst()
                .map(engine -> engine.substring("vscode@".length()))
                .orElse(null);
    }

    private boolean isWebExtension(ExtensionVersion extVer) {
        return extVer.getExtensionKind() != null && extVer.getExtensionKind().contains("web");
    }

    private boolean isWebResource(FileResource resource) {
        return resource.getType().equals(FileResource.RESOURCE) && resource.getName().startsWith("extension/");
    }

    private boolean test(int flags, int flag) {
        return (flags & flag) != 0;
    }
}
