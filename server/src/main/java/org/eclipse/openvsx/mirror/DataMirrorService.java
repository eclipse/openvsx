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

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.IExtensionRegistry;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.AbstractMap;
import java.util.Map;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
public class DataMirrorService {

    protected final Logger logger = LoggerFactory.getLogger(DataMirrorService.class);

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

    @Autowired
    ExtensionService extensions;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    IExtensionRegistry mirror;

    @Transactional
    public void createMirrorUser(String loginName) {
        if(repositories.findUserByLoginName(null, loginName) == null) {
            var user = new UserData();
            user.setLoginName(loginName);
            entityManager.persist(user);
        }
    }

    @Transactional
    public UserData getOrAddUser(UserJson json) {
        var user = repositories.findUserByLoginName(json.provider, json.loginName);
        if(user == null) {
            // TODO do we need all of the data?
            // TODO Is this legal (GDPR, other laws)? I'm not a lawyer.
            user = new UserData();
            user.setLoginName(json.loginName);
            user.setFullName(json.fullName);
            user.setAvatarUrl(json.avatarUrl);
            user.setProviderUrl(json.homepage);
            user.setProvider(json.provider);
            entityManager.persist(user);
        }

        return user;
    }

    @Transactional
    public void updateTimestamps(String namespaceName, String extensionName, String targetPlatform, String version, String timestamp) {
        var extVersion = repositories.findVersion(version, targetPlatform, extensionName, namespaceName);
        extVersion.setTimestamp(TimeUtil.fromUTCString(timestamp));
        var extension = extVersion.getExtension();
        if(extension.getPublishedDate().equals(extension.getLastUpdatedDate())) {
            extension.setPublishedDate(extVersion.getTimestamp());
        }

        extension.setLastUpdatedDate(extVersion.getTimestamp());
    }

    public String getOrAddAccessTokenValue(UserData user, String description) {
        return repositories.findAccessTokens(user)
                .filter(PersonalAccessToken::isActive)
                .filter(token -> token.getDescription().equals(description))
                .stream()
                .findFirst()
                .map(PersonalAccessToken::getValue)
                .orElse(users.createAccessToken(user, description).value);
    }

    @Transactional
    public Extension getDeactivatedExtension(String extensionName, String namespaceName) {
        var extension = repositories.findExtension(extensionName, namespaceName);
        if(extension != null) {
            extension.getVersions().forEach(extVersion -> extVersion.setActive(false));
            extensions.updateExtension(extension);
        }

        return extension;
    }

    @Transactional
    public void activateExtension(String namespaceName, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespaceName);
        extension.getVersions().stream().filter(this::canGetIcon).forEach(extVersion -> extVersion.setActive(true));
        extensions.updateExtension(extension);
    }

    private boolean canGetIcon(ExtensionVersion extVersion) {
        var resource = repositories.findFileByType(extVersion, FileResource.ICON);
        if (resource == null){
            return false;
        }

        var url = storageUtil.getLocation(resource);
        var request = new RequestEntity<Void>(HttpMethod.HEAD, url);
        try {
            restTemplate.exchange(request, byte[].class);
        } catch(HttpClientErrorException | HttpServerErrorException exc) {
            logger.error(exc.getStatusCode().value() + " " + exc.getStatusCode().name() + " - " + url);
            return false;
        } catch(RestClientException exc) {
            logger.error("HEAD " + url, exc);
            return false;
        }

        return true;
    }

    @Transactional
    public void updateMetadata(String namespaceName, String extensionName) {
        var extension = repositories.findExtension(extensionName, namespaceName);
        if(extension == null) {
            // extension has been deleted in the meantime
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
    }

    private void addReview(ReviewJson json, Extension extension) {
        var review = new ExtensionReview();
        review.setExtension(extension);
        review.setActive(true);
        review.setTimestamp(TimeUtil.fromUTCString(json.timestamp));
        review.setUser(getOrAddUser(json.user));
        review.setTitle(json.title);
        review.setComment(json.comment);
        review.setRating(json.rating);
        entityManager.persist(review);
    }
}
