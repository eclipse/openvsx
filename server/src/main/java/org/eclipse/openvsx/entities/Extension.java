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

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.eclipse.openvsx.search.ExtensionSearch;

@Entity
public class Extension {

    @Id
    @GeneratedValue
    long id;

    String name;

    @ManyToOne
    Publisher publisher;

    @OneToMany(mappedBy = "extension")
    List<ExtensionVersion> versions;

    @OneToOne
    ExtensionVersion latest;

    Double averageRating;


    /**
     * Convert to a search entity for Elasticsearch.
     */
    public ExtensionSearch toSearch() {
        var search = new ExtensionSearch();
        search.id = this.getId();
        search.name = this.getName();
        search.publisher = this.getPublisher().getName();
        var extVer = this.getLatest();
        search.displayName = extVer.getDisplayName();
        search.description = extVer.getDescription();
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Publisher getPublisher() {
		return publisher;
	}

	public void setPublisher(Publisher publisher) {
		this.publisher = publisher;
	}

	public ExtensionVersion getLatest() {
		return latest;
	}

	public void setLatest(ExtensionVersion latest) {
		this.latest = latest;
	}

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
    }

}