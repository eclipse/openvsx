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

import org.eclipse.openvsx.json.AdminStatisticsJson;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "year", "month"})})
public class AdminStatistics {

    @Id
    @GeneratedValue(generator = "adminStatisticsSeq")
    @SequenceGenerator(name = "adminStatisticsSeq", sequenceName = "admin_statistics_seq")
    long id;

    int year;

    int month;

    long extensions;

    long downloads;

    long downloadsTotal;

    long publishers;

    double averageReviewsPerExtension;

    long namespaceOwners;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "rating")
    @Column(name = "extensions")
    Map<Integer, Integer> extensionsByRating;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "extensions_published")
    @Column(name = "publishers")
    Map<Integer, Integer> publishersByExtensionsPublished;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "login_name")
    @Column(name = "extension_version_count")
    Map<String, Integer> topMostActivePublishingUsers;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "namespace")
    @Column(name = "extension_count")
    Map<String, Integer> topNamespaceExtensions;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "namespace")
    @Column(name = "extension_version_count")
    Map<String, Integer> topNamespaceExtensionVersions;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "extension_identifier")
    @Column(name = "downloads")
    Map<String, Long> topMostDownloadedExtensions;

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

        var values = new ArrayList<Number>(List.of(year, month, extensions, downloads, downloadsTotal, publishers, averageReviewsPerExtension, namespaceOwners));
        for(int i = 0; i  < ratings; i++) {
            values.add(extensionsByRating.getOrDefault(i + 1, 0));
        }
        for(var amount : extensionsPublishedAmounts) {
            values.add(publishersByExtensionsPublished.get(amount));
        }

        topMapToCsv(headers, values, topMostActivePublishingUsers, "most_active_publishing_users_");
        topMapToCsv(headers, values, topNamespaceExtensions, "namespace_extensions_");
        topMapToCsv(headers, values, topNamespaceExtensionVersions, "namespace_extension_versions_");
        topMapToCsv(headers, values, topMostDownloadedExtensions, "most_downloaded_extensions_");

        var valueStrings = values.stream().map(String::valueOf).collect(Collectors.joining(","));
        return String.join(",", headers) + "\n" + valueStrings;
    }

    private void topMapToCsv(List<String> headers, List<Number> values, Map<String, ? extends Number> topMap, String headerPrefix) {
        topMap.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, ? extends Number>>comparingLong(entry -> entry.getValue().longValue()).reversed())
                .forEach(entry -> {
                    headers.add(headerPrefix + entry.getKey());
                    values.add(entry.getValue());
                });
    }

    public AdminStatisticsJson toJson() {
        var json = new AdminStatisticsJson();
        json.year = year;
        json.month = month;
        json.extensions = extensions;
        json.downloads = downloads;
        json.downloadsTotal = downloadsTotal;
        json.publishers = publishers;
        json.averageReviewsPerExtension = averageReviewsPerExtension;
        json.namespaceOwners = namespaceOwners;
        json.extensionsByRating = mapExtensionsByRating();
        json.publishersByExtensionsPublished = mapPublishersByExtensionsPublished();
        json.topMostActivePublishingUsers = mapTopMostActivePublishingUsers();
        json.topNamespaceExtensions = mapTopNamespaceExtensions();
        json.topNamespaceExtensionVersions = mapTopNamespaceExtensionVersions();
        json.topMostDownloadedExtensions = mapTopMostDownloadedExtensions();

        return json;
    }

    private List<AdminStatisticsJson.ExtensionsByRating> mapExtensionsByRating() {
        return extensionsByRating.entrySet().stream()
                .map(entry -> {
                    var mapping = new AdminStatisticsJson.ExtensionsByRating();
                    mapping.rating = entry.getKey();
                    mapping.extensions = entry.getValue();
                    return mapping;
                })
                .sorted(Comparator.<AdminStatisticsJson.ExtensionsByRating>comparingInt(er -> er.rating).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminStatisticsJson.PublishersByExtensionsPublished> mapPublishersByExtensionsPublished() {
        return publishersByExtensionsPublished.entrySet().stream()
                .map(entry -> {
                    var mapping = new AdminStatisticsJson.PublishersByExtensionsPublished();
                    mapping.extensionsPublished = entry.getKey();
                    mapping.publishers = entry.getValue();
                    return mapping;
                })
                .sorted(Comparator.<AdminStatisticsJson.PublishersByExtensionsPublished>comparingInt(pe -> pe.extensionsPublished).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminStatisticsJson.TopMostActivePublishingUsers> mapTopMostActivePublishingUsers() {
        return topMostActivePublishingUsers.entrySet().stream()
                .map(entry -> {
                    var mapping = new AdminStatisticsJson.TopMostActivePublishingUsers();
                    mapping.userLoginName = entry.getKey();
                    mapping.publishedExtensionVersions = entry.getValue();
                    return mapping;
                })
                .sorted(Comparator.<AdminStatisticsJson.TopMostActivePublishingUsers>comparingInt(pe -> pe.publishedExtensionVersions).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminStatisticsJson.TopNamespaceExtensions> mapTopNamespaceExtensions() {
        return topNamespaceExtensions.entrySet().stream()
                .map(entry -> {
                    var mapping = new AdminStatisticsJson.TopNamespaceExtensions();
                    mapping.namespace = entry.getKey();
                    mapping.extensions = entry.getValue();
                    return mapping;
                })
                .sorted(Comparator.<AdminStatisticsJson.TopNamespaceExtensions>comparingInt(pe -> pe.extensions).reversed())
                .collect(Collectors.toList());
    }

    public static class TopNamespaceExtensionVersions {
        public String namespace;

        public int extensionVersions;
    }
    private List<AdminStatisticsJson.TopNamespaceExtensionVersions> mapTopNamespaceExtensionVersions() {
        return topNamespaceExtensionVersions.entrySet().stream()
                .map(entry -> {
                    var mapping = new AdminStatisticsJson.TopNamespaceExtensionVersions();
                    mapping.namespace = entry.getKey();
                    mapping.extensionVersions = entry.getValue();
                    return mapping;
                })
                .sorted(Comparator.<AdminStatisticsJson.TopNamespaceExtensionVersions>comparingInt(pe -> pe.extensionVersions).reversed())
                .collect(Collectors.toList());
    }

    private List<AdminStatisticsJson.TopMostDownloadedExtensions> mapTopMostDownloadedExtensions() {
        return topMostDownloadedExtensions.entrySet().stream()
                .map(entry -> {
                    var mapping = new AdminStatisticsJson.TopMostDownloadedExtensions();
                    mapping.extensionIdentifier = entry.getKey();
                    mapping.downloads = entry.getValue();
                    return mapping;
                })
                .sorted(Comparator.<AdminStatisticsJson.TopMostDownloadedExtensions>comparingLong(pe -> pe.downloads).reversed())
                .collect(Collectors.toList());
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

    public Map<String, Integer> getTopMostActivePublishingUsers() {
        return topMostActivePublishingUsers;
    }

    public void setTopMostActivePublishingUsers(Map<String, Integer> topMostActivePublishingUsers) {
        this.topMostActivePublishingUsers = topMostActivePublishingUsers;
    }

    public Map<String, Integer> getTopNamespaceExtensions() {
        return topNamespaceExtensions;
    }

    public void setTopNamespaceExtensions(Map<String, Integer> topNamespaceExtensions) {
        this.topNamespaceExtensions = topNamespaceExtensions;
    }

    public Map<String, Integer> getTopNamespaceExtensionVersions() {
        return topNamespaceExtensionVersions;
    }

    public void setTopNamespaceExtensionVersions(Map<String, Integer> topNamespaceExtensionVersions) {
        this.topNamespaceExtensionVersions = topNamespaceExtensionVersions;
    }

    public Map<String, Long> getTopMostDownloadedExtensions() {
        return topMostDownloadedExtensions;
    }

    public void setTopMostDownloadedExtensions(Map<String, Long> topMostDownloadedExtensions) {
        this.topMostDownloadedExtensions = topMostDownloadedExtensions;
    }
}
