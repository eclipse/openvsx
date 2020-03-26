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

import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionBinary;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.NamespaceResultJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.ReviewResultJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.util.CollectionUtil;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.UrlUtil;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LocalRegistryService implements IExtensionRegistry {
 
    Logger logger = LoggerFactory.getLogger(LocalRegistryService.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UserService users;

    @Autowired
    ExtensionValidator validator;

    @Autowired
    ElasticsearchOperations searchOperations;

    @Value("${ovsx.elasticsearch.enabled:true}")
    boolean enableSearch;

    @Value("${ovsx.elasticsearch.init-index:false}")
    boolean initSearchIndex;

    @Override
    public NamespaceJson getNamespace(String namespaceName) {
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new NotFoundException();
        }
        var json = new NamespaceJson();
        json.name = namespace.getName();
        json.extensions = new LinkedHashMap<>();
        var serverUrl = UrlUtil.getBaseUrl();
        for (var ext : repositories.findExtensions(namespace)) {
            String url = createApiUrl(serverUrl, "api", namespace.getName(), ext.getName());
            json.extensions.put(ext.getName(), url);
        }
        json.access = getAccessString(namespace);
        return json;
    }

    private String getAccessString(Namespace namespace) {
        var ownerships = repositories.findMemberships(namespace, NamespaceMembership.ROLE_OWNER);
        return ownerships.isEmpty() ? NamespaceJson.PUBLIC_ACCESS : NamespaceJson.RESTRICTED_ACCESS;
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            throw new NotFoundException();
        }
        ExtensionJson json = toJson(extension.getLatest(), true);
        return json;
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extensionName, String version) {
        var extVersion = repositories.findVersion(version, extensionName, namespace);
        if (extVersion == null) {
            throw new NotFoundException();
        }
        ExtensionJson json = toJson(extVersion, false);
        return json;
    }

    @Override
    @Transactional
    public byte[] getFile(String namespace, String extensionName, String fileName) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            throw new NotFoundException();
        }
        var extVersion = extension.getLatest();
        var resource = getFile(extVersion, fileName);
        if (resource == null) {
            throw new NotFoundException();
        }
        if (resource instanceof ExtensionBinary) {
            extension.setDownloadCount(extension.getDownloadCount() + 1);
        }
        return resource.getContent();
    }

    @Override
    @Transactional
    public byte[] getFile(String namespace, String extensionName, String version, String fileName) {
        var extVersion = repositories.findVersion(version, extensionName, namespace);
        if (extVersion == null)
            throw new NotFoundException();
        var resource = getFile(extVersion, fileName);
        if (resource == null)
            throw new NotFoundException();
        if (resource instanceof ExtensionBinary) {
            var extension = extVersion.getExtension();
            extension.setDownloadCount(extension.getDownloadCount() + 1);
        }
        return resource.getContent();
    }

    private FileResource getFile(ExtensionVersion extVersion, String fileName) {
        if (fileName.equals(extVersion.getExtensionFileName()))
            return repositories.findBinary(extVersion);
        if (fileName.equals(extVersion.getReadmeFileName()))
            return repositories.findReadme(extVersion);
        if (fileName.equals(extVersion.getLicenseFileName()))
            return repositories.findLicense(extVersion);
        if (fileName.equals(extVersion.getIconFileName()))
            return repositories.findIcon(extVersion);
        return null;
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null)
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

    @EventListener
    @Transactional
    public void initSearchIndex(ApplicationStartedEvent event) {
        if (enableSearch) {
            searchOperations.createIndex(ExtensionSearch.class);
            if (initSearchIndex) {
                logger.info("Initializing search index...");
                var allExtensions = repositories.findAllExtensions();
                if (!allExtensions.isEmpty()) {
                    var indexQueries = allExtensions.map(extension ->
                        new IndexQueryBuilder()
                            .withObject(extension.toSearch())
                            .build()
                    ).toList();
                    searchOperations.bulkIndex(indexQueries);
                }
            }
        }
    }

    public void updateSearchIndex(Extension extension) {
        var indexQuery = new IndexQueryBuilder()
                .withObject(extension.toSearch())
                .build();
        searchOperations.index(indexQuery);
    }

    @Override
    public SearchResultJson search(String queryString, String category, int size, int offset) {
        var json = new SearchResultJson();
        if (size <= 0 || !enableSearch) {
            json.extensions = Collections.emptyList();
            return json;
        }

        var pageRequest = PageRequest.of(offset / size, size);
        var searchResult = search(queryString, category, pageRequest);
        json.extensions = toSearchEntries(searchResult, size, offset % size);
        json.offset = offset;
        json.totalSize = (int) searchResult.getTotalElements();
        if (json.extensions.size() < size && searchResult.hasNext()) {
            // This is necessary when offset % size > 0
            var remainder = search(queryString, category, pageRequest.next());
            json.extensions.addAll(toSearchEntries(remainder, size - json.extensions.size(), 0));
        }
        return json;
    }

    private Page<ExtensionSearch> search(String queryString, String category, Pageable pageRequest) {
        var queryBuilder = new NativeSearchQueryBuilder()
                .withIndices("extensions")
                .withPageable(pageRequest);
        if (!Strings.isNullOrEmpty(queryString)) {
            var boolQuery = QueryBuilders.boolQuery();
            var multiMatchQuery = QueryBuilders.multiMatchQuery(queryString)
                    .field("name").boost(5)
                    .field("displayName").boost(5)
                    .field("tags").boost(3)
                    .field("namespace").boost(2)
                    .field("description")
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2);
            boolQuery.should(multiMatchQuery).boost(5);
            for (var term : queryString.split("\\s+")) {
                var namePrefixQuery = QueryBuilders.prefixQuery("displayName", term);
                boolQuery.should(namePrefixQuery).boost(2);
                var namespacePrefixQuery = QueryBuilders.prefixQuery("namespace", term);
                boolQuery.should(namespacePrefixQuery);
            }
            queryBuilder.withQuery(boolQuery);
        }
        if (!Strings.isNullOrEmpty(category)) {
            queryBuilder.withFilter(QueryBuilders.matchPhraseQuery("categories", category));
        }
        return searchOperations.queryForPage(queryBuilder.build(), ExtensionSearch.class);
    }

    private List<SearchEntryJson> toSearchEntries(Page<ExtensionSearch> page, int size, int offset) {
        if (offset > 0 || size < page.getNumberOfElements())
            return CollectionUtil.map(
                    Iterables.limit(Iterables.skip(page.getContent(), offset), size),
                    this::toSearchEntry);
        else
            return CollectionUtil.map(page.getContent(), this::toSearchEntry);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public NamespaceResultJson createNamespace(NamespaceJson json, String tokenValue) {
        var namespaceIssue = validator.validateNamespace(json.name);
        if (namespaceIssue.isPresent()) {
            throw new ErrorResultException(namespaceIssue.get().toString());
        }
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            throw new ErrorResultException("Invalid access token.");
        }
        var namespace = repositories.findNamespace(json.name);
        if (namespace != null) {
            throw new ErrorResultException("Namespace already exists: " + namespace.getName());
        }

        namespace = new Namespace();
        namespace.setName(json.name);
        entityManager.persist(namespace);
        return new NamespaceResultJson();
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ExtensionJson publish(InputStream content, String tokenValue) throws ErrorResultException {
        try (var processor = new ExtensionProcessor(content)) {
            var token = users.useAccessToken(tokenValue);
            if (token == null) {
                throw new ErrorResultException("Invalid access token.");
            }
            var extVersion = createExtensionVersion(processor, token.getUser(), token);
            var binary = processor.getBinary(extVersion);
            entityManager.persist(binary);
            var readme = processor.getReadme(extVersion);
            if (readme != null)
                entityManager.persist(readme);
            var license = processor.getLicense(extVersion);
            if (license != null)
                entityManager.persist(license);
            var icon = processor.getIcon(extVersion);
            if (icon != null)
                entityManager.persist(icon);
            processor.getExtensionDependencies().forEach(dep -> addDependency(dep, extVersion));
            processor.getBundledExtensions().forEach(dep -> addBundledExtension(dep, extVersion));

            updateSearchIndex(extVersion.getExtension());
            return toJson(extVersion, false);
        }
    }

    private ExtensionVersion createExtensionVersion(ExtensionProcessor processor, UserData user, PersonalAccessToken token) {
        var namespaceName = processor.getNamespace();
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new ErrorResultException("Unknown publisher: " + namespaceName
                    + "\nUse the 'create-namespace' command to create a namespace corresponding to your publisher name.");
        }
        if (!users.hasPublishPermission(user, namespace)) {
            throw new ErrorResultException("Insufficient access rights for publisher: " + namespace.getName());
        }

        var extensionName = processor.getExtensionName();
        var nameIssue = validator.validateExtensionName(extensionName);
        if (nameIssue.isPresent()) {
            throw new ErrorResultException(nameIssue.get().toString());
        }
        var extension = repositories.findExtension(extensionName, namespace);
        var extVersion = processor.getMetadata();
        if (extVersion.getDisplayName() != null && extVersion.getDisplayName().trim().isEmpty()) {
            extVersion.setDisplayName(null);
        }
        extVersion.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        extVersion.setPublishedWith(token);
        if (extension == null) {
            extension = new Extension();
            extension.setName(extensionName);
            extension.setNamespace(namespace);
            extension.setLatest(extVersion);
            entityManager.persist(extension);
        } else {
            if (repositories.findVersion(extVersion.getVersion(), extension) != null) {
                throw new ErrorResultException(
                        "Extension " + extension.getName()
                        + " version " + extVersion.getVersion()
                        + " is already published.");
            }
            if (isLatestVersion(extVersion.getVersion(), extension))
                extension.setLatest(extVersion);
        }
        extVersion.setExtension(extension);
        extVersion.setExtensionFileName(
                namespace.getName()
                + "." + extension.getName()
                + "-" + extVersion.getVersion()
                + ".vsix");
        var metadataIssues = validator.validateMetadata(extVersion);
        if (!metadataIssues.isEmpty()) {
            if (metadataIssues.size() == 1) {
                throw new ErrorResultException(metadataIssues.get(0).toString());
            }
            throw new ErrorResultException("Multiple issues were found in the extension metadata:\n"
                    + Joiner.on("\n").join(metadataIssues));
        }
        entityManager.persist(extVersion);
        return extVersion;
    }

    private boolean isLatestVersion(String version, Extension extension) {
        var allVersions = repositories.findVersions(extension);
        var newSemver = new SemanticVersion(version);
        for (var publishedVersion : allVersions) {
            var oldSemver = new SemanticVersion(publishedVersion.getVersion());
            if (newSemver.compareTo(oldSemver) < 0)
                return false;
        }
        return true;
    }

    private void addDependency(String dependency, ExtensionVersion extVersion) {
        var split = dependency.split("\\.");
        if (split.length != 2) {
            throw new ErrorResultException("Invalid 'extensionDependencies' format. Expected: '${namespace}.${name}'");
        }
        var namespace = repositories.findNamespace(split[0]);
        if (namespace == null) {
            throw new ErrorResultException("Cannot resolve dependency: " + dependency);
        }
        var extension = repositories.findExtension(split[1], namespace);
        if (extension == null) {
            throw new ErrorResultException("Cannot resolve dependency: " + dependency);
        }
        var depList = extVersion.getDependencies();
        if (depList == null) {
            depList = new ArrayList<Extension>();
            extVersion.setDependencies(depList);
        }
        depList.add(extension);
    }

    private void addBundledExtension(String bundled, ExtensionVersion extVersion) {
        var split = bundled.split("\\.");
        if (split.length != 2) {
            throw new ErrorResultException("Invalid 'extensionPack' format. Expected: '${namespace}.${name}'");
        }
        var namespace = repositories.findNamespace(split[0]);
        if (namespace == null) {
            throw new ErrorResultException("Cannot resolve bundled extension: " + bundled);
        }
        var extension = repositories.findExtension(split[1], namespace);
        if (extension == null) {
            throw new ErrorResultException("Cannot resolve bundled extension: " + bundled);
        }
        var depList = extVersion.getBundledExtensions();
        if (depList == null) {
            depList = new ArrayList<Extension>();
            extVersion.setBundledExtensions(depList);
        }
        depList.add(extension);
    }

    @Transactional(rollbackOn = ResponseStatusException.class)
    public ReviewResultJson postReview(ReviewJson review, String namespace, String extensionName) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            return ReviewResultJson.error("Extension not found: " + namespace + "." + extensionName);
        }
        var user = users.updateUser(principal);
        var activeReviews = repositories.findActiveReviews(extension, user);
        if (!activeReviews.isEmpty()) {
            return ReviewResultJson.error("You must not submit more than one review for an extension.");
        }

        var extReview = new ExtensionReview();
        extReview.setExtension(extension);
        extReview.setActive(true);
        extReview.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        extReview.setUser(user);
        extReview.setTitle(review.title);
        extReview.setComment(review.comment);
        extReview.setRating(review.rating);
        entityManager.persist(extReview);
        extension.setAverageRating(computeAverageRating(extension));
        return new ReviewResultJson();
    }

    @Transactional(rollbackOn = ResponseStatusException.class)
    public ReviewResultJson deleteReview(String namespace, String extensionName) {
        var principal = users.getOAuth2Principal();
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null) {
            return ReviewResultJson.error("Extension not found: " + namespace + "." + extensionName);
        }
        var user = users.updateUser(principal);
        var activeReviews = repositories.findActiveReviews(extension, user);
        if (activeReviews.isEmpty()) {
            return ReviewResultJson.error("You have not submitted any review yet.");
        }

        for (var extReview : activeReviews) {
            extReview.setActive(false);
        }
        extension.setAverageRating(computeAverageRating(extension));
        return new ReviewResultJson();
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

    private SearchEntryJson toSearchEntry(ExtensionSearch search) {
        var extension = entityManager.find(Extension.class, search.id);
        if (extension == null)
            return null;
        var extVer = extension.getLatest();
        var entry = extVer.toSearchEntryJson();
        var serverUrl = UrlUtil.getBaseUrl();
        entry.url = createApiUrl(serverUrl, "api", entry.namespace, entry.name);
        entry.files = new LinkedHashMap<>();
        entry.files.put("download", createApiUrl(serverUrl, "api", entry.namespace, entry.name, "file", extVer.getExtensionFileName()));
        entry.files.put("icon", createApiUrl(serverUrl, "api", entry.namespace, entry.name, "file", extVer.getIconFileName()));
        return entry;
    }

    private ExtensionJson toJson(ExtensionVersion extVersion, boolean isLatest) {
        var extension = extVersion.getExtension();
        var json = extVersion.toExtensionJson();
        json.namespaceAccess = getAccessString(extension.getNamespace());
        json.reviewCount = repositories.countActiveReviews(extension);
        var serverUrl = UrlUtil.getBaseUrl();
        json.namespaceUrl = createApiUrl(serverUrl, "api", json.namespace);
        json.reviewsUrl = createApiUrl(serverUrl, "api", json.namespace, json.name, "reviews");
        var allVersions = CollectionUtil.map(repositories.findVersions(extension),
                extVer -> new SemanticVersion(extVer.getVersion()));
        Collections.sort(allVersions, Comparator.reverseOrder());
        json.allVersions = new LinkedHashMap<>();
        for (var semVer : allVersions) {
            String url = createApiUrl(serverUrl, "api", json.namespace, json.name, semVer.toString());
            json.allVersions.put(semVer.toString(), url);
        }
        json.files = new LinkedHashMap<>();
        if (isLatest) {
            json.files.put("download", createApiUrl(serverUrl, "api", json.namespace, json.name, "file", extVersion.getExtensionFileName()));
            json.files.put("icon", createApiUrl(serverUrl, "api", json.namespace, json.name, "file", extVersion.getIconFileName()));
            json.files.put("readme", createApiUrl(serverUrl, "api", json.namespace, json.name, "file", extVersion.getReadmeFileName()));
            json.files.put("license", createApiUrl(serverUrl, "api", json.namespace, json.name, "file", extVersion.getLicenseFileName()));
        } else {
            json.files.put("download", createApiUrl(serverUrl, "api", json.namespace, json.name, json.version, "file", extVersion.getExtensionFileName()));
            json.files.put("icon", createApiUrl(serverUrl, "api", json.namespace, json.name, json.version, "file", extVersion.getIconFileName()));
            json.files.put("readme", createApiUrl(serverUrl, "api", json.namespace, json.name, json.version, "file", extVersion.getReadmeFileName()));
            json.files.put("license", createApiUrl(serverUrl, "api", json.namespace, json.name, json.version, "file", extVersion.getLicenseFileName()));
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

}