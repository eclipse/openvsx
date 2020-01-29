/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import com.google.common.base.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.entities.UserSession;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;

@RestController
public class UserAPI {

    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // one week in seconds

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Value("#{environment.OVSX_SERVER_URL}")
    String serverUrl;

    @Value("#{environment.OVSX_WEBUI_URL}")
    String webuiUrl;

    @GetMapping(
        value = "/api/-/user",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResponseEntity<UserJson> userInfo(@CookieValue(name = "sessionid", required = false) String sessionId,
                                             HttpServletResponse response) {
        response.addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        addAccessControlHeaders(response);
        if (sessionId == null) {
            var json = UserJson.error("Not logged in.");
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
        var session = repositories.findUserSession(sessionId);
        if (session == null) {
            var json = UserJson.error("Invalid session.");
            return new ResponseEntity<>(json, HttpStatus.OK);
        }

        updateLastUsed(session);
        var json = new UserJson();
        json.name = "test_user";
        json.avatarUrl = "https://s.gravatar.com/avatar/9a638e5879d268e59d158a2091723c3c?s=80";
        response.addCookie(createSessionCookie(session.getId(), COOKIE_MAX_AGE));
        return new ResponseEntity<>(json, HttpStatus.OK);
    }

    @GetMapping("/api/-/user/login")
    @Transactional
    public RedirectView login(@CookieValue(name = "sessionid", required = false) String sessionId,
                              HttpServletResponse response) {
        var session = sessionId != null ? repositories.findUserSession(sessionId) : null;
        if (session == null) {
            session = new UserSession();
            session.setId(UUID.randomUUID().toString());
            session.setUser(getDummyUser());
            entityManager.persist(session);
        }

        updateLastUsed(session);
        addAccessControlHeaders(response);
        response.addCookie(createSessionCookie(session.getId(), COOKIE_MAX_AGE));
        return new RedirectView(getRedirectUrl());
    }

    private UserData getDummyUser() {
        var allUsers = repositories.findAllUsers();
        if (allUsers.isEmpty()) {
            var user = new UserData();
            user.setName("test_user");
            entityManager.persist(user);
            return user;
        }
        return allUsers.iterator().next();
    }

    @GetMapping("/api/-/user/logout")
    @Transactional
    public RedirectView logout(@CookieValue(name = "sessionid", required = false) String sessionId,
                              HttpServletResponse response) {
        if (sessionId != null) {
            var session = repositories.findUserSession(sessionId);
            if (session != null) {
                entityManager.remove(session);
            }
            response.addCookie(createSessionCookie(sessionId, 0));
        }
        addAccessControlHeaders(response);
        return new RedirectView(getRedirectUrl());
    }

    private void addAccessControlHeaders(HttpServletResponse response) {
        if (!Strings.isNullOrEmpty(webuiUrl)) {
            response.addHeader("Access-Control-Allow-Origin", webuiUrl);
            response.addHeader("Access-Control-Allow-Credentials", "true");
            response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        }
    }

    private Cookie createSessionCookie(String sessionId, int maxAge) {
        var cookie = new Cookie("sessionid", sessionId);
        cookie.setDomain(getDomain());
        cookie.setPath(getPath());
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    private String getDomain() {
        try {
            var uri = new URI(serverUrl);
            return uri.getHost();
        } catch (URISyntaxException exc) {
            throw new RuntimeException(exc);
        }
    }

    private String getPath() {
        try {
            var uri = new URI(serverUrl);
            var path = uri.getRawPath();
            if (Strings.isNullOrEmpty(path))
                return "/";
            return path;
        } catch (URISyntaxException exc) {
            throw new RuntimeException(exc);
        }
    }

    private String getRedirectUrl() {
        if (webuiUrl != null)
            return webuiUrl;
        return "/";
    }

    private void updateLastUsed(UserSession session) {
        session.setLastUsed(LocalDateTime.now(ZoneId.of("UTC")));
    }

}