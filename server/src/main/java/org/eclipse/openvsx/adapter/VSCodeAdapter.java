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

import java.net.URLConnection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.CollectionUtil;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
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
    SearchService search;

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
        if (param.filters == null || param.filters.isEmpty()) {
            pageRequest = PageRequest.of(0, 20);
        } else {
            var filter = param.filters.get(0);
            var extensionId = filter.findCriterion(FILTER_EXTENSION_ID);
            if (!Strings.isNullOrEmpty(extensionId)) {
                try {
                    // Find a single extension by identifier
                    return findExtension(Long.parseLong(extensionId), param.flags);
                } catch (NumberFormatException exc) {
                    // Ignore the filter and proceed with search
                }
            }
            queryString = filter.findCriterion(FILTER_SEARCH_TEXT);
            if (queryString == null)
                queryString = filter.findCriterion(FILTER_TAG);
            category = filter.findCriterion(FILTER_CATEGORY);
            pageRequest = PageRequest.of(filter.pageNumber - 1, filter.pageSize);
        }

        var searchResult = search.search(queryString, category, pageRequest);
        return findExtensions(searchResult, param.flags);
    }

    private ExtensionQueryResult findExtension(long id, int flags) {
        var extension = entityManager.find(Extension.class, id);
        var resultItem = new ExtensionQueryResult.ResultItem();
        if (extension == null)
            resultItem.extensions = Collections.emptyList();
        else
            resultItem.extensions = Lists.newArrayList(toQueryExtension(extension, flags));

        var countMetadataItem = new ExtensionQueryResult.ResultMetadataItem();
        countMetadataItem.name = "TotalCount";
        countMetadataItem.count = extension == null ? 0 : 1;
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

    @GetMapping("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType:.+}")
    @CrossOrigin
    @Transactional
    public ResponseEntity<byte[]> getFile(@PathVariable String namespace,
                                          @PathVariable String extensionName,
                                          @PathVariable String version,
                                          @PathVariable String assetType) {
        var extVersion = repositories.findVersion(version, extensionName, namespace);
        if (extVersion == null)
            throw new NotFoundException();
        var fileNameAndResource = getFile(extVersion, assetType);
        if (fileNameAndResource == null || fileNameAndResource.getSecond() == null)
            throw new NotFoundException();
        if (fileNameAndResource.getSecond().getType().equals(FileResource.DOWNLOAD)) {
            var extension = extVersion.getExtension();
            extension.setDownloadCount(extension.getDownloadCount() + 1);
            search.updateSearchEntry(extension);
        }
        var content = fileNameAndResource.getSecond().getContent();
        var headers = getFileResponseHeaders(fileNameAndResource.getFirst());
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }
    
    private Pair<String, FileResource> getFile(ExtensionVersion extVersion, String assetType) {
        switch (assetType) {
            case FILE_VSIX:
                return Pair.of(
                    extVersion.getExtensionFileName(),
                    repositories.findFile(extVersion, FileResource.DOWNLOAD)
                );
            case FILE_MANIFEST:
                return Pair.of(
                    "package.json",
                    repositories.findFile(extVersion, FileResource.MANIFEST)
                );
            case FILE_DETAILS:
                return Pair.of(
                    extVersion.getReadmeFileName(),
                    repositories.findFile(extVersion, FileResource.README)
                );
            case FILE_LICENSE:
                return Pair.of(
                    extVersion.getLicenseFileName(),
                    repositories.findFile(extVersion, FileResource.LICENSE)
                );
            case FILE_ICON:
                return Pair.of(
                    extVersion.getIconFileName(),
                    repositories.findFile(extVersion, FileResource.ICON)
                );
            default:
               return null;
        }
    }

    private HttpHeaders getFileResponseHeaders(String fileName) {
        var headers = new HttpHeaders();
        MediaType fileType = getFileType(fileName);
        headers.setContentType(fileType);
        // Files are requested with a version string in the URL, so their content cannot change
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
        if (fileName.endsWith(".vsix")) {
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        return headers;
    }

    private MediaType getFileType(String fileName) {
        if (fileName.endsWith(".vsix")) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        var contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return MediaType.parseMediaType(contentType);
        }
        return MediaType.TEXT_PLAIN;
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

    private ExtensionQueryResult.Extension toQueryExtension(Extension extension, int flags) {
        var queryExt = new ExtensionQueryResult.Extension();
        var namespace = extension.getNamespace();
        queryExt.publisher = new ExtensionQueryResult.Publisher();
        queryExt.publisher.publisherId = Long.toString(namespace.getId());
        queryExt.publisher.publisherName = namespace.getName();
        queryExt.extensionId = Long.toString(extension.getId());
        queryExt.extensionName = extension.getName();
        var latest = extension.getLatest();
        queryExt.displayName = latest.getDisplayName();
        queryExt.flags = latest.isPreview() ? FLAG_PREVIEW : "";
        queryExt.shortDescription = latest.getDescription();

        if (test(flags, FLAG_INCLUDE_LATEST_VERSION_ONLY)) {
            queryExt.versions = Lists.newArrayList(toQueryVersion(latest, flags));
        } else if (test(flags, FLAG_INCLUDE_VERSIONS)) {
            queryExt.versions = CollectionUtil.map(extension.getVersions(), ev -> toQueryVersion(ev, flags));
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
            queryVer.files = Lists.newArrayList();
            queryVer.addFile(FILE_MANIFEST,
                    UrlUtil.createApiUrl(serverUrl, "api", namespace, extensionName, extVer.getVersion(), "file", "package.json"));
            queryVer.addFile(FILE_DETAILS,
                    UrlUtil.createApiUrl(serverUrl, "api", namespace, extensionName, extVer.getVersion(), "file", extVer.getReadmeFileName()));
            queryVer.addFile(FILE_LICENSE,
                    UrlUtil.createApiUrl(serverUrl, "api", namespace, extensionName, extVer.getVersion(), "file", extVer.getLicenseFileName()));
            queryVer.addFile(FILE_ICON,
                    UrlUtil.createApiUrl(serverUrl, "api", namespace, extensionName, extVer.getVersion(), "file", extVer.getIconFileName()));
            queryVer.addFile(FILE_VSIX,
                    UrlUtil.createApiUrl(serverUrl, "api", namespace, extensionName, extVer.getVersion(), "file", extVer.getExtensionFileName()));
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