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

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import java.net.URI;

import javax.persistence.EntityManager;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.NamespaceMembershipListJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.StatsJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    SearchService search;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

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
                var now = TimeUtil.getCurrentUTC();
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

        var result = ResultJson.success("Updated search index");
        admins.logAdminAction(token.getUser(), result);
        return result;
    }

    @PostMapping(
        path = "/admin/{namespaceName}/change-member",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> editNamespaceMember(@PathVariable String namespaceName,
                                         @RequestParam("user") String userName,
                                         @RequestParam(required = false) String provider,
                                         @RequestParam String role) {
        try {
            var user = admins.checkAdminUser();
            return new ResponseEntity<>(admins.editNamespaceMember(namespaceName, userName, provider, role, user), HttpStatus.OK);
        } catch (ErrorResultException exc) {
            return getErrorResponse(exc);
        }
    }

    @PostMapping(
        path = "/admin/{namespaceName}/delete-extension",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> deleteExtension(@PathVariable String namespaceName,
                                      @RequestParam("extension") String extensionName,
                                      @RequestParam(required = false) String version) {
        try {
            var user = admins.checkAdminUser();
            return new ResponseEntity<ResultJson>(admins.deleteExtension(namespaceName, extensionName, version, user), HttpStatus.OK);
        } catch (ErrorResultException exc) {
            return getErrorResponse(exc);
        }
    }

    @GetMapping(path = "/admin/{namespaceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NamespaceJson> getNamespace(@PathVariable String namespaceName) {
        try {
            admins.checkAdminUser();
            var namespace = admins.getNamespace(namespaceName);
            return new ResponseEntity<>(namespace, HttpStatus.OK);
        } catch (ErrorResultException exc) {
            var err = getErrorResponse(exc);
            return new ResponseEntity<>(NamespaceJson.error(err.getBody().error), err.getStatusCode());
        }
    }

    @GetMapping(path = "/admin/{namespaceName}/members", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NamespaceMembershipListJson> getNamespaceMembers(@PathVariable String namespaceName) {
        try{
            admins.checkAdminUser();
            var namespace = repositories.findNamespace(namespaceName);
            var memberships = repositories.findMemberships(namespace);
            var membershipList = new NamespaceMembershipListJson();
            membershipList.namespaceMemberships = memberships.map(membership -> membership.toJson()).toList();
            return new ResponseEntity<>(membershipList, HttpStatus.OK);
        } catch (ErrorResultException exc) {
            var err = getErrorResponse(exc);
            return new ResponseEntity<>(NamespaceMembershipListJson.error(err.getBody().error), err.getStatusCode());
        }
    }

    @PostMapping(
        path = "/admin/-/create-namespace",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> createNamespace(@RequestBody NamespaceJson namespace) {
        try {
            admins.checkAdminUser();
            var json = admins.createNamespace(namespace);
            var serverUrl = UrlUtil.getBaseUrl();
            var url = UrlUtil.createApiUrl(serverUrl, "admin", namespace.name);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(url))
                    .body(json);
        } catch (ErrorResultException exc) {
            return getErrorResponse(exc);
        }
    }

    private ResponseEntity<ResultJson> getErrorResponse(ErrorResultException exc) {
            var json = ResultJson.error(exc.getMessage());
            var status = exc.getStatus() != null ? exc.getStatus() : HttpStatus.BAD_REQUEST;
            return new ResponseEntity<>(json, status);
    }
    
}