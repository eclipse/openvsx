package org.eclipse.openvsx.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ExtensionDTO {

    private final long id;
    private final String publicId;
    private final String name;
    private final NamespaceDTO namespace;
    private final ExtensionVersionDTO latest;
    private final Double averageRating;
    private final int downloadCount;

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
            List<String> latestEngines,
            List<String> latestCategories,
            List<String> latestTags,
            List<String> latestExtensionKind,
            String latestRepository,
            String latestGalleryColor,
            String latestGalleryTheme,
            List<String> latestDependencies,
            List<String> latestBundledExtensions
    ) {
        this.id = id;
        this.publicId = publicId;
        this.name = name;
        this.averageRating = averageRating;
        this.downloadCount = downloadCount;

        this.namespace = new NamespaceDTO(namespaceId, namespacePublicId, namespaceName);
        this.latest = new ExtensionVersionDTO(
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

    public ExtensionVersionDTO getLatest() {
        return latest;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public int getDownloadCount() {
        return downloadCount;
    }
}
