/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.entities;

import jakarta.persistence.*;
import org.eclipse.openvsx.json.PublisherStatisticsJson;

import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "year", "month"})})
public class PublisherStatistics {

    @Id
    @GeneratedValue(generator = "publisherStatisticsSeq")
    @SequenceGenerator(name = "publisherStatisticsSeq", sequenceName = "publisher_statistics_seq")
    private long id;

    @ManyToOne
    @JoinColumn(name = "user_data")
    private UserData user;

    private int year;

    private int month;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "extension_identifier")
    @Column(name = "downloads")
    private Map<String, Long> extensionDownlaods;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "extension_identifier")
    @Column(name = "downloads")
    private Map<String, Long> extensionTotalDownlaods;

    public PublisherStatisticsJson toJson() {
        var json = new PublisherStatisticsJson();
        json.setYear(year);
        json.setMonth(month);
        json.setExtensionDownloads(toExtensionDownloads(extensionDownlaods));
        json.setExtensionTotalDownloads(toExtensionDownloads(extensionTotalDownlaods));

        return json;
    }

    private List<PublisherStatisticsJson.ExtensionDownloads> toExtensionDownloads(Map<String, Long> downloads) {
        return downloads.entrySet().stream()
                .map(entry -> {
                    var mapping = new PublisherStatisticsJson.ExtensionDownloads();
                    mapping.setExtensionIdentifier(entry.getKey());
                    mapping.setDownloads(entry.getValue());
                    return mapping;
                })
                .sorted(Comparator.comparingLong(PublisherStatisticsJson.ExtensionDownloads::getDownloads).reversed())
                .collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
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

    public Map<String, Long> getExtensionDownlaods() {
        return extensionDownlaods;
    }

    public void setExtensionDownlaods(Map<String, Long> extensionDownlaods) {
        this.extensionDownlaods = extensionDownlaods;
    }

    public Map<String, Long> getExtensionTotalDownlaods() {
        return extensionTotalDownlaods;
    }

    public void setExtensionTotalDownlaods(Map<String, Long> extensionTotalDownlaods) {
        this.extensionTotalDownlaods = extensionTotalDownlaods;
    }
}
