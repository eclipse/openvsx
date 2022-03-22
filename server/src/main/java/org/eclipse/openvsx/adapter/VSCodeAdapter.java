/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import com.google.common.collect.Lists;
import org.eclipse.openvsx.dto.*;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
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

@RestController
public class VSCodeAdapter {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    RepositoryService repositories;

    @Autowired
    VSCodeIdService idService;

    @Autowired
    SearchUtilService search;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    @PostMapping(
        path = "/vscode/gallery/extensionquery",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ExtensionQueryResult extensionQuery(@RequestBody ExtensionQueryParam param) {
        String targetPlatform;
        String queryString = null;
        String category = null;
        PageRequest pageRequest;
        String sortOrder;
        String sortBy;
        Set<String> extensionIds;
        Set<String> extensionNames;
        if (param.filters == null || param.filters.isEmpty()) {
            pageRequest = PageRequest.of(0, DEFAULT_PAGE_SIZE);
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

            var pageSize = filter.pageSize > 0 ? filter.pageSize : DEFAULT_PAGE_SIZE;
            pageRequest = PageRequest.of(filter.pageNumber - 1, pageSize);
            sortOrder = getSortOrder(filter.sortOrder);
            sortBy = getSortBy(filter.sortBy);
        }

        Long totalCount = null;
        List<ExtensionDTO> extensions;
        if (!extensionIds.isEmpty()) {
            extensions = repositories.findAllActiveExtensionDTOsByPublicId(extensionIds);
        } else if (!extensionNames.isEmpty()) {
            extensions = extensionNames.stream()
                    .map(name -> name.split("\\."))
                    .filter(split -> split.length == 2)
                    .map(split -> {
                        var name = split[1];
                        var namespaceName = split[0];
                        return repositories.findActiveExtensionDTO(name, namespaceName);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } else if (!search.isEnabled()) {
            extensions = Collections.emptyList();
        } else {
            try {
                var offset = pageRequest.getPageNumber() * pageRequest.getPageSize();
                var searchOptions = new SearchUtilService.Options(queryString, category, targetPlatform, pageRequest.getPageSize(),
                        offset, sortOrder, sortBy, false);

                var searchResult = search.search(searchOptions, pageRequest);
                totalCount = searchResult.getTotalHits();
                var ids = searchResult.getSearchHits().stream()
                        .map(hit -> hit.getContent().id)
                        .collect(Collectors.toList());

                var extensionsMap = repositories.findAllActiveExtensionDTOsById(ids).stream()
                        .collect(Collectors.toMap(e -> e.getId(), e -> e));

                // keep the same order as search results
                extensions = ids.stream()
                        .map(extensionsMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } catch (ErrorResultException exc) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exc.getMessage(), exc);
            }
        }
        if(totalCount == null) {
            totalCount = (long) extensions.size();
        }


        var flags = param.flags;
        var extensionsMap = extensions.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        List<ExtensionVersionDTO> allActiveExtensionVersions = repositories.findAllActiveExtensionVersionDTOs(extensionsMap.keySet(), targetPlatform);

        List<ExtensionVersionDTO> extensionVersions;
        if (test(flags, FLAG_INCLUDE_LATEST_VERSION_ONLY)) {
            extensionVersions = allActiveExtensionVersions.stream()
                    .collect(Collectors.groupingBy(ev -> ev.getExtensionId() + "@" + ev.getTargetPlatform()))
                    .values()
                    .stream()
                    .map(VersionUtil::getLatest)
                    .collect(Collectors.toList());
        } else if (test(flags, FLAG_INCLUDE_VERSIONS) || test(flags, FLAG_INCLUDE_VERSION_PROPERTIES)) {
            extensionVersions = allActiveExtensionVersions;
        } else {
            extensionVersions = Collections.emptyList();
        }

        // similar to ExtensionVersion.SORT_COMPARATOR, difference is that it compares by extension id first
        var comparator = Comparator.<ExtensionVersionDTO, Long>comparing(ev -> ev.getExtension().getId())
                .thenComparing(ExtensionVersionDTO::getSemanticVersion)
                .thenComparing(ExtensionVersionDTO::getTimestamp)
                .reversed();

        var extensionVersionsMap = extensionVersions.stream()
                .map(ev -> {
                    ev.setExtension(extensionsMap.get(ev.getExtensionId()));
                    return ev;
                })
                .sorted(comparator)
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtension));

        Map<ExtensionVersionDTO, List<FileResourceDTO>> resources;
        if (test(flags, FLAG_INCLUDE_FILES) && !extensionVersionsMap.isEmpty()) {
            var types = List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG, WEB_RESOURCE);
            var idsMap = extensionVersionsMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(ev -> ev.getId(), ev -> ev));

