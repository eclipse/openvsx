/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
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
public class AdminStatisticsJson extends ResultJson {

    public static AdminStatisticsJson error(String message) {
        var result = new AdminStatisticsJson();
        result.error = message;
        return result;
    }

    public int year;

    public int month;

    public long extensions;

    public long downloads;

    public long downloadsTotal;

    public long publishers;

    public double averageReviewsPerExtension;

    public long namespaceOwners;

    public List<ExtensionsByRating> extensionsByRating;

    public List<PublishersByExtensionsPublished> publishersByExtensionsPublished;

    public List<TopMostActivePublishingUsers> topMostActivePublishingUsers;

    public List<TopNamespaceExtensions> topNamespaceExtensions;

    public List<TopNamespaceExtensionVersions> topNamespaceExtensionVersions;

    public List<TopMostDownloadedExtensions> topMostDownloadedExtensions;

    public static class ExtensionsByRating {
        public int rating;

        public int extensions;
    }

    public static class PublishersByExtensionsPublished {
        public int extensionsPublished;

        public int publishers;
    }

    public static class TopMostActivePublishingUsers {
        public String userLoginName;

        public int publishedExtensionVersions;
    }

    public static class TopNamespaceExtensions {
        public String namespace;

        public int extensions;
    }

    public static class TopNamespaceExtensionVersions {
        public String namespace;

        public int extensionVersions;
    }

    public static class TopMostDownloadedExtensions {
        public String extensionIdentifier;

        public long downloads;
    }
}
