/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Download;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.List;

@Component
public class DownloadCountService {

    @Autowired
    SearchUtilService search;

    @Autowired
    CacheService cache;

    @Autowired
    EntityManager entityManager;

    /**
     * Register a package file download by increasing its download count.
     */
    public void increaseDownloadCount(FileResource resource) {
        var download = new Download();
        download.setAmount(1);
        download.setTimestamp(TimeUtil.getCurrentUTC());
        download.setFileResourceId(resource.getId());

        increaseDownloadCount(resource.getExtension().getExtension(), List.of(download));
    }

    /**
     * Register a package file download by increasing its download count.
     */
    public void increaseDownloadCount(Extension extension, List<Download> downloads) {
        downloads.forEach(entityManager::persist);
        extension.setDownloadCount(extension.getDownloadCount() + downloads.size());
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        if (extension.isActive()) {
            search.updateSearchEntry(extension);
        }
    }
}
