/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.StatsJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AdminAPI {

    @Autowired
    RepositoryService repositories;

    @Autowired
    AdminService admins;

    @Autowired
    UserService users;

    @Autowired
    SearchService search;

    @GetMapping(
        path = "/admin/stats",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public StatsJson getStats(@RequestParam("token") String tokenValue) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            return StatsJson.error("Invalid access token.");
        }
        if (!UserData.ROLE_ADMIN.equals(token.getUser().getRole())) {
            return StatsJson.error("Administration role is required.");
        }
        var json = new StatsJson();
        json.userCount = repositories.countUsers();
        json.extensionCount = repositories.countExtensions();
        json.namespaceCount = repositories.countNamespaces();
        return json;
    }

    @GetMapping(
        path = "/admin/log",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String getLog(@RequestParam("token") String tokenValue,
                         @RequestParam(name = "period", required = false) String periodString) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid access token.");
        }
        if (!UserData.ROLE_ADMIN.equals(token.getUser().getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration role is required.");
        }

        Streamable<PersistedLog> logs;
        if (Strings.isNullOrEmpty(periodString)) {
            logs = repositories.findAllPersistedLogs();
        } else {
            try {
                var period = Period.parse(periodString);
                var now = LocalDateTime.now(ZoneId.of("UTC"));
                logs = repositories.findPersistedLogsAfter(now.minus(period));
            } catch (DateTimeParseException exc) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid period");
            }
        }
        return logs.stream()
                .map(this::toString)
                .collect(Collectors.joining("\n")) + "\n";
    }

    private String toString(PersistedLog log) {
        var timestamp = log.getTimestamp().minusNanos(log.getTimestamp().getNano());
        return timestamp + "\t" + log.getUser().getLoginName() + "\t" + log.getMessage();
    }

    @PostMapping(
        path = "/admin/update-search-index",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResultJson updateSearchIndex(@RequestParam("token") String tokenValue) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            return ResultJson.error("Invalid access token.");
        }
        if (!UserData.ROLE_ADMIN.equals(token.getUser().getRole())) {
            return ResultJson.error("Administration role is required.");
        }

        search.updateSearchIndex();
        return admins.logAdminAction(token.getUser(), "Updated search index");
    }

    @PostMapping(
        path = "/admin/namespace-member",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResultJson editNamespaceMember(@RequestParam("token") String tokenValue,
                                         @RequestParam("namespace") String namespaceName,
                                         @RequestParam("user") String userName,
                                         @RequestParam(required = false) String provider,
                                         @RequestParam(required = false) String role) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            return ResultJson.error("Invalid access token.");
        }
        if (!UserData.ROLE_ADMIN.equals(token.getUser().getRole())) {
            return ResultJson.error("Administration role is required.");
        }
        try {
            return users.editNamespaceMember(namespaceName, userName, provider, role, token.getUser());
        } catch (ErrorResultException exc) {
            return ResultJson.error(exc.getMessage());
        }
    }

    @PostMapping(
        path = "/admin/delete-extension",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResultJson deleteExtension(@RequestParam("token") String tokenValue,
                                      @RequestParam("namespace") String namespaceName,
                                      @RequestParam("extension") String extensionName,
                                      @RequestParam(required = false) String version) {
        var token = users.useAccessToken(tokenValue);
        if (token == null) {
            return ResultJson.error("Invalid access token.");
        }
        if (!UserData.ROLE_ADMIN.equals(token.getUser().getRole())) {
            return ResultJson.error("Administration role is required.");
        }
        try {
            return admins.deleteExtension(namespaceName, extensionName, version, token.getUser());
        } catch (ErrorResultException exc) {
            return ResultJson.error(exc.getMessage());
        }
    }

}