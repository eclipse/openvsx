/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.UserData;
import org.springframework.data.repository.Repository;
import org.springframework.data.util.Streamable;

import java.time.LocalDateTime;

import static org.eclipse.openvsx.cache.CacheService.CACHE_AVERAGE_REVIEW_RATING;

public interface ExtensionReviewRepository extends Repository<ExtensionReview, Long> {

    Streamable<ExtensionReview> findByExtension(Extension extension);

    Streamable<ExtensionReview> findByExtensionAndActiveTrue(Extension extension);

    Streamable<ExtensionReview> findByExtensionAndUserAndActiveTrue(Extension extension, UserData user);

    long countByExtensionAndActiveTrue(Extension extension);

    @Cacheable(CACHE_AVERAGE_REVIEW_RATING)
    @Query("select coalesce(avg(r.rating),0) from ExtensionReview r where r.active = true")
    double averageRatingAndActiveTrue();

    @Query("select avg(r.rating) from ExtensionReview r where r.active = true and r.extension = ?1")
    Double averageRatingAndActiveTrue(Extension extension);
}