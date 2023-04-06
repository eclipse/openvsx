/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import java.util.List;

// Keep interfaces in sync with
// https://github.com/microsoft/vscode/blob/de0724b414e2f95f6cc484b03bccbc96686c2cfd/src/vs/platform/extensionManagement/common/extensionGalleryService.ts#L34-L81
public class ExtensionQueryResult {

    public List<ResultItem> results;

    public static class ResultItem {
        public List<Extension> extensions;
        public List<ResultMetadata> resultMetadata;
    }

    public static class Extension {
        public static final String FLAG_PREVIEW = "preview";

        public String extensionId;
        public String extensionName;
        public String displayName;
        public String shortDescription;
        public Publisher publisher;
        public List<ExtensionVersion> versions;
        public List<Statistic> statistics;
        public List<String> tags;
        public String releaseDate;
        public String publishedDate;
        public String lastUpdated;
        public List<String> categories;
        public String flags;
    }

    public static class Publisher {
        public String displayName;
        public String publisherId;
        public String publisherName;
        public String domain;
        public Boolean isDomainVerified;
    }

    public static class ExtensionVersion {
        public String version;
        public String lastUpdated;
        public String assetUri;
        public String fallbackAssetUri;
        public List<ExtensionFile> files;
        public List<Property> properties;
        public String targetPlatform;

        public void addFile(String assetType, String source) {
            if (source != null) {
                var file = new ExtensionFile();
                file.assetType = assetType;
                file.source = source;
                files.add(file);
            }
        }

        public void addProperty(String key, String value) {
            if (value != null) {
                var repositoryProp = new Property();
                repositoryProp.key = key;
                repositoryProp.value = value;
                properties.add(repositoryProp);
            }
        }
    }

    public static class ExtensionFile {
        public static final String FILE_ICON = "Microsoft.VisualStudio.Services.Icons.Default";
        public static final String FILE_DETAILS = "Microsoft.VisualStudio.Services.Content.Details";
        public static final String FILE_CHANGELOG = "Microsoft.VisualStudio.Services.Content.Changelog";
        public static final String FILE_MANIFEST = "Microsoft.VisualStudio.Code.Manifest";
        public static final String FILE_VSIX = "Microsoft.VisualStudio.Services.VSIXPackage";
        public static final String FILE_LICENSE = "Microsoft.VisualStudio.Services.Content.License";
        public static final String FILE_WEB_RESOURCES = "Microsoft.VisualStudio.Code.WebResources/";
        public static final String FILE_VSIXMANIFEST = "Microsoft.VisualStudio.Services.VsixManifest";
        public static final String FILE_SIGNATURE = "Microsoft.VisualStudio.Services.VsixSignature";
        public static final String FILE_PUBLIC_KEY = "Microsoft.VisualStudio.Services.PublicKey";

        public String assetType;
        public String source;
    }

    public static class Property {
        public static final String PROP_REPOSITORY = "Microsoft.VisualStudio.Services.Links.Source";
        public static final String PROP_SPONSOR_LINK = "Microsoft.VisualStudio.Code.SponsorLink";
        public static final String PROP_DEPENDENCY = "Microsoft.VisualStudio.Code.ExtensionDependencies";
        public static final String PROP_EXTENSION_PACK = "Microsoft.VisualStudio.Code.ExtensionPack";
        public static final String PROP_ENGINE = "Microsoft.VisualStudio.Code.Engine";
        public static final String PROP_LOCALIZED_LANGUAGES = "Microsoft.VisualStudio.Code.LocalizedLanguages";
        public static final String PROP_BRANDING_COLOR = "Microsoft.VisualStudio.Services.Branding.Color";
        public static final String PROP_BRANDING_THEME = "Microsoft.VisualStudio.Services.Branding.Theme";
        public static final String PROP_WEB_EXTENSION = "Microsoft.VisualStudio.Code.WebExtension";
        public static final String PROP_PRE_RELEASE = "Microsoft.VisualStudio.Code.PreRelease";

        public String key;
        public String value;
    }

    public static class Statistic {
        public static final String STAT_INSTALL = "install";
        public static final String STAT_AVERAGE_RATING = "averagerating";
        public static final String STAT_RATING_COUNT = "ratingcount";

        public String statisticName;
        public double value;
    }

    public static class ResultMetadata {
        public String metadataType;
        public List<ResultMetadataItem> metadataItems;
    }

    public static class ResultMetadataItem {
        public String name;
        public long count;
    }

}