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
public record ExtensionQueryResult(List<ResultItem> results) {

    public record ResultItem(List<Extension> extensions, List<ResultMetadata> resultMetadata) {}

    public record Extension(
            String extensionId,
            String extensionName,
            String displayName,
            String shortDescription,
            Publisher publisher,
            List<ExtensionVersion> versions,
            List<Statistic> statistics,
            List<String> tags,
            String releaseDate,
            String publishedDate,
            String lastUpdated,
            List<String> categories,
            String flags
    ) {
        public static final String FLAG_PREVIEW = "preview";
    }

    public record Publisher(
            String displayName,
            String publisherId,
            String publisherName,
            String domain,
            Boolean isDomainVerified
    ) {}

    public record ExtensionVersion(
            String version,
            String lastUpdated,
            String assetUri,
            String fallbackAssetUri,
            List<ExtensionFile> files,
            List<Property> properties,
            String targetPlatform
    ) {}

    public record ExtensionFile(String assetType, String source) {
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
    }

    public record Property(String key, String value) {
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
    }

    public record Statistic(String statisticName, double value) {
        public static final String STAT_INSTALL = "install";
        public static final String STAT_AVERAGE_RATING = "averagerating";
        public static final String STAT_RATING_COUNT = "ratingcount";
    }

    public record ResultMetadata(String metadataType, List<ResultMetadataItem> metadataItems) {}

    public record ResultMetadataItem(String name, long count) {}
}