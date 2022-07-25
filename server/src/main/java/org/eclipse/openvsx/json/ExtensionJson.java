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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(
    value = "Extension",
    description = "Metadata of an extension"
)
@JsonInclude(Include.NON_NULL)
public class ExtensionJson extends ResultJson implements Serializable {

    public static ExtensionJson error(String message) {
        var info = new ExtensionJson();
        info.error = message;
        return info;
    }

    @ApiModelProperty("URL to get metadata of the extension's namespace")
    @NotNull
    public String namespaceUrl;

    @ApiModelProperty("URL to get the list of reviews of this extension")
    @NotNull
    public String reviewsUrl;

    @ApiModelProperty("Map of file types (download, manifest, icon, readme, license, changelog) to their respective URLs")
    public Map<String, String> files;

    @ApiModelProperty("Name of the extension")
    @NotNull
    public String name;

    @ApiModelProperty("Namespace of the extension")
    @NotNull
    public String namespace;

    @ApiModelProperty("Name of the target platform")
    public String targetPlatform;

    @ApiModelProperty("Selected version, or the latest version if none was specified")
    @NotNull
    public String version;

    @ApiModelProperty("Indicates whether this is a pre-release version")
    public Boolean preRelease;

    @ApiModelProperty("Data of the user who published this version")
    @NotNull
    public UserJson publishedBy;

    @ApiModelProperty(hidden = true)
    public Boolean active;

    @ApiModelProperty("The value 'true' means the publishing user is a member of the extension's namespace and the namespace has at least one owner.")
    @NotNull
    public Boolean verified;

    @ApiModelProperty("Deprecated: use 'verified' instead (this property is just the negation of 'verified')")
    @NotNull
    @Deprecated
    public Boolean unrelatedPublisher;

    @ApiModelProperty(value = "Access level of the extension's namespace. Deprecated: namespaces are now always restricted", allowableValues = "public,restricted")
    @NotNull
    @Deprecated
    public String namespaceAccess;

    @ApiModelProperty("Map of available versions to their metadata URLs")
    public Map<String, String> allVersions;

    @ApiModelProperty(value = "Average rating", allowableValues = "range[0,5]")
    public Double averageRating;

    @ApiModelProperty("Number of downloads of the extension package")
    @Min(0)
    public Integer downloadCount;

    @ApiModelProperty("Number of reviews")
    @Min(0)
    public Long reviewCount;

    @ApiModelProperty("Available version aliases ('latest' or 'pre-release')")
    public List<String> versionAlias;

    @ApiModelProperty("Date and time when this version was published (ISO-8601)")
    @NotNull
    public String timestamp;

    @ApiModelProperty("Indicates whether this is a preview extension")
    public Boolean preview;

    @ApiModelProperty("Name to be displayed in user interfaces")
    public String displayName;

    public String description;

    @ApiModelProperty("Map of engine names to the respective version constraints")
    public Map<String, String> engines;

    public List<String> categories;

    @ApiModelProperty("A list that indicates where the extension should run in remote configurations. Values are \"ui\" (run locally), \"workspace\" (run on remote machine) and \"web\"")
    public List<String> extensionKind;

    public List<String> tags;

    @ApiModelProperty("License identifier")
    public String license;

    @ApiModelProperty("URL of the extension's homepage")
    public String homepage;

    @ApiModelProperty("URL of the extension's source repository")
    public String repository;

    @ApiModelProperty("URL of the extension's bug tracker")
    public String bugs;

    @ApiModelProperty(value = "Markdown rendering engine to use in user interfaces", allowableValues = "standard,github")
    public String markdown;

    @ApiModelProperty("CSS color to use as background in user interfaces")
    public String galleryColor;

    @ApiModelProperty(value = "Theme type for user interfaces", allowableValues = "light,dark")
    public String galleryTheme;

    @ApiModelProperty("URL of the extension's Q&A page")
    public String qna;

    @ApiModelProperty("List of badges to display in user interfaces")
    public List<BadgeJson> badges;

    @ApiModelProperty("List of dependencies to other extensions")
    public List<ExtensionReferenceJson> dependencies;

    @ApiModelProperty("List of extensions bundled with this extension")
    public List<ExtensionReferenceJson> bundledExtensions;

    @ApiModelProperty("Map of download links by target platform")
    public Map<String, String> downloads;

    @ApiModelProperty("Map of target platforms by extension version")
    public Map<String, List<String>> allTargetPlatformVersions;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionJson that = (ExtensionJson) o;
        return Objects.equals(namespaceUrl, that.namespaceUrl)
                && Objects.equals(reviewsUrl, that.reviewsUrl)
                && Objects.equals(files, that.files)
                && Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(targetPlatform, that.targetPlatform)
                && Objects.equals(version, that.version)
                && Objects.equals(preRelease, that.preRelease)
                && Objects.equals(publishedBy, that.publishedBy)
                && Objects.equals(active, that.active)
                && Objects.equals(verified, that.verified)
                && Objects.equals(unrelatedPublisher, that.unrelatedPublisher)
                && Objects.equals(namespaceAccess, that.namespaceAccess)
                && Objects.equals(allVersions, that.allVersions)
                && Objects.equals(averageRating, that.averageRating)
                && Objects.equals(downloadCount, that.downloadCount)
                && Objects.equals(reviewCount, that.reviewCount)
                && Objects.equals(versionAlias, that.versionAlias)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(preview, that.preview)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description)
                && Objects.equals(engines, that.engines)
                && Objects.equals(categories, that.categories)
                && Objects.equals(extensionKind, that.extensionKind)
                && Objects.equals(tags, that.tags)
                && Objects.equals(license, that.license)
                && Objects.equals(homepage, that.homepage)
                && Objects.equals(repository, that.repository)
                && Objects.equals(bugs, that.bugs)
                && Objects.equals(markdown, that.markdown)
                && Objects.equals(galleryColor, that.galleryColor)
                && Objects.equals(galleryTheme, that.galleryTheme)
                && Objects.equals(qna, that.qna)
                && Objects.equals(badges, that.badges)
                && Objects.equals(dependencies, that.dependencies)
                && Objects.equals(bundledExtensions, that.bundledExtensions)
                && Objects.equals(downloads, that.downloads)
                && Objects.equals(allTargetPlatformVersions, that.allTargetPlatformVersions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                namespaceUrl, reviewsUrl, files, name, namespace, targetPlatform, version, preRelease, publishedBy,
                active, verified, unrelatedPublisher, namespaceAccess, allVersions, averageRating, downloadCount,
                reviewCount, versionAlias, timestamp, preview, displayName, description, engines, categories,
                extensionKind, tags, license, homepage, repository, bugs, markdown, galleryColor, galleryTheme, qna,
                badges, dependencies, bundledExtensions, downloads, allTargetPlatformVersions
        );
    }
}