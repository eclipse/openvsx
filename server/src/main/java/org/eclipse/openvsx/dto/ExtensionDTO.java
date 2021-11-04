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
    private final NamespaceDTO namespace;
    private Long latestId;
    private ExtensionVersionDTO latest;
    private Long previewId;
    private final Double averageRating;
    private final int downloadCount;

    public ExtensionDTO(
            long id,
            String publicId,
            String name,
            Long latestId,
            Long previewId,
            Double averageRating,
            int downloadCount,
            long namespaceId,
            String namespacePublicId,
            String namespaceName
    ) {
        this(
                id, publicId, name, averageRating, downloadCount,
                namespaceId, namespacePublicId, namespaceName
        );

        this.latestId = latestId;
        this.previewId = previewId;
    }

    public ExtensionDTO(
            long id,
            String publicId,
            String name,
            Double averageRating,
            int downloadCount,
            long namespaceId,
            String namespacePublicId,
            String namespaceName,
            long latestId,
            String latestVersion,
            boolean latestPreview,
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
                id, publicId, name, averageRating, downloadCount,
                namespaceId, namespacePublicId, namespaceName
        );

        this.latest = new ExtensionVersionDTO(
                id,
                latestId,
                latestVersion,
                latestPreview,
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
            Double averageRating,
            int downloadCount,
            long namespaceId,
            String namespacePublicId,
            String namespaceName
    ) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
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

    public NamespaceDTO getNamespace() {
        return namespace;
    }

    public Long getLatestId() {
        return latestId;
    }

    public ExtensionVersionDTO getLatest() {
        return latest;
    }

    public Long getPreviewId() {
        return previewId;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public int getDownloadCount() {
        return downloadCount;
    }
}
