/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.dto;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.ExtensionReferenceJson;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.TimeUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExtensionVersionDTO {

    private Long extensionId;
    private ExtensionDTO extension;

    private final long id;
    private final String version;
    private SemanticVersion semver;
    private final boolean preview;
    private final LocalDateTime timestamp;
    private PersonalAccessTokenDTO publishedWith;
    private final String displayName;
    private final String description;
    private final List<String> engines;
    private final List<String> categories;
    private final List<String> tags;
    private final List<String> extensionKind;
    private String license;
    private String homepage;
    private final String repository;
    private String bugs;
    private String markdown;
    private final String galleryColor;
    private final String galleryTheme;
    private String qna;
    private final List<String> dependencies;
    private final List<String> bundledExtensions;

    public ExtensionVersionDTO(
            long namespaceId,
            String namespacePublicId,
            String namespaceName,
            long extensionId,
            String extensionPublicId,
            String extensionName,
            Long extensionLatestId,
            Long extensionPreviewId,
            Double extensionAverageRating,
            Integer extensionDownloadCount,
            Long userId,
            String userLoginName,
            String userFullName,
            String userAvatarUrl,
            String userProviderUrl,
            String userProvider,
            long id,
            String version,
            boolean preview,
            LocalDateTime timestamp,
            String displayName,
            String description,
            List<String> engines,
            List<String> categories,
            List<String> tags,
            List<String> extensionKind,
            String license,
            String homepage,
            String repository,
            String bugs,
            String markdown,
            String galleryColor,
            String galleryTheme,
            String qna,
            List<String> dependencies,
            List<String> bundledExtensions
    ) {
        this(
                id,
                version,
                preview,
                timestamp,
                displayName,
                description,
                engines,
                categories,
                tags,
                extensionKind,
                repository,
                galleryColor,
                galleryTheme,
                dependencies,
                bundledExtensions
        );

        this.extensionId = extensionId;
        this.extension = new ExtensionDTO(
                extensionId,
                extensionPublicId,
                extensionName,
                extensionLatestId,
                extensionPreviewId,
                extensionAverageRating,
                extensionDownloadCount,
                namespaceId,
                namespacePublicId,
                namespaceName
        );

        if(userId != null) {
            this.publishedWith = new PersonalAccessTokenDTO(userId, userLoginName, userFullName, userAvatarUrl, userProviderUrl, userProvider);
        }

        this.license = license;
        this.homepage = homepage;
        this.bugs = bugs;
        this.markdown = markdown;
        this.qna = qna;
    }

    public ExtensionVersionDTO(
            long extensionId,
            long id,
            String version,
            boolean preview,
            LocalDateTime timestamp,
            String displayName,
            String description,
            List<String> engines,
            List<String> categories,
            List<String> tags,
            List<String> extensionKind,
            String repository,
            String galleryColor,
            String galleryTheme,
            List<String> dependencies,
            List<String> bundledExtensions
    ) {
        this(
                id,
                version,
                preview,
                timestamp,
                displayName,
                description,
                engines,
                categories,
                tags,
                extensionKind,
                repository,
                galleryColor,
                galleryTheme,
                dependencies,
                bundledExtensions
        );

        this.extensionId = extensionId;
    }

    public ExtensionVersionDTO(
            long id,
            String version,
            boolean preview,
            LocalDateTime timestamp,
            String displayName,
            String description,
            List<String> engines,
            List<String> categories,
            List<String> tags,
            List<String> extensionKind,
            String repository,
            String galleryColor,
            String galleryTheme,
            List<String> dependencies,
            List<String> bundledExtensions
    ) {
        this.id = id;
        this.version = version;
        this.preview = preview;
        this.timestamp = timestamp;
        this.displayName = displayName;
        this.description = description;
        this.engines = engines;
        this.categories = categories;
        this.tags = tags;
        this.extensionKind = extensionKind;
        this.repository = repository;
        this.galleryColor = galleryColor;
        this.galleryTheme = galleryTheme;
        this.dependencies = dependencies;
        this.bundledExtensions = bundledExtensions;
    }

    private List<ExtensionReferenceJson> toExtensionReferenceJson(List<String> extensionReferences) {
        return extensionReferences.stream().map(fqn -> {
            var startIndex = fqn.indexOf('.');
            var lastIndex = fqn.lastIndexOf('.');
            if (startIndex <= 0 || lastIndex >= fqn.length() - 1 || startIndex != lastIndex) {
                return null;
            }
            var ref = new ExtensionReferenceJson();
            ref.namespace = fqn.substring(0, startIndex);
            ref.extension = fqn.substring(startIndex + 1);
            return ref;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Convert to a JSON object without URLs.
     */
    public ExtensionJson toExtensionJson() {
        var json = new ExtensionJson();
        var extension = this.getExtension();
        json.namespace = extension.getNamespace().getName();
        json.name = extension.getName();
        json.averageRating = extension.getAverageRating();
        json.downloadCount = extension.getDownloadCount();
        json.version = this.getVersion();
        json.preview = this.isPreview();
        if (this.getTimestamp() != null) {
            json.timestamp = TimeUtil.toUTCString(this.getTimestamp());
        }
        json.displayName = this.getDisplayName();
        json.description = this.getDescription();
        json.engines = this.getEnginesMap();
        json.categories = this.getCategories();
        json.extensionKind = this.getExtensionKind();
        json.tags = this.getTags();
        json.license = this.getLicense();
        json.homepage = this.getHomepage();
        json.repository = this.getRepository();
        json.bugs = this.getBugs();
        json.markdown = this.getMarkdown();
        json.galleryColor = this.getGalleryColor();
        json.galleryTheme = this.getGalleryTheme();
        json.qna = this.getQna();
        if (this.getPublishedWith() != null) {
            json.publishedBy = this.getPublishedWith().getUser().toUserJson();
        }
        if (this.getDependencies() != null) {
            json.dependencies = toExtensionReferenceJson(this.getDependencies());
        }
        if (this.getBundledExtensions() != null) {
            json.bundledExtensions = toExtensionReferenceJson(this.getBundledExtensions());
        }
        return json;
    }

    private Map<String, String> getEnginesMap() {
        var engines = this.getEngines();
        if (engines == null)
            return null;
        var result = Maps.<String, String>newLinkedHashMapWithExpectedSize(engines.size());
        for (var engine : engines) {
            var split = engine.split("@");
            if (split.length == 2) {
                result.put(split[0], split[1]);
            }
        }
        return result;
    }

    public long getExtensionId() {
        return extensionId;
    }

    public void setExtensionId(long extensionId) {
        this.extensionId = extensionId;
    }

    public ExtensionDTO getExtension() {
        return extension;
    }

    public void setExtension(ExtensionDTO extension) {
        if(extensionId == null || extension.getId() == extensionId) {
            this.extension = extension;
        } else {
            throw new IllegalArgumentException("extension must have the same id as extensionId");
        }
    }

    public long getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public SemanticVersion getSemanticVersion() {
        if (semver == null) {
            var version = getVersion();
            if (version != null)
                semver = new SemanticVersion(version);
        }
        return semver;
    }

    public boolean isPreview() {
        return preview;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public PersonalAccessTokenDTO getPublishedWith() {
        return publishedWith;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getEngines() {
        return engines;
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getExtensionKind() {
        return extensionKind;
    }

    public String getLicense() {
        return license;
    }

    public String getHomepage() {
        return homepage;
    }

    public String getRepository() {
        return repository;
    }

    public String getBugs() {
        return bugs;
    }

    public String getMarkdown() {
        return markdown;
    }

    public String getGalleryColor() {
        return galleryColor;
    }

    public String getGalleryTheme() {
        return galleryTheme;
    }

    public String getQna() {
        return qna;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getBundledExtensions() {
        return bundledExtensions;
    }
}
