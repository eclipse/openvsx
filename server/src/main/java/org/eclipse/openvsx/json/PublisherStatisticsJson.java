/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublisherStatisticsJson extends ResultJson {

    public static PublisherStatisticsJson error(String message) {
        var result = new PublisherStatisticsJson();
        result.setError(message);
        return result;
    }

    private int year;

    private int month;

    private List<ExtensionDownloads> extensionDownloads;

    private List<ExtensionDownloads> extensionTotalDownloads;

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

    public List<ExtensionDownloads> getExtensionDownloads() {
        return extensionDownloads;
    }

    public void setExtensionDownloads(List<ExtensionDownloads> extensionDownloads) {
        this.extensionDownloads = extensionDownloads;
    }

    public List<ExtensionDownloads> getExtensionTotalDownloads() {
        return extensionTotalDownloads;
    }

    public void setExtensionTotalDownloads(List<ExtensionDownloads> extensionTotalDownloads) {
        this.extensionTotalDownloads = extensionTotalDownloads;
    }

    public static class ExtensionDownloads {
        private String extensionIdentifier;

        private long downloads;

        public String getExtensionIdentifier() {
            return extensionIdentifier;
        }

        public void setExtensionIdentifier(String extensionIdentifier) {
            this.extensionIdentifier = extensionIdentifier;
        }

        public long getDownloads() {
            return downloads;
        }

        public void setDownloads(long downloads) {
            this.downloads = downloads;
        }
    }
}
