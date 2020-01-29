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

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

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
import org.springframework.stereotype.Component;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Publisher;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.PublisherJson;
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

@Component
public class LocalRegistryService implements IExtensionRegistry {
 
    Logger logger = LoggerFactory.getLogger(LocalRegistryService.class);

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    ElasticsearchOperations searchOperations;

    @Value("#{environment.OVSX_SERVER_URL}")
    String serverUrl;

    @Override
    public PublisherJson getPublisher(String publisherName) {
        var publisher = repositories.findPublisher(publisherName);
        if (publisher == null)
            throw new NotFoundException();
        var json = new PublisherJson();
        json.name = publisher.getName();
        json.extensions = new LinkedHashMap<>();
        for (var ext : repositories.findExtensions(publisher)) {
            String url = createApiUrl(serverUrl, publisher.getName(), ext.getName());
            json.extensions.put(ext.getName(), url);
        }
        return json;
    }

    @Override
    public ExtensionJson getExtension(String publisherName, String extensionName) {
        var extension = repositories.findExtension(extensionName, publisherName);
        if (extension == null)
            throw new NotFoundException();
        ExtensionJson json = toJson(extension.getLatest(), true);
        return json;
    }

    @Override
    public ExtensionJson getExtension(String publisherName, String extensionName, String version) {
        var extVersion = repositories.findVersion(version, extensionName, publisherName);
        if (extVersion == null)
            throw new NotFoundException();
        ExtensionJson json = toJson(extVersion, false);
        return json;
    }

    @Override
    public byte[] getFile(String publisherName, String extensionName, String fileName) {
        var extension = repositories.findExtension(extensionName, publisherName);
        if (extension == null)
            throw new NotFoundException();
        var extVersion = extension.getLatest();
        var resource = getFile(extVersion, fileName);
        if (resource == null)
            throw new NotFoundException();
        return resource.getContent();
    }

    @Override
    public byte[] getFile(String publisherName, String extensionName, String version, String fileName) {
        var extVersion = repositories.findVersion(version, extensionName, publisherName);
        if (extVersion == null)
            throw new NotFoundException();
        var resource = getFile(extVersion, fileName);
        if (resource == null)
            throw new NotFoundException();
        return resource.getContent();
    }

    private FileResource getFile(ExtensionVersion extVersion, String fileName) {
        if (fileName.equals(extVersion.getExtensionFileName()))
            return repositories.findBinary(extVersion);
        if (fileName.equals(extVersion.getReadmeFileName()))
            return repositories.findReadme(extVersion);
        if (fileName.equals(extVersion.getIconFileName()))
            return repositories.findIcon(extVersion);
        return null;
    }

    @Override
    public ReviewListJson getReviews(String publisherName, String extensionName) {
        var extension = repositories.findExtension(extensionName, publisherName);
        if (extension == null)
            throw new NotFoundException();
        var list = new ReviewListJson();
        list.postUrl = createApiUrl(serverUrl, extension.getPublisher().getName(), extension.getName(), "review");
        list.reviews = repositories.findReviews(extension)
                .map(extReview -> extReview.toReviewJson())
                .toList();
        return list;
    }

