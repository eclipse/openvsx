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

import java.time.ZoneOffset;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.eclipse.openvsx.search.ExtensionSearch;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "publicId" }))
public class Extension {

    @Id
    @GeneratedValue
    long id;

    @Column(length = 128)
    String publicId;

    String name;

    @ManyToOne
    Namespace namespace;

    @OneToMany(mappedBy = "extension")
    List<ExtensionVersion> versions;

    @OneToOne
    ExtensionVersion latest;

    @OneToOne
    ExtensionVersion preview;

    Double averageRating;

    int downloadCount;


    /**
     * Convert to a search entity for Elasticsearch.
     */
    public ExtensionSearch toSearch() {
        var search = new ExtensionSearch();
        search.id = this.getId();
        search.name = this.getName();
        search.namespace = this.getNamespace().getName();
        search.averageRating = this.getAverageRating();
        search.downloadCount = this.getDownloadCount();
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
    
    public List<ExtensionVersion> getVersions() {
        return versions;
    }

	public void setNamespace(Namespace namespace) {
		this.namespace = namespace;
	}

	public ExtensionVersion getLatest() {
		return latest;
	}

	public void setLatest(ExtensionVersion latest) {
		this.latest = latest;
	}

	public ExtensionVersion getPreview() {
		return preview;
	}

	public void setPreview(ExtensionVersion preview) {
		this.preview = preview;
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

}