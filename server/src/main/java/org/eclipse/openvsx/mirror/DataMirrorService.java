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

import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Component
public class DataMirrorService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

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
}
