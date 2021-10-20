package org.eclipse.openvsx.repositories;

import org.eclipse.openvsx.dto.ExtensionVersionDTO;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.util.Collection;
import java.util.List;

public interface ExtensionVersionDTORepository extends Repository<ExtensionVersion, Long> {

    @Query("select new org.eclipse.openvsx.dto.ExtensionVersionDTO(" +
            "ev.extension.id," +
            "ev.id," +
            "ev.version," +
            "ev.preview," +
            "ev.timestamp," +
            "ev.displayName," +
            "ev.description," +
            "ev.engines," +
            "ev.categories," +
            "ev.tags," +
            "ev.extensionKind," +
            "ev.repository," +
            "ev.galleryColor," +
            "ev.galleryTheme," +
            "ev.dependencies," +
            "ev.bundledExtensions" +
            ") " +
            "from ExtensionVersion ev " +
            "where ev.active = true and ev.extension.id in(?1)")
    Streamable<ExtensionVersionDTO> findAllActiveByExtensionId(Collection<Long> extensionIds);
}
