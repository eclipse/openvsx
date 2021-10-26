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

import org.eclipse.openvsx.util.SemanticVersion;

import java.time.LocalDateTime;
import java.util.List;

public class ExtensionVersionDTO {

    private Long extensionId;
    private ExtensionDTO extension;

    private final long id;
    private final String version;
    private SemanticVersion semver;
    private final boolean preview;
    private final LocalDateTime timestamp;
    private final String displayName;
    private final String description;
    private final List<String> engines;
    private final List<String> categories;
    private final List<String> tags;
    private final List<String> extensionKind;
    private final String repository;
    private final String galleryColor;
    private final String galleryTheme;
    private final List<String> dependencies;
    private final List<String> bundledExtensions;

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

    public String getRepository() {
        return repository;
    }

    public String getGalleryColor() {
        return galleryColor;
    }

    public String getGalleryTheme() {
        return galleryTheme;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getBundledExtensions() {
        return bundledExtensions;
    }
}
