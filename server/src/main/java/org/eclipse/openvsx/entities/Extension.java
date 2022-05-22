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
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.*;

import org.eclipse.openvsx.search.ExtensionSearch;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionUtil;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = { "publicId" }),
        @UniqueConstraint(columnNames = { "namespace_id", "name" })
})
public class Extension {

    @Id
    @GeneratedValue
    long id;

    @Column(length = 128)
    String publicId;

    String name;

    @ManyToOne
    Namespace namespace;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "extension")
    List<ExtensionVersion> versions;

    boolean active;

    Double averageRating;

    int downloadCount;

    LocalDateTime publishedDate;

    LocalDateTime lastUpdatedDate;

    /**
     * Convert to a search entity for Elasticsearch.
     */
    public ExtensionSearch toSearch() {
        var search = new ExtensionSearch();
        search.id = this.getId();
        search.name = this.getName();
        search.namespace = this.getNamespace().getName();
        search.extensionId = search.namespace + "." + search.name;
        search.averageRating = this.getAverageRating();
        search.downloadCount = this.getDownloadCount();
        search.targetPlatforms = this.getVersions().stream()
                .map(ExtensionVersion::getTargetPlatform)
                .distinct()
                .collect(Collectors.toList());

        var extVer = this.getLatest();
        search.displayName = extVer.getDisplayName();
        search.description = extVer.getDescription();
        search.timestamp = extVer.getTimestamp().toEpochSecond(ZoneOffset.UTC);
        search.categories = extVer.getCategories();
        search.tags = extVer.getTags();
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

    public ExtensionVersion getLatest() {
        return getLatest(null, true);
    }

    public ExtensionVersion getLatest(String targetPlatform, boolean onlyActive) {
        var filters = new ArrayList<Predicate<ExtensionVersion>>();
        if(TargetPlatform.isValid(targetPlatform)) {
            filters.add(ev -> ev.getTargetPlatform().equals(targetPlatform));
        }
        if(onlyActive) {
            filters.add(ExtensionVersion::isActive);
        }

        return VersionUtil.getLatest(getVersions(), filters);
    }

    public ExtensionVersion getLatestPreRelease() {
        return getLatestPreRelease(null, true);
    }

    public ExtensionVersion getLatestPreRelease(String targetPlatform, boolean onlyActive) {
        var filters = new ArrayList<Predicate<ExtensionVersion>>();
        if(TargetPlatform.isValid(targetPlatform)) {
            filters.add(ev -> ev.getTargetPlatform().equals(targetPlatform));
        }

        filters.add(ExtensionVersion::isPreRelease);
        if(onlyActive) {
            filters.add(ExtensionVersion::isActive);
        }

        return VersionUtil.getLatest(getVersions(), filters);
    }
}