            resources = repositories.findAllFileResourceDTOsByExtensionVersionIdAndType(idsMap.keySet(), types).stream()
                    .map(r -> {
                        r.setExtensionVersion(idsMap.get(r.getExtensionVersionId()));
                        return r;
                    })
                    .collect(Collectors.groupingBy(FileResourceDTO::getExtensionVersion));
        } else {
            resources = Collections.emptyMap();
        }

        Map<Long, Integer> activeReviewCounts;
        if(test(flags, FLAG_INCLUDE_STATISTICS) && !extensions.isEmpty()) {
            var ids = extensions.stream().map(ExtensionDTO::getId).collect(Collectors.toList());
            activeReviewCounts = repositories.findAllActiveReviewCountsByExtensionId(ids);
        } else {
            activeReviewCounts = Collections.emptyMap();
        }

        var latestVersions = allActiveExtensionVersions.stream()
                .collect(Collectors.groupingBy(ExtensionVersionDTO::getExtensionId))
                .values()
                .stream()
                .map(VersionUtil::getLatest)
                .collect(Collectors.toMap(ExtensionVersionDTO::getExtensionId, ev -> ev));

        var extensionQueryResults = new ArrayList<ExtensionQueryResult.Extension>();
        for(var extension : extensions) {
            var latest = latestVersions.get(extension.getId());
            var queryExt = toQueryExtension(extension, latest, activeReviewCounts, flags);
            queryExt.versions = extensionVersionsMap.getOrDefault(extension, Collections.emptyList()).stream()
                    .map(extVer -> toQueryVersion(extVer, resources, flags))
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
        countMetadata.metadataItems = Lists.newArrayList(countMetadataItem);
        resultItem.resultMetadata = Lists.newArrayList(countMetadata);

        var result = new ExtensionQueryResult();
        result.results = Lists.newArrayList(resultItem);
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

    @GetMapping("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}/**")
    @CrossOrigin
    public ResponseEntity<byte[]> getAsset(HttpServletRequest request,
                                           @PathVariable String namespace,
                                           @PathVariable String extensionName,
                                           @PathVariable String version,
                                           @PathVariable String assetType,
                                           @RequestParam(defaultValue = TargetPlatform.NAME_UNIVERSAL) String targetPlatform) {
        var restOfTheUrl = UrlUtil.extractWildcardPath(request);
        var asset = (restOfTheUrl != null && restOfTheUrl.length() > 0) ? (assetType + "/" + restOfTheUrl) : assetType;
        var extVersion = repositories.findVersion(version, targetPlatform, extensionName, namespace);
        if (extVersion == null || !extVersion.isActive())
            throw new NotFoundException();
        var resource = getFileFromDB(extVersion, asset);
        if (resource == null)
            throw new NotFoundException();
        if (resource.getType().equals(FileResource.DOWNLOAD))
            storageUtil.increaseDownloadCount(extVersion, resource);
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
                if (assetType.startsWith(FILE_WEB_RESOURCES)) {
                    var name = assetType.substring((FILE_WEB_RESOURCES.length()));
                    return repositories.findFileByTypeAndName(extVersion, FileResource.WEB_RESOURCE, name);
                } else {
                    return null;
                }
            }
        }
    }

    @GetMapping("/vscode/item")
    @CrossOrigin
    public ModelAndView getItemUrl(@RequestParam String itemName, ModelMap model) {
        var dotIndex = itemName.indexOf('.');
        if (dotIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expecting an item of the form `{publisher}.{name}`");
        }
        var namespace = itemName.substring(0, dotIndex);
        var extension = itemName.substring(dotIndex + 1);
        return new ModelAndView("redirect:" + UrlUtil.createApiUrl(webuiUrl, "extension", namespace, extension), model);
    }

    @GetMapping("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage")
    @CrossOrigin
    public ModelAndView download(@PathVariable String namespace, @PathVariable String extension,
                                 @PathVariable String version, @RequestParam(defaultValue = TargetPlatform.NAME_UNIVERSAL) String targetPlatform, ModelMap model) {
        if (googleStorage.isEnabled()) {
            var extVersion = repositories.findVersion(version, targetPlatform, extension, namespace);
            if (extVersion == null || !extVersion.isActive())
                throw new NotFoundException();
            var resource = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
            if (resource == null)
                throw new NotFoundException();
            if (resource.getStorageType().equals(FileResource.STORAGE_GOOGLE)) {
                storageUtil.increaseDownloadCount(extVersion, resource);
                return new ModelAndView("redirect:" + storageUtil.getLocation(resource), model);
            }
        }

        var apiUrl = UrlUtil.createApiUrl(UrlUtil.getBaseUrl(), "vscode", "asset", namespace, extension, version, FILE_VSIX);
        if(!TargetPlatform.isUniversal(targetPlatform)) {
            apiUrl = UrlUtil.addQuery(apiUrl, "targetPlatform", targetPlatform);
        }
        
        return new ModelAndView("redirect:" + apiUrl, model);
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
        queryExt.tags = latest.getTags();
        // TODO: add these
        // queryExt.releaseDate
        // queryExt.publishedDate
        // queryExt.lastUpdated
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
            Map<ExtensionVersionDTO, List<FileResourceDTO>> resources,
            int flags
    ) {
        var queryVer = new ExtensionQueryResult.ExtensionVersion();
        queryVer.version = extVer.getVersion();
        queryVer.lastUpdated = extVer.getTimestamp().toString();
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

        if(resources.containsKey(extVer)) {
            var resourcesByType = resources.get(extVer).stream()
                    .collect(Collectors.groupingBy(FileResourceDTO::getType));

            var webResources = resourcesByType.remove(WEB_RESOURCE);
            var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, namespaceName, extensionName, extVer.getTargetPlatform(), extVer.getVersion());

            queryVer.files = Lists.newArrayList();
            queryVer.addFile(FILE_MANIFEST, createFileUrl(resourcesByType.get(MANIFEST), fileBaseUrl));
            queryVer.addFile(FILE_DETAILS, createFileUrl(resourcesByType.get(README), fileBaseUrl));
            queryVer.addFile(FILE_LICENSE, createFileUrl(resourcesByType.get(LICENSE), fileBaseUrl));
            queryVer.addFile(FILE_ICON, createFileUrl(resourcesByType.get(ICON), fileBaseUrl));
            queryVer.addFile(FILE_VSIX, createFileUrl(resourcesByType.get(DOWNLOAD), fileBaseUrl));
            queryVer.addFile(FILE_CHANGELOG, createFileUrl(resourcesByType.get(CHANGELOG), fileBaseUrl));

            if (webResources != null) {
                for (var webResource : webResources) {
                    var name = webResource.getName();
                    var url = createFileUrl(webResource, fileBaseUrl);
                    queryVer.addFile(FILE_WEB_RESOURCES + name, url);
                }
            }
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
}
