/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;;

@JsonInclude(Include.NON_NULL)
public class ExtensionJson extends ResultJson {

    public static ExtensionJson error(String message) {
        var info = new ExtensionJson();
        info.error = message;
        return info;
    }

    public String namespaceUrl;

    public String reviewsUrl;

    // key: file id ("download", "icon", "readme", "license"), value: url
    public Map<String, String> files;

    public String name;

    public String namespace;

    public UserJson publishedBy;

    public boolean unrelatedPublisher;

    // see constants in NamespaceJson
    public String namespaceAccess;

    // key: version, value: url
    public Map<String, String> allVersions;

    @Nullable
    public Double averageRating;

    public int downloadCount;

    public long reviewCount;

    public String version;

    public String timestamp;

    @Nullable
    public Boolean preview;

    @Nullable
    public String displayName;

    @Nullable
    public String description;

    @Nullable
    public List<String> categories;

    @Nullable
    public List<String> tags;

    @Nullable
    public String license;

    @Nullable
    public String homepage;

    @Nullable
    public String repository;

    @Nullable
    public String bugs;

    @Nullable
    public String markdown;

    @Nullable
    public String galleryColor;

    @Nullable
    public String galleryTheme;

    @Nullable
    public String qna;

    @Nullable
    public List<BadgeJson> badges;

    @Nullable
    public List<ExtensionReferenceJson> dependencies;

    @Nullable
    public List<ExtensionReferenceJson> bundledExtensions;

}