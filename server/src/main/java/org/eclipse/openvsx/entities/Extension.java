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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.*;

import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.util.NamingUtil;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "publicId" }),
        @UniqueConstraint(columnNames = { "namespace_id", "name" })
})
public class Extension implements Serializable {

    @Id
    @GeneratedValue(generator = "extensionSeq")
    @SequenceGenerator(name = "extensionSeq", sequenceName = "extension_seq")
    long id;

    @Column(length = 128)
    String publicId;

    String name;

    @ManyToOne
    Namespace namespace;

    @OneToMany(mappedBy = "extension")
    List<ExtensionVersion> versions;

    boolean active;

    Double averageRating;

    Long reviewCount;

    int downloadCount;

    LocalDateTime publishedDate;

    LocalDateTime lastUpdatedDate;

    /**
     * Convert to a search entity for Elasticsearch.
     */
    public ExtensionSearch toSearch(ExtensionVersion latest) {
        var search = new ExtensionSearch();
        search.id = this.getId();
        search.name = this.getName();
        search.namespace = this.getNamespace().getName();
        search.extensionId = NamingUtil.toExtensionId(search);
        search.downloadCount = this.getDownloadCount();
        search.targetPlatforms = this.getVersions().stream()
                .map(ExtensionVersion::getTargetPlatform)
                .distinct()
                .collect(Collectors.toList());

        search.displayName = latest.getDisplayName();
        search.description = latest.getDescription();
        search.timestamp = latest.getTimestamp().toEpochSecond(ZoneOffset.UTC);
        search.categories = latest.getCategories();
        search.tags = latest.getTags();

        return search;
    }

    public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Namespace getNamespace() {
		return namespace;
    }

	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
    }

    public Long getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Long reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(LocalDateTime lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

    public List<ExtensionVersion> getVersions() {
        if(versions == null) {
            versions = new ArrayList<>();
        }

        return versions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Extension extension = (Extension) o;
        return id == extension.id
                && active == extension.active
                && downloadCount == extension.downloadCount
                && Objects.equals(publicId, extension.publicId)
                && Objects.equals(name, extension.name)
                && Objects.equals(namespace, extension.namespace)
                && Objects.equals(versions, extension.versions)
                && Objects.equals(averageRating, extension.averageRating)
                && Objects.equals(reviewCount, extension.reviewCount)
                && Objects.equals(publishedDate, extension.publishedDate)
                && Objects.equals(lastUpdatedDate, extension.lastUpdatedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publicId, name, namespace, versions, active, averageRating, reviewCount, downloadCount, publishedDate, lastUpdatedDate);
    }
}