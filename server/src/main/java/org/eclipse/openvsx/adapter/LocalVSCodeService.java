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

import com.google.common.collect.Lists;
import io.micrometer.observation.annotation.Observed;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.search.SortBy;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.openvsx.adapter.ExtensionQueryParam.Criterion.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Extension.FLAG_PREVIEW;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.ExtensionFile.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Property.*;
import static org.eclipse.openvsx.adapter.ExtensionQueryResult.Statistic.*;
import static org.eclipse.openvsx.entities.FileResource.*;

@Component
public class LocalVSCodeService implements IVSCodeService {
    protected final Logger logger = LoggerFactory.getLogger(LocalVSCodeService.class);

    private final RepositoryService repositories;
    private final VersionService versions;
    private final SearchUtilService search;
    private final StorageUtilService storageUtil;
    private final ExtensionVersionIntegrityService integrityService;
    private final WebResourceService webResources;
    private final CacheService cache;

    @Value("${ovsx.webui.url:}")
    String webuiUrl;

    public LocalVSCodeService(
            RepositoryService repositories,
            VersionService versions,
            SearchUtilService search,
            StorageUtilService storageUtil,
            ExtensionVersionIntegrityService integrityService,
            WebResourceService webResources,
            CacheService cache
    ) {
        this.repositories = repositories;
        this.versions = versions;
        this.search = search;
        this.storageUtil = storageUtil;
        this.integrityService = integrityService;
        this.webResources = webResources;
        this.cache = cache;
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
        if (param.filters() == null || param.filters().isEmpty()) {
            pageNumber = 0;
            pageSize = defaultPageSize;
            sortBy = SortBy.RELEVANCE;
            sortOrder = "desc";
            targetPlatform = null;
            extensionIds = Collections.emptySet();
            extensionNames = Collections.emptySet();
        } else {
            var filter = param.filters().get(0);
            extensionIds = new LinkedHashSet<>(filter.findCriteria(FILTER_EXTENSION_ID));
            extensionNames = new LinkedHashSet<>(filter.findCriteria(FILTER_EXTENSION_NAME));

            queryString = filter.findCriterion(FILTER_SEARCH_TEXT);
            if (queryString == null)
                queryString = filter.findCriterion(FILTER_TAG);

            category = filter.findCriterion(FILTER_CATEGORY);
            var targetCriterion = filter.findCriterion(FILTER_TARGET);
            targetPlatform = TargetPlatform.isValid(targetCriterion) ? targetCriterion : null;

            pageNumber = Math.max(0, filter.pageNumber() - 1);
            pageSize = filter.pageSize() > 0 ? filter.pageSize() : defaultPageSize;
            sortOrder = getSortOrder(filter.sortOrder());
            sortBy = getSortBy(filter.sortBy());
        }

        Long totalCount = null;
        List<ExtensionSearch> searchHits = Collections.emptyList();
        var usePublicIdAsKey = false;
        var data = new HashMap<String, ExtensionQueryExtensionData>();
        List<Extension> extensionsList;
        if (!extensionIds.isEmpty()) {
            usePublicIdAsKey = true;
            var cachedData = cache.getExtensionQueryExtensionDataByPublicId(extensionIds);
            data.putAll(cachedData);

            var remainingExtensionIds = extensionIds.stream().filter(id -> !cachedData.containsKey(id)).toList();
            extensionsList = repositories.findActiveExtensionsByPublicId(remainingExtensionIds, BuiltInExtensionUtil.getBuiltInNamespace());
        } else if (!extensionNames.isEmpty()) {
            var cachedData = cache.getExtensionQueryExtensionDataByExtensionId(extensionNames);
            data.putAll(cachedData);

            extensionsList = extensionNames.stream()
                    .filter(name -> !cachedData.containsKey(name))
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
                        pageOffset, sortOrder, sortBy, false, new String[]{BuiltInExtensionUtil.getBuiltInNamespace()});

                var searchResult = search.search(searchOptions);
                totalCount = searchResult.getTotalHits();
                searchHits = searchResult.getHits();
            } catch (
                    ErrorResultException exc) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exc.getMessage(), exc);
            }

            var searchIds = searchHits.stream().map(ExtensionSearch::getExtensionId).toList();
            var cachedData = cache.getExtensionQueryExtensionDataByExtensionId(searchIds);
            data.putAll(cachedData);

            var ids = searchHits.stream()
                    .filter(hit -> !cachedData.containsKey(hit.getExtensionId()))
                    .map(ExtensionSearch::getId)
                    .collect(Collectors.toList());

            extensionsList = repositories.findActiveExtensionsById(ids);
        }
        if(totalCount == null) {
            totalCount = (long) (data.size() + extensionsList.size());
        }

        // when mapping the list of extensions to a map, we need to handle duplicate entries which can happen,
        // see https://github.com/eclipse/openvsx/issues/1394
        var extensionsMap = extensionsList.stream().collect(Collectors.toMap(Extension::getId, Function.identity(), (a, b) -> a));
        List<ExtensionVersion> allActiveExtensionVersions = repositories.findActiveExtensionVersions(extensionsMap.keySet(), targetPlatform);

        var comparator = Comparator.<ExtensionVersion, Long>comparing(ev -> ev.getExtension().getId())
                .thenComparing(ExtensionVersion.SORT_COMPARATOR);

        var extensionVersionsMap = allActiveExtensionVersions.stream()
                .map(ev -> {
                    ev.setExtension(extensionsMap.get(ev.getExtension().getId()));
                    return ev;
                })
                .sorted(comparator)
                .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()));

        var latestExtensionVersionsMap = extensionVersionsMap.entrySet().stream()
                .map(entry -> {
                    var ids = entry.getValue().stream()
                            .collect(Collectors.groupingBy(ExtensionVersion::getTargetPlatform))
                            .values()
                            .stream()
                            .map(list -> versions.getLatest(list, true))
                            .map(ExtensionVersion::getId)
                            .toList();

                    return Map.entry(entry.getKey(), ids);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var fileResources = includeFiles(allActiveExtensionVersions);

        var latestVersions = allActiveExtensionVersions.stream()
                .collect(Collectors.groupingBy(ev -> ev.getExtension().getId()))
                .values()
                .stream()
                .map(list -> versions.getLatest(list, false))
                .collect(Collectors.toMap(ev -> ev.getExtension().getId(), ev -> ev));

        Function<ExtensionQueryExtensionData, String> keyMapper = usePublicIdAsKey ? ExtensionQueryExtensionData::publicId : ExtensionQueryExtensionData::extensionId;
        for(var extension : extensionsList) {
            var latest = latestVersions.get(extension.getId());
            var queryVersions = extensionVersionsMap.getOrDefault(extension.getId(), Collections.emptyList()).stream()
                    .map(extVer -> toQueryVersion(extVer, fileResources))
                    .collect(Collectors.toList());

            var latestQueryVersions = latestExtensionVersionsMap.get(extension.getId());
            var queryExt = toQueryExtension(extension, latest, queryVersions, latestQueryVersions);
            cache.putExtensionQueryExtensionData(queryExt);
            data.put(keyMapper.apply(queryExt), queryExt);
        }

        Stream<String> resultStream;
        if (!extensionIds.isEmpty()) {
            resultStream = extensionIds.stream();
        } else if (!extensionNames.isEmpty()) {
            resultStream = extensionNames.stream();
        } else if (!searchHits.isEmpty()) {
            resultStream = searchHits.stream().map(ExtensionSearch::getExtensionId);
        } else {
            resultStream = Stream.empty();
        }

        var extensionQueryResults = resultStream
                .map(data::get)
                .filter(Objects::nonNull)
                .map(d -> d.toJson(param.flags()))
                .toList();

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
        var countMetadataItem = new ExtensionQueryResult.ResultMetadataItem("TotalCount", totalCount);
        var countMetadata = new ExtensionQueryResult.ResultMetadata("ResultCount", List.of(countMetadataItem));
        var resultItem = new ExtensionQueryResult.ResultItem(extensions, List.of(countMetadata));
        return new ExtensionQueryResult(List.of(resultItem));
    }

    private Map<Long, List<FileResource>> includeFiles(List<ExtensionVersion> extVersions) {
        var types = new ArrayList<>(List.of(MANIFEST, README, LICENSE, ICON, DOWNLOAD, CHANGELOG, VSIXMANIFEST));
        if(integrityService.isEnabled()) {
            types.add(DOWNLOAD_SIG);
        }

        var idsMap = extVersions.stream().collect(Collectors.toMap(ExtensionVersion::getId, ev -> ev));
        return repositories.findFileResourcesByExtensionVersionIdAndType(idsMap.keySet(), types).stream()
                .peek(r -> r.setExtension(idsMap.get(r.getExtension().getId())))
                .collect(Collectors.groupingBy(fr -> fr.getExtension().getId()));
    }

    private String getSortBy(int sortBy) {
        return switch (sortBy) {
            case 4 -> // InstallCount
                    SortBy.DOWNLOADS;
            case 5 -> // PublishedDate
                    SortBy.TIMESTAMP;
            case 6 -> // AverageRating
                    SortBy.RATING;
            default -> SortBy.RELEVANCE;
        };
    }

    private String getSortOrder(int sortOrder) {
        return sortOrder == 1 ? "asc" : "desc";
    }

    @Observed
    @Override
    public ResponseEntity<StreamingResponseBody> getAsset(
            String namespace, String extensionName, String version, String assetType, String targetPlatform,
            String restOfTheUrl
    ) {
        if(BuiltInExtensionUtil.isBuiltIn(namespace)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(builtinExtensionResponse());
        }

        var asset = (restOfTheUrl != null && !restOfTheUrl.isEmpty()) ? (assetType + "/" + restOfTheUrl) : assetType;
        if((asset.equals(FILE_PUBLIC_KEY) || asset.equals(FILE_SIGNATURE)) && !integrityService.isEnabled()) {
            throw new NotFoundException();
        }

        if(asset.equals(FILE_PUBLIC_KEY)) {
            var publicId = repositories.findSignatureKeyPairPublicId(namespace, extensionName, targetPlatform, version);
            if(publicId == null) {
                throw new NotFoundException();
            } else {
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

        var type = assets.get(assetType);
        if(type != null) {
            var resource = repositories.findFileByType(namespace, extensionName, targetPlatform, version, type);
            if (resource == null) {
                throw new NotFoundException();
            }
            if (resource.getType().equals(FileResource.DOWNLOAD)) {
                storageUtil.increaseDownloadCount(resource);
            }

            return storageUtil.getFileResponse(resource);
        } else if(asset.startsWith(FILE_WEB_RESOURCES + "/extension/")) {
            var name = asset.substring((FILE_WEB_RESOURCES.length() + 1));
            var extensionDownloadPath = webResources.getExtensionDownload(namespace, extensionName, targetPlatform, version);
            var file = extensionDownloadPath != null ? getWebResource(namespace, extensionName, targetPlatform, version, name, extensionDownloadPath) : null;
            if(file != null) {
                return storageUtil.getFileResponse(file);
            }
        }

        throw new NotFoundException();
    }

    private Path getWebResource(String namespaceName, String extensionName, String targetPlatform, String version, String name, Path extensionDownloadPath) {
        var file = webResources.getWebResource(namespaceName, extensionName, targetPlatform, version, name, extensionDownloadPath);
        if(file != null && !Files.exists(file)) {
            logger.error("File doesn't exist {}", file);
            cache.evictWebResourceFile(namespaceName, extensionName, targetPlatform, version, name);
            file = null;
        }
        return file;
    }

    private String builtinExtensionMessage() {
        return "Built-in extension namespace '" + BuiltInExtensionUtil.getBuiltInNamespace() + "' not allowed";
    }

    private StreamingResponseBody builtinExtensionResponse() {
        return out -> out.write(builtinExtensionMessage().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getItemUrl(String namespaceName, String extensionName) {
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, builtinExtensionMessage());
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, builtinExtensionMessage());
        }

        var resource = repositories.findFileByType(namespaceName, extensionName, targetPlatform, version, FileResource.DOWNLOAD);
        if (resource == null) {
            throw new NotFoundException();
        }

        if(resource.getStorageType().equals(STORAGE_LOCAL)) {
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

    @Observed
    @Override
    public ResponseEntity<StreamingResponseBody> browse(String namespaceName, String extensionName, String version, String path) {
        if(BuiltInExtensionUtil.isBuiltIn(namespaceName)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(builtinExtensionResponse());
        }

        var extensionDownloadPath = webResources.getExtensionDownload(namespaceName, extensionName, null, version);
        if(extensionDownloadPath == null) {
            throw new NotFoundException();
        }

        var file = getWebResource(namespaceName, extensionName, null, version, path, extensionDownloadPath);
        if(file != null) {
            return storageUtil.getFileResponse(file);
        }

        var node = webResources.browseExtensionPackage(namespaceName, extensionName, null, version, path, extensionDownloadPath);
        if(node != null) {
            return storageUtil.getFileResponse(node);
        }

        throw new NotFoundException();
    }

    private ExtensionQueryExtensionData toQueryExtension(Extension extension, ExtensionVersion latest, List<ExtensionQueryExtensionData.ExtensionVersion> versions, List<Long> latestVersions) {
        var statistics = getQueryExtensionStatistics(extension);
        var namespace = extension.getNamespace();
        var publisher = new ExtensionQueryResult.Publisher(
                !StringUtils.isEmpty(namespace.getDisplayName()) ? namespace.getDisplayName() : namespace.getName(),
                namespace.getPublicId(),
                namespace.getName(),
                null,
                null
        );

        return new ExtensionQueryExtensionData(
                extension.getPublicId(),
                NamingUtil.toExtensionId(extension),
                extension.getName(),
                latest.getDisplayName(),
                latest.getDescription(),
                publisher,
                versions,
                latestVersions,
                statistics,
                latest.getTags(),
                TimeUtil.toUTCString(extension.getPublishedDate()),
                TimeUtil.toUTCString(extension.getPublishedDate()),
                TimeUtil.toUTCString(extension.getLastUpdatedDate()),
                latest.getCategories(),
                latest.isPreview() ? FLAG_PREVIEW : ""
        );
    }

    private List<ExtensionQueryResult.Statistic> getQueryExtensionStatistics(Extension extension) {
        var statistics = new ArrayList<ExtensionQueryResult.Statistic>();
        var installStat = new ExtensionQueryResult.Statistic(STAT_INSTALL, extension.getDownloadCount());
        statistics.add(installStat);
        if (extension.getAverageRating() != null) {
            var avgRatingStat = new ExtensionQueryResult.Statistic(STAT_AVERAGE_RATING, extension.getAverageRating());
            statistics.add(avgRatingStat);
        }
        var ratingCountStat = new ExtensionQueryResult.Statistic(
                STAT_RATING_COUNT,
                Optional.ofNullable(extension.getReviewCount()).orElse(0L)
        );
        statistics.add(ratingCountStat);

        return statistics;
    }

    private ExtensionQueryExtensionData.ExtensionVersion toQueryVersion(
            ExtensionVersion extVer,
            Map<Long, List<FileResource>> fileResources
    ) {
        var serverUrl = UrlUtil.getBaseUrl();
        var namespaceName = extVer.getExtension().getNamespace().getName();
        var extensionName = extVer.getExtension().getName();
        var assetUri =  UrlUtil.createApiUrl(serverUrl, "vscode", "asset", namespaceName, extensionName, extVer.getVersion());

        var properties = new ArrayList<ExtensionQueryResult.Property>();
        addQueryExtensionVersionProperty(properties, PROP_BRANDING_COLOR, extVer.getGalleryColor());
        addQueryExtensionVersionProperty(properties, PROP_BRANDING_THEME, extVer.getGalleryTheme());
        addQueryExtensionVersionProperty(properties, PROP_REPOSITORY, extVer.getRepository());
        addQueryExtensionVersionProperty(properties, PROP_SPONSOR_LINK, extVer.getSponsorLink());
        addQueryExtensionVersionProperty(properties, PROP_ENGINE, getVscodeEngine(extVer));
        var dependencies = String.join(",", extVer.getDependencies());
        addQueryExtensionVersionProperty(properties, PROP_DEPENDENCY, dependencies);
        var bundledExtensions = String.join(",", extVer.getBundledExtensions());
        addQueryExtensionVersionProperty(properties, PROP_EXTENSION_PACK, bundledExtensions);
        var localizedLanguages = String.join(",", extVer.getLocalizedLanguages());
        addQueryExtensionVersionProperty(properties, PROP_LOCALIZED_LANGUAGES, localizedLanguages);
        if (extVer.isPreRelease()) {
            addQueryExtensionVersionProperty(properties, PROP_PRE_RELEASE, "true");
        }
        if (isWebExtension(extVer)) {
            addQueryExtensionVersionProperty(properties, PROP_WEB_EXTENSION, "true");
        }

        List<ExtensionQueryResult.ExtensionFile> files = null;
        var resourcesByType = fileResources.get(extVer.getId()).stream()
                .collect(Collectors.groupingBy(FileResource::getType));

        var fileBaseUrl = UrlUtil.createApiFileBaseUrl(serverUrl, namespaceName, extensionName, extVer.getTargetPlatform(), extVer.getVersion());

        files = Lists.newArrayList();
        addQueryExtensionVersionFile(files, FILE_MANIFEST, createFileUrl(resourcesByType.get(MANIFEST), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_DETAILS, createFileUrl(resourcesByType.get(README), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_LICENSE, createFileUrl(resourcesByType.get(LICENSE), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_ICON, createFileUrl(resourcesByType.get(ICON), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_VSIX, createFileUrl(resourcesByType.get(DOWNLOAD), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_CHANGELOG, createFileUrl(resourcesByType.get(CHANGELOG), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_VSIXMANIFEST, createFileUrl(resourcesByType.get(VSIXMANIFEST), fileBaseUrl));
        addQueryExtensionVersionFile(files, FILE_SIGNATURE, createFileUrl(resourcesByType.get(DOWNLOAD_SIG), fileBaseUrl));
        if(resourcesByType.containsKey(DOWNLOAD_SIG)) {
            addQueryExtensionVersionFile(files, FILE_PUBLIC_KEY, UrlUtil.getPublicKeyUrl(extVer));
        }

        return new ExtensionQueryExtensionData.ExtensionVersion(
                extVer.getId(),
                new ExtensionQueryResult.ExtensionVersion(
                        extVer.getVersion(),
                        TimeUtil.toUTCString(extVer.getTimestamp()),
                        assetUri,
                        assetUri,
                        files,
                        properties,
                        extVer.getTargetPlatform()
                )
        );
    }

    public void addQueryExtensionVersionFile(List<ExtensionQueryResult.ExtensionFile> files, String assetType, String source) {
        if (source != null) {
            files.add(new ExtensionQueryResult.ExtensionFile(assetType, source));
        }
    }

    public void addQueryExtensionVersionProperty(List<ExtensionQueryResult.Property> properties, String key, String value) {
        if (value != null) {
            properties.add(new ExtensionQueryResult.Property(key, value));
        }
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
}
