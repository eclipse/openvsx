/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.dto;

import java.time.LocalDateTime;

public class ExtensionDTO {

    private final long id;
    private final String publicId;
    private final String name;
    private final boolean preview;
    private final NamespaceDTO namespace;
    private Long latestId;
    private ExtensionVersionDTO latest;
    private Long latestPreReleaseId;
    private final Double averageRating;
    private final int downloadCount;

    public ExtensionDTO(
            long id,
            String publicId,
            String name,
            boolean preview,
            Long latestId,
            Long latestPreReleaseId,
            Double averageRating,
            int downloadCount,
            long namespaceId,
            String namespacePublicId,
            String namespaceName
    ) {
        this(
                id, publicId, name, preview, averageRating, downloadCount,
                namespaceId, namespacePublicId, namespaceName
        );

        this.latestId = latestId;
        this.latestPreReleaseId = latestPreReleaseId;
    }

    public ExtensionDTO(
            long id,
            String publicId,
            String name,
            boolean preview,
            Double averageRating,
            int downloadCount,
            long namespaceId,
            String namespacePublicId,
            String namespaceName,
            long latestId,
            String latestVersion,
            boolean latestPreRelease,
            LocalDateTime latestTimestamp,
            String latestDisplayName,
            String latestDescription,
            String latestEngines,
            String latestCategories,
            String latestTags,
            String latestExtensionKind,
            String latestRepository,
            String latestGalleryColor,
            String latestGalleryTheme,
            String latestDependencies,
            String latestBundledExtensions
    ) {
        this(
                id, publicId, name, preview, averageRating, downloadCount,
                namespaceId, namespacePublicId, namespaceName
        );

        this.latest = new ExtensionVersionDTO(
                id,
                latestId,
                latestVersion,
                latestPreRelease,
                latestTimestamp,
                latestDisplayName,
                latestDescription,
                latestEngines,
                latestCategories,
                latestTags,
                latestExtensionKind,
                latestRepository,
                latestGalleryColor,
                latestGalleryTheme,
                latestDependencies,
                latestBundledExtensions
        );
        this.latest.setExtension(this);
    }

    public ExtensionDTO(
            long id,
            String publicId,
            String name,
            boolean preview,
            Double averageRating,
            int downloadCount,
            long namespaceId,
            String namespacePublicId,
            String namespaceName
    ) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.preview = preview;
        this.averageRating = averageRating;
        this.downloadCount = downloadCount;

        this.namespace = new NamespaceDTO(namespaceId, namespacePublicId, namespaceName);
    }

    public long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    public boolean isPreview() { return preview; }

    public NamespaceDTO getNamespace() {
        return namespace;
    }

    public Long getLatestId() {
        return latestId;
    }

    public ExtensionVersionDTO getLatest() {
        return latest;
    }

    public Long getLatestPreReleaseId() {
        return latestPreReleaseId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public int getDownloadCount() {
        return downloadCount;
    }
}
