/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.entities;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;

@Entity
public class AdminStatistics {

    @Id
    @GeneratedValue
    long id;

    int year;

    int month;

    long extensions;

    long downloads;

    long downloadsTotal;

    long publishers;

    double averageReviewsPerExtension;

    long namespaceOwners;

    @ElementCollection
    @MapKeyColumn(name = "rating")
    @Column(name = "extensions")
    Map<Integer, Integer> extensionsByRating;

    @ElementCollection
    @MapKeyColumn(name = "extensions_published")
    @Column(name = "publishers")
    Map<Integer, Integer> publishersByExtensionsPublished;

    public String toCsv() {
        var ratings = 5;
        var headers = new ArrayList<>(List.of("year", "month", "extensions", "downloads", "downloads_total", "publishers", "average_reviews_per_extension", "namespace_owners"));
        for(int i = 0; i < ratings; i++) {
            headers.add("extensions_by_rating_" + (i + 1));
        }

        var extensionsPublishedAmounts = publishersByExtensionsPublished.keySet().stream().sorted().collect(Collectors.toList());
        for(var amount : extensionsPublishedAmounts) {
            headers.add("publishers_published_extensions_" + amount);
        }

        var values = new ArrayList<>(List.of(year, month, extensions, downloads, downloadsTotal, publishers, averageReviewsPerExtension, namespaceOwners));
        for(int i = 0; i  < ratings; i++) {
            values.add(extensionsByRating.getOrDefault(i + 1, 0));
        }
        for(var amount : extensionsPublishedAmounts) {
            values.add(publishersByExtensionsPublished.get(amount));
        }

        var valueStrings = values.stream().map(String::valueOf).collect(Collectors.joining(","));
        return String.join(",", headers) + "\n" + valueStrings;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public long getExtensions() {
        return extensions;
    }

    public void setExtensions(long extensions) {
        this.extensions = extensions;
    }

    public long getDownloads() {
        return downloads;
    }

    public void setDownloads(long downloads) {
        this.downloads = downloads;
    }

    public long getDownloadsTotal() {
        return downloadsTotal;
    }

    public void setDownloadsTotal(long downloadsTotal) {
        this.downloadsTotal = downloadsTotal;
    }

    public long getPublishers() {
        return publishers;
    }

    public void setPublishers(long publishers) {
        this.publishers = publishers;
    }

    public double getAverageReviewsPerExtension() {
        return averageReviewsPerExtension;
    }

    public void setAverageReviewsPerExtension(double averageReviewsPerExtension) {
        this.averageReviewsPerExtension = averageReviewsPerExtension;
    }

    public long getNamespaceOwners() {
        return namespaceOwners;
    }

    public void setNamespaceOwners(long namespaceOwners) {
        this.namespaceOwners = namespaceOwners;
    }

    public Map<Integer, Integer> getExtensionsByRating() {
        return extensionsByRating;
    }

    public void setExtensionsByRating(Map<Integer, Integer> extensionsByRating) {
        this.extensionsByRating = extensionsByRating;
    }

    public Map<Integer, Integer> getPublishersByExtensionsPublished() {
        return publishersByExtensionsPublished;
    }

    public void setPublishersByExtensionsPublished(Map<Integer, Integer> publishersByExtensionsPublished) {
        this.publishersByExtensionsPublished = publishersByExtensionsPublished;
    }
}
