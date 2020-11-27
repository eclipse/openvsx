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

import static org.eclipse.openvsx.adapter.ExtensionQueryParam.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryParam.Criterion.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Extension.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.ExtensionFile.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Property.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Statistic.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.CollectionUtil;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class VSCodeAdapter {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    VSCodeIdService idService;

    @Autowired
    SearchService search;

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
        String queryString = null;
        String category = null;
        PageRequest pageRequest;
        String sortOrder;
        String sortBy;
        if (param.filters == null || param.filters.isEmpty()) {
            pageRequest = PageRequest.of(0, 20);
            sortBy = "relevance";
            sortOrder = "desc";
        } else {
            var filter = param.filters.get(0);
    
            var extensionIds = filter.findCriteria(FILTER_EXTENSION_ID);
            if (!extensionIds.isEmpty()) {
                // Find extensions by identifier
                return findExtensionsById(extensionIds, param.flags);
            }
            var extensionNames = filter.findCriteria(FILTER_EXTENSION_NAME);
            if (!extensionNames.isEmpty()) {
                // Find extensions by qualified name
                return findExtensionsByName(extensionNames, param.flags);
            }
    
            queryString = filter.findCriterion(FILTER_SEARCH_TEXT);
            if (queryString == null)
                queryString = filter.findCriterion(FILTER_TAG);
            category = filter.findCriterion(FILTER_CATEGORY);
            pageRequest = PageRequest.of(filter.pageNumber - 1, filter.pageSize);
            sortOrder = getSortOrder(filter.sortOrder);
            sortBy = getSortBy(filter.sortBy);
        }

        if (!search.isEnabled()) {
            return toQueryResult(Collections.emptyList());
        }
        try {
            var searchOptions = new SearchService.Options(queryString, category, pageRequest.getPageSize(),
                    pageRequest.getPageNumber() * pageRequest.getPageSize(), sortOrder, sortBy, false);
            var searchResult = search.search(searchOptions, pageRequest);
            return findExtensions(searchResult, param.flags);
        } catch (ErrorResultException exc) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exc.getMessage(), exc);
        }
    }

    private ExtensionQueryResult findExtensionsById(List<String> ids, int flags) {
        var extensions = new ArrayList<ExtensionQueryResult.Extension>(ids.size());
        for (var uuid : ids) {
            var extension = repositories.findExtensionByPublicId(uuid);
            if (extension != null && extension.isActive()) {
                extensions.add(toQueryExtension(extension, flags));
            }
        }
        return toQueryResult(extensions);
    }

    private ExtensionQueryResult findExtensionsByName(List<String> names, int flags) {
        var extensions = new ArrayList<ExtensionQueryResult.Extension>(names.size());
        for (var qualifiedName : names) {
            var split = qualifiedName.split("\\.");
            if (split.length == 2) {
                var extension = repositories.findExtension(split[1], split[0]);
                if (extension != null && extension.isActive()) {
                    extensions.add(toQueryExtension(extension, flags));
                }
            }
        }
        return toQueryResult(extensions);
    }

    private ExtensionQueryResult toQueryResult(List<ExtensionQueryResult.Extension> extensions) {
        var resultItem = new ExtensionQueryResult.ResultItem();
        resultItem.extensions = extensions;

        var countMetadataItem = new ExtensionQueryResult.ResultMetadataItem();
        countMetadataItem.name = "TotalCount";
        countMetadataItem.count = resultItem.extensions.size();
        var countMetadata = new ExtensionQueryResult.ResultMetadata();
        countMetadata.metadataType = "ResultCount";
        countMetadata.metadataItems = Lists.newArrayList(countMetadataItem);
        resultItem.resultMetadata = Lists.newArrayList(countMetadata);

        var result = new ExtensionQueryResult();
        result.results = Lists.newArrayList(resultItem);
        return result;
    }

    private ExtensionQueryResult findExtensions(Page<ExtensionSearch> searchResult, int flags) {
        var resultItem = new ExtensionQueryResult.ResultItem();
        resultItem.extensions = CollectionUtil.map(searchResult.getContent(), es -> {
            var extension = entityManager.find(Extension.class, es.id);
            if (extension == null)
                return null;
            return toQueryExtension(extension, flags);
        });

        var countMetadataItem = new ExtensionQueryResult.ResultMetadataItem();
        countMetadataItem.name = "TotalCount";
        countMetadataItem.count = searchResult.getTotalElements();
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

    @GetMapping("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType:.+}")
    @CrossOrigin
    public ResponseEntity<byte[]> getAsset(@PathVariable String namespace,
                                          @PathVariable String extensionName,
                                          @PathVariable String version,
                                          @PathVariable String assetType) {
        var extVersion = repositories.findVersion(version, extensionName, namespace);
        if (extVersion == null || !extVersion.isActive())
            throw new NotFoundException();
        var resource = getFileFromDB(extVersion, assetType);
        if (resource == null)
            throw new NotFoundException();
        if (resource.getType().equals(FileResource.DOWNLOAD))
            storageUtil.increaseDownloadCount(extVersion);
        if (resource.getStorageType().equals(FileResource.STORAGE_DB)) {
            var headers = storageUtil.getFileResponseHeaders(resource.getName());
            return new ResponseEntity<>(resource.getContent(), headers, HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(storageUtil.getLocation(resource))
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
            default:
                return null;
        }
    }

    @GetMapping("/vscode/item")
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
    public ModelAndView download(@PathVariable String namespace, @PathVariable String extension,
                                 @PathVariable String version, ModelMap model) {
        if (googleStorage.isEnabled()) {
            var extVersion = repositories.findVersion(version, extension, namespace);
            if (extVersion == null || !extVersion.isActive())
                throw new NotFoundException();
            var resource = repositories.findFileByType(extVersion, FileResource.DOWNLOAD);
            if (resource == null)
                throw new NotFoundException();
            if (resource.getStorageType().equals(FileResource.STORAGE_GOOGLE)) {
                storageUtil.increaseDownloadCount(extVersion);
                return new ModelAndView("redirect:" + storageUtil.getLocation(resource), model);
            }
        }
        var serverUrl = UrlUtil.getBaseUrl();
        return new ModelAndView("redirect:" + UrlUtil.createApiUrl(serverUrl, "vscode", "asset", namespace, extension, version, FILE_VSIX), model);
    }

    private ExtensionQueryResult.Extension toQueryExtension(Extension extension, int flags) {
        if (Strings.isNullOrEmpty(extension.getPublicId())) {
            idService.createPublicId(extension);
        }
        var queryExt = new ExtensionQueryResult.Extension();
        var namespace = extension.getNamespace();
        queryExt.publisher = new ExtensionQueryResult.Publisher();
        queryExt.publisher.publisherId = namespace.getPublicId();
        queryExt.publisher.publisherName = namespace.getName();
        queryExt.extensionId = extension.getPublicId();
        queryExt.extensionName = extension.getName();
        var latest = extension.getLatest();
        queryExt.displayName = latest.getDisplayName();
        queryExt.flags = latest.isPreview() ? FLAG_PREVIEW : "";
        queryExt.shortDescription = latest.getDescription();

        if (test(flags, FLAG_INCLUDE_LATEST_VERSION_ONLY)) {
            queryExt.versions = Lists.newArrayList(toQueryVersion(latest, flags));
        } else if (test(flags, FLAG_INCLUDE_VERSIONS) || test(flags, FLAG_INCLUDE_VERSION_PROPERTIES)) {
            var allVersions = Lists.newArrayList(repositories.findActiveVersions(extension));
            Collections.sort(allVersions, ExtensionVersion.SORT_COMPARATOR);
            queryExt.versions = CollectionUtil.map(allVersions, ev -> toQueryVersion(ev, flags));
        }

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
            ratingCountStat.value = repositories.countActiveReviews(extension);
            queryExt.statistics.add(ratingCountStat);
        }
        return queryExt;
    }

    private ExtensionQueryResult.ExtensionVersion toQueryVersion(ExtensionVersion extVer, int flags) {
        var queryVer = new ExtensionQueryResult.ExtensionVersion();
        queryVer.version = extVer.getVersion();
        queryVer.lastUpdated = extVer.getTimestamp().toString();
        var serverUrl = UrlUtil.getBaseUrl();
        var namespace = extVer.getExtension().getNamespace().getName();
        var extensionName = extVer.getExtension().getName();

        if (test(flags, FLAG_INCLUDE_ASSET_URI)) {
            queryVer.assetUri = UrlUtil.createApiUrl(serverUrl, "vscode", "asset", namespace, extensionName, extVer.getVersion());
            queryVer.fallbackAssetUri = queryVer.assetUri;
        }

        if (test(flags, FLAG_INCLUDE_FILES)) {
            Map<String, String> type2Url = Maps.newHashMapWithExpectedSize(6);
            storageUtil.addFileUrls(extVer, serverUrl, type2Url,
                    FileResource.MANIFEST, FileResource.README, FileResource.LICENSE, FileResource.ICON, FileResource.DOWNLOAD, FileResource.CHANGELOG);
            queryVer.files = Lists.newArrayList();
            queryVer.addFile(FILE_MANIFEST, type2Url.get(FileResource.MANIFEST));
            queryVer.addFile(FILE_DETAILS, type2Url.get(FileResource.README));
            queryVer.addFile(FILE_LICENSE, type2Url.get(FileResource.LICENSE));
            queryVer.addFile(FILE_ICON, type2Url.get(FileResource.ICON));
            queryVer.addFile(FILE_VSIX, type2Url.get(FileResource.DOWNLOAD));
            queryVer.addFile(FILE_CHANGELOG, type2Url.get(FileResource.CHANGELOG));
        }

        if (test(flags, FLAG_INCLUDE_VERSION_PROPERTIES)) {
            queryVer.properties = Lists.newArrayList();
            queryVer.addProperty(PROP_BRANDING_COLOR, extVer.getGalleryColor());
            queryVer.addProperty(PROP_BRANDING_THEME, extVer.getGalleryTheme());
            queryVer.addProperty(PROP_REPOSITORY, extVer.getRepository());
            queryVer.addProperty(PROP_ENGINE, getVscodeEngine(extVer));
            var dependencies = extVer.getDependencies().stream()
                    .map(e -> e.getNamespace().getName() + "." + e.getName())
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_DEPENDENCY, dependencies);
            var bundledExtensions = extVer.getBundledExtensions().stream()
                    .map(e -> e.getNamespace().getName() + "." + e.getName())
                    .collect(Collectors.joining(","));
            queryVer.addProperty(PROP_EXTENSION_PACK, bundledExtensions);
            queryVer.addProperty(PROP_LOCALIZED_LANGUAGES, "");
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

    private boolean test(int flags, int flag) {
        return (flags & flag) != 0;
    }

}