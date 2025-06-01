/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Personal License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.query;

import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.eclipse.openvsx.events.PersonalAccessTokenAccessed;
import org.eclipse.openvsx.events.PersonalAccessTokenCreated;
import org.eclipse.openvsx.events.PersonalAccessTokenDeleted;
import org.eclipse.openvsx.events.UserDataCreated;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class PersonalAccessTokenHandler {

    private final UserAccessTokensRepository repository;

    public PersonalAccessTokenHandler(UserAccessTokensRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void on(PersonalAccessTokenCreated event) {
        var accessToken = new AccessTokenJson();
        accessToken.setId(event.tokenId());
        accessToken.setCreatedTimestamp(event.createdTimestamp());
        accessToken.setDescription(event.description());
        accessToken.setDeleteTokenUrl(UrlUtil.createApiUrl("", "user", "token", "delete", Long.toString(event.tokenId())));

        var userAccessTokens = repository.findById(event.userId()).get();
        var accessTokens = userAccessTokens.getAccessTokens();
        if(accessTokens != null) {
            accessTokens.add(accessToken);
        } else {
            userAccessTokens.setAccessTokens(List.of(accessToken));
        }

        repository.save(userAccessTokens);
    }

    @EventHandler
    public void on(PersonalAccessTokenDeleted event) {
        var userAccessTokens = repository.findById(event.userId()).get();
        var removed = false;
        var iterator = userAccessTokens.getAccessTokens().iterator();
        while(iterator.hasNext()) {
            var accessToken = iterator.next();
            if(accessToken.getId() == event.tokenId()) {
                iterator.remove();
                removed = true;
                break;
            }
        }
        if(!removed) {
            throw new IllegalStateException("Access token doesn't exist");
        }

        repository.save(userAccessTokens);
    }

    @EventHandler
    public void on(PersonalAccessTokenAccessed event) {
        var userAccessTokens = repository.findById(event.userId()).get();
        var accessToken = userAccessTokens.getAccessTokens().stream()
                .filter(t -> t.getId() == event.tokenId())
                .findFirst()
                .get();

        accessToken.setAccessedTimestamp(event.accessedTimestamp());
        repository.save(userAccessTokens);
    }

    @EventHandler
    public void on(UserDataCreated event) {
        if(repository.findById(event.userId()).isPresent()) {
            throw new IllegalStateException("User access tokens already exist");
        }

        var userAccessTokens = new UserAccessTokensData();
        userAccessTokens.setUserId(event.userId());
        repository.save(userAccessTokens);
    }

    @QueryHandler
    public List<AccessTokenJson> handle(GetUserAccessTokens query) {
        var accessTokens = repository.findById(query.userId()).get().getAccessTokens();
        return Optional.ofNullable(accessTokens).orElse(Collections.emptyList());
    }
}
