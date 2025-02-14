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

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublisherStatisticsJson extends ResultJson {

    public static PublisherStatisticsJson error(String message) {
        var result = new PublisherStatisticsJson();
        result.setError(message);
        return result;
    }

    private int year;

    private int month;

    private Map<String, Long> extensionDownloads;

    private Map<String, Long> extensionTotalDownloads;

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

    public Map<String, Long> getExtensionDownloads() {
        return extensionDownloads;
    }

    public void setExtensionDownloads(Map<String, Long> extensionDownlaods) {
        this.extensionDownloads = extensionDownlaods;
    }

    public Map<String, Long> getExtensionTotalDownloads() {
        return extensionTotalDownloads;
    }

    public void setExtensionTotalDownloads(Map<String, Long> extensionTotalDownlaods) {
        this.extensionTotalDownloads = extensionTotalDownlaods;
    }
}
