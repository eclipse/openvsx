package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.dto.ExtensionDTO;
import org.eclipse.openvsx.dto.ExtensionReviewCountDTO;
import org.eclipse.openvsx.entities.Extension;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;

public interface ExtensionDTORepository extends Repository<Extension, Long> {

    @Query("select new org.eclipse.openvsx.dto.ExtensionDTO(" +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.averageRating," +
            "e.downloadCount," +
            "e.namespace.id," +
            "e.namespace.publicId," +
            "e.namespace.name," +
            "e.latest.id," +
            "e.latest.version," +
            "e.latest.preview," +
            "e.latest.timestamp," +
            "e.latest.displayName," +
            "e.latest.description," +
            "e.latest.engines," +
            "e.latest.categories," +
            "e.latest.tags," +
            "e.latest.extensionKind," +
            "e.latest.repository," +
            "e.latest.galleryColor," +
            "e.latest.galleryTheme," +
            "e.latest.dependencies," +
            "e.latest.bundledExtensions" +
            ") " +
            "from Extension e " +
            "where e.active = true and e.id in(?1)")
    Streamable<ExtensionDTO> findAllActiveById(Collection<Long> ids);

    @Query("select new org.eclipse.openvsx.dto.ExtensionDTO(" +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.averageRating," +
            "e.downloadCount," +
            "e.namespace.id," +
            "e.namespace.publicId," +
            "e.namespace.name," +
            "e.latest.id," +
            "e.latest.version," +
            "e.latest.preview," +
            "e.latest.timestamp," +
            "e.latest.displayName," +
            "e.latest.description," +
            "e.latest.engines," +
            "e.latest.categories," +
            "e.latest.tags," +
            "e.latest.extensionKind," +
            "e.latest.repository," +
            "e.latest.galleryColor," +
            "e.latest.galleryTheme," +
            "e.latest.dependencies," +
            "e.latest.bundledExtensions" +
            ") " +
            "from Extension e " +
            "where e.active = true and e.publicId in(?1)")
    Streamable<ExtensionDTO> findAllActiveByPublicId(Collection<String> publicIds);

    @Query("select new org.eclipse.openvsx.dto.ExtensionDTO(" +
            "e.id," +
            "e.publicId," +
            "e.name," +
            "e.averageRating," +
            "e.downloadCount," +
            "e.namespace.id," +
            "e.namespace.publicId," +
            "e.namespace.name," +
            "e.latest.id," +
            "e.latest.version," +
            "e.latest.preview," +
            "e.latest.timestamp," +
            "e.latest.displayName," +
            "e.latest.description," +
            "e.latest.engines," +
            "e.latest.categories," +
            "e.latest.tags," +
            "e.latest.extensionKind," +
            "e.latest.repository," +
            "e.latest.galleryColor," +
            "e.latest.galleryTheme," +
            "e.latest.dependencies," +
            "e.latest.bundledExtensions" +
            ") " +
            "from Extension e " +
            "where e.active = true and upper(e.name) = upper(?1) and upper(e.namespace.name) = upper(?2)")
    ExtensionDTO findActiveByNameIgnoreCaseAndNamespaceNameIgnoreCase(String name, String namespaceName);

    @Query("select new org.eclipse.openvsx.dto.ExtensionReviewCountDTO(r.extension.id, count(r.id)) " +
            "from ExtensionReview r " +
            "where r.active = true and r.extension.id in(?1) group by r.extension.id")
    Streamable<ExtensionReviewCountDTO> countAllActiveReviewsById(Collection<Long> ids);
}
