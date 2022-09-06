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
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.search.SearchUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
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
    public void increaseDownloadCount(ExtensionVersion extVersion, FileResource resource, List<LocalDateTime> downloadTimes) {
        var extension = extVersion.getExtension();
        extension.setDownloadCount(extension.getDownloadCount() + downloadTimes.size());
        for(var time : downloadTimes) {
            var download = new Download();
            download.setAmount(1);
            download.setTimestamp(time);
            download.setFileResourceId(resource.getId());
            entityManager.persist(download);
        }

        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
    }
}