    @EventListener
    @Transactional
    public void initSearchIndex(ApplicationStartedEvent event) {
        searchOperations.createIndex(ExtensionSearch.class);
        if (event.getApplicationContext().getEnvironment().getProperty("OVSX_INIT_SEARCH_INDEX") != null) {
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

    public void updateSearchIndex(Extension extension) {
        var indexQuery = new IndexQueryBuilder()
                .withObject(extension.toSearch())
                .build();
        searchOperations.index(indexQuery);
    }

    @Override
    public SearchResultJson search(String queryString, String category, int size, int offset) {
        var json = new SearchResultJson();
        if (size <= 0) {
            json.extensions = Collections.emptyList();
            return json;
        }

        var pageRequest = PageRequest.of(offset / size, size);
        var searchResult = search(queryString, category, pageRequest);
        json.extensions = toSearchEntries(searchResult, size, offset % size);
        json.offset = offset;
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
            var multiMatchQuery = QueryBuilders.multiMatchQuery(queryString)
                    .field("name").boost(5)
                    .field("displayName").boost(5)
                    .field("tags").boost(3)
                    .field("publisher").boost(2)
                    .field("description")
                    .fuzziness(Fuzziness.AUTO)
                    .prefixLength(2);
            queryBuilder.withQuery(multiMatchQuery);
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

    @Transactional
    public ExtensionJson publish(InputStream content) {
        try (var processor = new ExtensionProcessor(content)) {
            var publisher = repositories.findPublisher(processor.getPublisherName());
            if (publisher == null) {
                publisher = new Publisher();
                publisher.setName(processor.getPublisherName());
                entityManager.persist(publisher);
            }
            var extension = repositories.findExtension(processor.getExtensionName(), publisher);
            var extVersion = processor.getMetadata();
            extVersion.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
            if (extension == null) {
                extension = new Extension();
                extension.setName(processor.getExtensionName());
                extension.setPublisher(publisher);
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
                    publisher.getName()
                    + "." + extension.getName()
                    + "-" + extVersion.getVersion()
                    + ".vsix");

            entityManager.persist(extVersion);
            var binary = processor.getBinary(extVersion);
            entityManager.persist(binary);
            var readme = processor.getReadme(extVersion);
            if (readme != null)
                entityManager.persist(readme);
            var icon = processor.getIcon(extVersion);
            if (icon != null)
                entityManager.persist(icon);
            processor.getExtensionDependencies().forEach(dep -> addDependency(dep, extVersion));
            processor.getBundledExtensions().forEach(dep -> addBundledExtension(dep, extVersion));

            updateSearchIndex(extension);
            return toJson(extVersion, false);
        } catch (ErrorResultException exc) {
            return ExtensionJson.error(exc.getMessage());
        }
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
        if (split.length != 2)
            return;
        var publisher = repositories.findPublisher(split[0]);
        if (publisher == null)
            return;
        var extension = repositories.findExtension(split[1], publisher);
        if (extension == null)
            return;
        var depList = extVersion.getDependencies();
        if (depList == null) {
            depList = new ArrayList<Extension>();
            extVersion.setDependencies(depList);
        }
        depList.add(extension);
    }

    private void addBundledExtension(String bundled, ExtensionVersion extVersion) {
        var split = bundled.split("\\.");
        if (split.length != 2)
            return;
        var publisher = repositories.findPublisher(split[0]);
        if (publisher == null)
            return;
        var extension = repositories.findExtension(split[1], publisher);
        if (extension == null)
            return;
        var depList = extVersion.getBundledExtensions();
        if (depList == null) {
            depList = new ArrayList<Extension>();
            extVersion.setBundledExtensions(depList);
        }
        depList.add(extension);
    }

    @Transactional
    public ReviewResultJson review(ReviewJson review, String publisherName, String extensionName, String sessionId) {
        var session = repositories.findUserSession(sessionId);
        if (session == null)
            return ReviewResultJson.error("Invalid session.");
        var extension = repositories.findExtension(extensionName, publisherName);
        if (extension == null)
            throw new NotFoundException();
        var extReview = new ExtensionReview();
        extReview.setExtension(extension);
        extReview.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        extReview.setUsername(session.getUser().getName());
        extReview.setTitle(review.title);
        extReview.setComment(review.comment);
        extReview.setRating(review.rating);
        entityManager.persist(extReview);
        extension.setAverageRating(computeAverageRating(extension));
        return new ReviewResultJson();
    }

    private double computeAverageRating(Extension extension) {
        var reviews = repositories.findReviews(extension);
        long sum = 0;
        long count = 0;
        for (var review : reviews) {
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
        entry.url = createApiUrl(serverUrl, entry.publisher, entry.name);
        entry.iconUrl = createApiUrl(serverUrl, entry.publisher, entry.name, "file", extVer.getIconFileName());
        entry.downloadUrl = createApiUrl(serverUrl, entry.publisher, entry.name, "file", extVer.getExtensionFileName());
        return entry;
    }

    private ExtensionJson toJson(ExtensionVersion extVersion, boolean isLatest) {
        var extension = extVersion.getExtension();
        var json = extVersion.toExtensionJson();
        json.reviewCount = repositories.countReviews(extension);
        json.publisherUrl = createApiUrl(serverUrl, json.publisher);
        json.reviewsUrl = createApiUrl(serverUrl, json.publisher, json.name, "reviews");
        var allVersions = CollectionUtil.map(repositories.findVersions(extension),
                extVer -> new SemanticVersion(extVer.getVersion()));
        Collections.sort(allVersions, Comparator.reverseOrder());
        json.allVersions = new LinkedHashMap<>();
        for (var semVer : allVersions) {
            String url = createApiUrl(serverUrl, json.publisher, json.name, semVer.toString());
            json.allVersions.put(semVer.toString(), url);
        }
        if (isLatest) {
            json.downloadUrl = createApiUrl(serverUrl, json.publisher, json.name, "file", extVersion.getExtensionFileName());
            json.iconUrl = createApiUrl(serverUrl, json.publisher, json.name, "file", extVersion.getIconFileName());
            json.readmeUrl = createApiUrl(serverUrl, json.publisher, json.name, "file", extVersion.getReadmeFileName());
        } else {
            json.downloadUrl = createApiUrl(serverUrl, json.publisher, json.name, json.version, "file", extVersion.getExtensionFileName());
            json.iconUrl = createApiUrl(serverUrl, json.publisher, json.name, json.version, "file", extVersion.getIconFileName());
            json.readmeUrl = createApiUrl(serverUrl, json.publisher, json.name, json.version, "file", extVersion.getReadmeFileName());
        }
        if (json.dependencies != null) {
            json.dependencies.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, ref.publisher, ref.extension);
            });
        }
        if (json.bundledExtensions != null) {
            json.bundledExtensions.forEach(ref -> {
                ref.url = createApiUrl(serverUrl, ref.publisher, ref.extension);
            });
        }
        return json;
    }

}