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

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class ExtensionDTO implements Serializable {

    private final long id;
    private final String publicId;
    private final String name;
    private final NamespaceDTO namespace;
    private final Double averageRating;
    private final int downloadCount;
    private final LocalDateTime publishedDate;
    private final LocalDateTime lastUpdatedDate;

    public ExtensionDTO(
            long id,
            String publicId,
            String name,
            Double averageRating,
            int downloadCount,
            LocalDateTime publishedDate,
            LocalDateTime lastUpdatedDate,
            long namespaceId,
            String namespacePublicId,
            String namespaceName
    ) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.averageRating = averageRating;
        this.downloadCount = downloadCount;
        this.publishedDate = publishedDate;
        this.lastUpdatedDate = lastUpdatedDate;

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

    public Double getAverageRating() {
        return averageRating;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public LocalDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionDTO that = (ExtensionDTO) o;
        return id == that.id
                && downloadCount == that.downloadCount
                && Objects.equals(publicId, that.publicId)
                && Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(averageRating, that.averageRating)
                && Objects.equals(publishedDate, that.publishedDate)
                && Objects.equals(lastUpdatedDate, that.lastUpdatedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, publicId, name, namespace, averageRating, downloadCount, publishedDate, lastUpdatedDate);
    }
}
