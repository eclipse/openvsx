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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PersonalAccessTokenHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalAccessTokenHandler.class);

    @EventHandler
    public void on(PersonalAccessTokenCreated event) {
        LOGGER.info("RECEIVED PersonalAccessTokenCreated | [{}] {} - {}", event.tokenId(), event.createdTimestamp(), event.description());
    }

    @EventHandler
    public void on(PersonalAccessTokenDeleted event) {
        LOGGER.info("RECEIVED PersonalAccessTokenDeleted | [{}]", event.tokenId());
    }

    @EventHandler
    public void on(PersonalAccessTokenAccessed event) {
        LOGGER.info("RECEIVED PersonalAccessTokenAccessed | [{}] {}", event.tokenId(), event.accessedTimestamp());
    }

    @EventHandler
    public void on(UserDataCreated event) {

    }

    @QueryHandler
    public List<AccessTokenJson> query(GetUserAccessTokens criteria) {
        // return the query result based on given criteria
        return Collections.emptyList();
    }
}
