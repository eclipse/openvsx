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

import com.google.common.collect.Maps;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.ExtensionReferenceJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.TimeUtil;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "targetPlatform", "version" })})
public class ExtensionVersion implements Serializable {

    public static final Comparator<ExtensionVersion> SORT_COMPARATOR =
        Comparator.comparing(ExtensionVersion::getSemanticVersion)
                .thenComparing(ExtensionVersion::isUniversalTargetPlatform, Comparator.reverseOrder())
                .thenComparing(ExtensionVersion::getTargetPlatform)
                .thenComparing(ExtensionVersion::getTimestamp, Comparator.reverseOrder());

    public enum Type {
        REGULAR,
        MINIMAL,
        EXTENDED
    }

    @Id
    @GeneratedValue(generator = "extensionVersionSeq")
    @SequenceGenerator(name = "extensionVersionSeq", sequenceName = "extension_version_seq")
    long id;

    @ManyToOne
    Extension extension;

    String version;

    String targetPlatform;

    boolean universalTargetPlatform;

    @Embedded
    @AttributeOverride(name = "major", column = @Column(name = "semver_major"))
    @AttributeOverride(name = "minor", column = @Column(name = "semver_minor"))
    @AttributeOverride(name = "patch", column = @Column(name = "semver_patch"))
    @AttributeOverride(name = "preRelease", column = @Column(name = "semver_pre_release"))
    @AttributeOverride(name = "isPreRelease", column = @Column(name = "semver_is_pre_release"))
    @AttributeOverride(name = "buildMetadata", column = @Column(name = "semver_build_metadata"))
    SemanticVersion semver;

    boolean preRelease;

    boolean preview;

    LocalDateTime timestamp;

    @ManyToOne
    PersonalAccessToken publishedWith;

    boolean active;

    boolean potentiallyMalicious;

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

    String sponsorLink;

    String bugs;

    @Column(length = 16)
    String markdown;

    @Column(length = 16)
    String galleryColor;

    @Column(length = 16)
    String galleryTheme;

    @Column
    @Convert(converter = ListOfStringConverter.class)
    List<String> localizedLanguages;

    String qna;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    List<String> dependencies;

    @Column(length = 2048)
    @Convert(converter = ListOfStringConverter.class)
    List<String> bundledExtensions;

    @ManyToOne
    SignatureKeyPair signatureKeyPair;

    @Transient
    Type type;

