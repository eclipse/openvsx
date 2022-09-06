/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.UpstreamRegistryService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.TimeUtil;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.AbstractMap;
import java.util.Map;

@Component
public class MirrorExtensionMetadataJobRequestHandler implements JobRequestHandler<MirrorExtensionMetadataJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorExtensionMetadataJobRequestHandler.class);

    @Autowired
    DataMirrorService data;

    @Qualifier("mirror")
    @Autowired(required = false)
    IExtensionRegistry mirror;

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    SearchUtilService search;

    @Autowired
    CacheService cache;

    @Value("${ovsx.data.mirror.enabled:false}")
    boolean enabled;

    @Override
    @Transactional
    @Job(name="Mirror Extension Metadata", retries=10)
    public void run(MirrorExtensionMetadataJobRequest jobRequest) throws Exception {
        if(!enabled) {
            return;
        }

        var namespaceName = jobRequest.getNamespace();
        var extensionName = jobRequest.getExtension();
        LOGGER.info(">> Starting MirrorExtensionMetadataJob for {}.{}", namespaceName, extensionName);
        var extension = repositories.findExtension(extensionName, namespaceName);
        if(extension == null || !extension.isActive()) {
            // extension has been deleted or de-activated in the meantime
            return;
        }

        var json = mirror.getExtension(namespaceName, extensionName, null);
        extension.setDownloadCount(json.downloadCount);
        extension.setAverageRating(json.averageRating);

        var remoteReviews = mirror.getReviews(namespaceName, extensionName);
        var localReviews = repositories.findAllReviews(extension)
                .map(review -> new AbstractMap.SimpleEntry<>(review.toReviewJson(), review));

        remoteReviews.reviews.stream()
                .filter(review -> localReviews.stream().noneMatch(entry -> entry.getKey().equals(review)))
                .forEach(review -> addReview(review, extension));

        localReviews.stream()
                .filter(entry -> remoteReviews.reviews.stream().noneMatch(review -> review.equals(entry.getKey())))
                .map(Map.Entry::getValue)
                .forEach(entityManager::remove);

        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        LOGGER.info("<< Completed MirrorExtensionMetadataJob for {}.{}", namespaceName, extensionName);
    }

    private void addReview(ReviewJson json, Extension extension) {
        var review = new ExtensionReview();
        review.setExtension(extension);
        review.setActive(true);
        review.setTimestamp(TimeUtil.fromUTCString(json.timestamp));
        review.setUser(data.getOrAddUser(json.user));
        review.setTitle(json.title);
        review.setComment(json.comment);
        review.setRating(json.rating);
        entityManager.persist(review);
    }
}
