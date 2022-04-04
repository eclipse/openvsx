/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import javax.persistence.*;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.ExtensionReferenceJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.TimeUtil;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "targetPlatform", "version" })})
public class ExtensionVersion {

    public static final Comparator<ExtensionVersion> SORT_COMPARATOR =
        Comparator.<ExtensionVersion, SemanticVersion>comparing(ev -> ev.getSemanticVersion())
                .thenComparing(Comparator.comparing(ev -> ev.getTimestamp()))
                .reversed();

    @Id
    @GeneratedValue
    long id;

    @ManyToOne
    Extension extension;

    String version;

    String targetPlatform;

    @Transient
    SemanticVersion semver;

    boolean preRelease;

    boolean preview;

    LocalDateTime timestamp;

    @ManyToOne
    PersonalAccessToken publishedWith;

    boolean active;

    String displayName;

    @Column(length = 2048)
    String description;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    List<String> engines;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    List<String> categories;

    @Column(length = 16384)
    @Convert(converter = ListOfStringConverter.class)
    List<String> tags;

    @Column
    @Convert(converter = ListOfStringConverter.class)
    List<String> extensionKind;

    String license;

    String homepage;

    String repository;

    String bugs;

    @Column(length = 16)
    String markdown;

    @Column(length = 16)
    String galleryColor;

    @Column(length = 16)
    String galleryTheme;

    String qna;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    List<String> dependencies;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    List<String> bundledExtensions;


    /**
     * Convert to a JSON object without URLs.
     */
    public ExtensionJson toExtensionJson() {
        var json = new ExtensionJson();
        json.targetPlatform = this.getTargetPlatform();
        json.namespace = extension.getNamespace().getName();
        json.name = extension.getName();
        json.averageRating = extension.getAverageRating();
        json.downloadCount = extension.getDownloadCount();
        json.version = this.getVersion();
        json.preRelease = this.isPreRelease();
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
     * Convert to a search entry JSON object without URLs.
     */
    public SearchEntryJson toSearchEntryJson() {
        var entry = new SearchEntryJson();
        var extension = this.getExtension();
        entry.name = extension.getName();
        entry.namespace = extension.getNamespace().getName();
        entry.averageRating = extension.getAverageRating();
        entry.downloadCount = extension.getDownloadCount();
        entry.version = this.getVersion();
        entry.timestamp = TimeUtil.toUTCString(this.getTimestamp());
        entry.displayName = this.getDisplayName();
        entry.description = this.getDescription();
        return entry;
    }

    public Map<String, String> getEnginesMap() {
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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Extension getExtension() {
        return extension;
    }

    public void setExtension(Extension extension) {
        this.extension = extension;
    }

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
    }

    public SemanticVersion getSemanticVersion() {
        if (semver == null) {
            var version = getVersion();
            if (version != null)
                semver = new SemanticVersion(version);
        }
        return semver;
    }

	public boolean isPreRelease() {
		return preRelease;
	}

	public void setPreRelease(boolean preRelease) {
		this.preRelease = preRelease;
	}

	public boolean isPreview() {
        return preview; 
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

    public PersonalAccessToken getPublishedWith() {
        return publishedWith;
    }

    public void setPublishedWith(PersonalAccessToken publishedWith) {
        this.publishedWith = publishedWith;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public List<String> getEngines() {
		return engines;
	}

	public void setEngines(List<String> engines) {
		this.engines = engines;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public List<String> getExtensionKind() {
		return extensionKind;
	}

	public void setExtensionKind(List<String> extensionKind) {
		this.extensionKind = extensionKind;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getHomepage() {
		return homepage;
	}

	public void setHomepage(String homepage) {
		this.homepage = homepage;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getBugs() {
		return bugs;
	}

	public void setBugs(String bugs) {
		this.bugs = bugs;
	}

	public String getMarkdown() {
		return markdown;
	}

	public void setMarkdown(String markdown) {
		this.markdown = markdown;
	}

	public String getGalleryColor() {
		return galleryColor;
	}

	public void setGalleryColor(String galleryColor) {
		this.galleryColor = galleryColor;
	}

	public String getGalleryTheme() {
		return galleryTheme;
	}

	public void setGalleryTheme(String galleryTheme) {
		this.galleryTheme = galleryTheme;
	}

	public String getQna() {
		return qna;
	}

	public void setQna(String qna) {
		this.qna = qna;
	}

	public List<String> getDependencies() {
		return dependencies;
	}

	public void setDependencies(List<String> dependencies) {
		this.dependencies = dependencies;
	}

	public List<String> getBundledExtensions() {
		return bundledExtensions;
	}

	public void setBundledExtensions(List<String> bundledExtensions) {
		this.bundledExtensions = bundledExtensions;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionVersion that = (ExtensionVersion) o;
        return id == that.id
                && preRelease == that.preRelease
                && preview == that.preview
                && active == that.active
                && Objects.equals(getId(extension), getId(that.extension)) // use id to prevent infinite recursion
                && Objects.equals(version, that.version)
                && Objects.equals(targetPlatform, that.targetPlatform)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(getId(publishedWith), getId(that.publishedWith)) // use id to prevent infinite recursion
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description)
                && Objects.equals(engines, that.engines)
                && Objects.equals(categories, that.categories)
                && Objects.equals(tags, that.tags)
                && Objects.equals(extensionKind, that.extensionKind)
                && Objects.equals(license, that.license)
                && Objects.equals(homepage, that.homepage)
                && Objects.equals(repository, that.repository)
                && Objects.equals(bugs, that.bugs)
                && Objects.equals(markdown, that.markdown)
                && Objects.equals(galleryColor, that.galleryColor)
                && Objects.equals(galleryTheme, that.galleryTheme)
                && Objects.equals(qna, that.qna)
                && Objects.equals(dependencies, that.dependencies)
                && Objects.equals(bundledExtensions, that.bundledExtensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, preRelease, preview, active, getId(extension), version, targetPlatform, timestamp,
                getId(publishedWith), displayName, description, engines, categories, tags, extensionKind, license,
                homepage, repository, bugs, markdown, galleryColor, galleryTheme, qna, dependencies, bundledExtensions);
    }

    private Long getId(Extension extension) {
        return Optional.ofNullable(extension).map(Extension::getId).orElse(null);
    }

    private Long getId(PersonalAccessToken token) {
        return Optional.ofNullable(token).map(PersonalAccessToken::getId).orElse(null);
    }
}