    /**
     * Convert to a JSON object without URLs.
     */
    public ExtensionJson toExtensionJson() {
        var json = new ExtensionJson();
        json.setTargetPlatform(this.getTargetPlatform());
        var namespace = extension.getNamespace();
        json.setNamespace(namespace.getName());
        json.setNamespaceDisplayName(StringUtils.isNotEmpty(namespace.getDisplayName())
                ? namespace.getDisplayName()
                : json.getNamespace());
        json.setName(extension.getName());
        json.setAverageRating(extension.getAverageRating());
        json.setDownloadCount(extension.getDownloadCount());
        json.setVersion(this.getVersion());
        json.setPreRelease(this.isPreRelease());
        if (this.getTimestamp() != null) {
            json.setTimestamp(TimeUtil.toUTCString(this.getTimestamp()));
        }
        json.setDisplayName(this.getDisplayName());
        json.setDescription(this.getDescription());
        json.setEngines(this.getEnginesMap());
        json.setCategories(this.getCategories());
        json.setExtensionKind(this.getExtensionKind());
        json.setTags(this.getTags());
        json.setLicense(this.getLicense());
        json.setHomepage(this.getHomepage());
        json.setRepository(this.getRepository());
        json.setSponsorLink(this.getSponsorLink());
        json.setBugs(this.getBugs());
        json.setMarkdown(this.getMarkdown());
        json.setGalleryColor(this.getGalleryColor());
        json.setGalleryTheme(this.getGalleryTheme());
        json.setLocalizedLanguages(this.getLocalizedLanguages());
        json.setQna(this.getQna());
        if (this.getPublishedWith() != null) {
            json.setPublishedBy(this.getPublishedWith().getUser().toUserJson());
        }
        if (this.getDependencies() != null) {
            json.setDependencies(toExtensionReferenceJson(this.getDependencies()));
        }
        if (this.getBundledExtensions() != null) {
            json.setBundledExtensions(toExtensionReferenceJson(this.getBundledExtensions()));
        }

        json.setDeprecated(extension.isDeprecated());
        json.setDownloadable(extension.isDownloadable());
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
            ref.setNamespace(fqn.substring(0, startIndex));
            ref.setExtension(fqn.substring(startIndex + 1));
            return ref;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Convert to a search entry JSON object without URLs.
     */
    public SearchEntryJson toSearchEntryJson() {
        var entry = new SearchEntryJson();
        var extension = this.getExtension();
        entry.setName(extension.getName());
        entry.setNamespace(extension.getNamespace().getName());
        entry.setAverageRating(extension.getAverageRating());
        entry.setReviewCount(extension.getReviewCount());
        entry.setDownloadCount(extension.getDownloadCount());
        entry.setVersion(this.getVersion());
        entry.setTimestamp(TimeUtil.toUTCString(this.getTimestamp()));
        entry.setDisplayName(this.getDisplayName());
        entry.setDescription(this.getDescription());
        entry.setDeprecated(extension.isDeprecated());
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
        this.semver = SemanticVersion.parse(version);
    }

    public String getTargetPlatform() {
        return targetPlatform;
    }

    public void setTargetPlatform(String targetPlatform) {
        this.targetPlatform = targetPlatform;
        this.universalTargetPlatform = TargetPlatform.isUniversal(targetPlatform);
    }

    public boolean isUniversalTargetPlatform() {
        return universalTargetPlatform;
    }

    public void setUniversalTargetPlatform(boolean universalTargetPlatform) {
        // do nothing, universalTargetPlatform is derived from targetPlatform
    }

    public SemanticVersion getSemanticVersion() {
        return semver;
    }

    public void setSemanticVersion(SemanticVersion semver) {
        // do nothing, semver is derived from version
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

    public boolean isPotentiallyMalicious() {
        return potentiallyMalicious;
    }

    public void setPotentiallyMalicious(boolean potentiallyMalicious) {
        this.potentiallyMalicious = potentiallyMalicious;
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

	public String getSponsorLink() {
        return sponsorLink;
    }

    public void setSponsorLink(String sponsorLink) {
        this.sponsorLink = sponsorLink;
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

	public List<String> getLocalizedLanguages() {
        return localizedLanguages;
    }

    public void setLocalizedLanguages(List<String> localizedLanguages) {
        this.localizedLanguages = localizedLanguages;
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

    public SignatureKeyPair getSignatureKeyPair() {
        return signatureKeyPair;
    }

    public void setSignatureKeyPair(SignatureKeyPair signatureKeyPair) {
        this.signatureKeyPair = signatureKeyPair;
    }

	public void setType(ExtensionVersion.Type type) {
        this.type = type;
    }

    public ExtensionVersion.Type getType() {
        return type;
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
                && potentiallyMalicious == that.potentiallyMalicious
                && Objects.equals(getId(extension), getId(that.extension)) // use id to prevent infinite recursion
                && Objects.equals(version, that.version)
                && Objects.equals(targetPlatform, that.targetPlatform)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(getId(publishedWith), getId(that.publishedWith)) // use id to prevent infinite recursion                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description)
                && Objects.equals(engines, that.engines)
                && Objects.equals(categories, that.categories)
                && Objects.equals(tags, that.tags)
                && Objects.equals(extensionKind, that.extensionKind)
                && Objects.equals(license, that.license)
                && Objects.equals(homepage, that.homepage)
                && Objects.equals(repository, that.repository)
                && Objects.equals(sponsorLink, that.sponsorLink)
                && Objects.equals(bugs, that.bugs)
                && Objects.equals(markdown, that.markdown)
                && Objects.equals(galleryColor, that.galleryColor)
                && Objects.equals(galleryTheme, that.galleryTheme)
                && Objects.equals(localizedLanguages, that.localizedLanguages)
                && Objects.equals(qna, that.qna)
                && Objects.equals(dependencies, that.dependencies)
                && Objects.equals(bundledExtensions, that.bundledExtensions)
                && Objects.equals(signatureKeyPair, that.signatureKeyPair)
                && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, getId(extension), version, targetPlatform, semver, preRelease, preview, timestamp, getId(publishedWith),
                active, potentiallyMalicious, displayName, description, engines, categories, tags, extensionKind, license, homepage, repository,
                sponsorLink, bugs, markdown, galleryColor, galleryTheme, localizedLanguages, qna, dependencies,
                bundledExtensions, signatureKeyPair, type
        );
    }

    private Long getId(Extension extension) {
        return Optional.ofNullable(extension).map(Extension::getId).orElse(null);
    }

    private Long getId(PersonalAccessToken token) {
        return Optional.ofNullable(token).map(PersonalAccessToken::getId).orElse(null);
    }
}