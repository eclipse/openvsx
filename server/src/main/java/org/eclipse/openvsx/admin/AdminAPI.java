/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.admin;

import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URI;

import com.fasterxml.jackson.databind.JsonNode;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.entities.AdminStatistics;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.*;
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

import static org.eclipse.openvsx.entities.UserData.ROLE_ADMIN;

@RestController
public class AdminAPI {

    @Autowired
    RepositoryService repositories;

    @Autowired
    VersionService versions;

    @Autowired
    AdminService admins;

    @Autowired
    LocalRegistryService local;

    @Autowired
    SearchUtilService search;

    @GetMapping(
            path = "/admin/report",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AdminStatisticsJson> getReportJson(
            @RequestParam("token") String tokenValue,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        try {
            var statistics = getReport(tokenValue, year, month);
            return ResponseEntity.ok(statistics.toJson());
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(AdminStatisticsJson.class);
        }
    }

    @GetMapping(
            path = "/admin/report",
            produces = "text/csv"
    )
    public ResponseEntity<String> getReportCsv(
            @RequestParam("token") String tokenValue,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        try {
            var statistics = getReport(tokenValue, year, month);
            return ResponseEntity.ok(statistics.toCsv());
        } catch (ErrorResultException exc) {
            return ResponseEntity.status(exc.getStatus()).body(exc.getMessage());
        }
    }

    private void validateToken(String tokenValue) {
        var accessToken = repositories.findAccessToken(tokenValue);
        if(accessToken == null || !accessToken.isActive() || accessToken.getUser() == null || !ROLE_ADMIN.equals(accessToken.getUser().getRole())) {
            throw new ErrorResultException("Invalid access token", HttpStatus.FORBIDDEN);
        }
    }

    private AdminStatistics getReport(String tokenValue, int year, int month) {
        validateToken(tokenValue);
        return admins.getAdminStatistics(year, month);
    }

    @GetMapping(
        path = "/admin/stats",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StatsJson> getStats() {
        try {
            admins.checkAdminUser();

            var json = new StatsJson();
            json.userCount = repositories.countUsers();
            json.extensionCount = repositories.countExtensions();
            json.namespaceCount = repositories.countNamespaces();
            return ResponseEntity.ok(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(StatsJson.class);
        }
    }

    @GetMapping(
        path = "/admin/log",
        produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String getLog(@RequestParam(name = "period", required = false) String periodString) {
        try {
            admins.checkAdminUser();

            Streamable<PersistedLog> logs;
            if (StringUtils.isEmpty(periodString)) {
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
        } catch (ErrorResultException exc) {
            var status = exc.getStatus() != null ? exc.getStatus() : HttpStatus.BAD_REQUEST;
            throw new ResponseStatusException(status);
        }
    }

    private String toString(PersistedLog log) {
        var timestamp = log.getTimestamp().minusNanos(log.getTimestamp().getNano());
        return timestamp + "\t" + log.getUser().getLoginName() + "\t" + log.getMessage();
    }

    @PostMapping(
        path = "/admin/update-search-index",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> updateSearchIndex() {
        try {
            var adminUser = admins.checkAdminUser();

            search.updateSearchIndex(true);

            var result = ResultJson.success("Updated search index");
            admins.logAdminAction(adminUser, result);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(
        path = "/admin/extension/{namespaceName}/{extensionName}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ExtensionJson> getExtension(@PathVariable String namespaceName,
                                                      @PathVariable String extensionName) {
        try {
            admins.checkAdminUser();

            var extension = repositories.findExtension(extensionName, namespaceName);
            if (extension == null) {
                var json = ExtensionJson.error("Extension not found: " + NamingUtil.toExtensionId(namespaceName, extensionName));
                return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
            }

            ExtensionJson json;
            var latest = versions.getLatestTrxn(extension, null, false, false);
            if (latest == null) {
                json = new ExtensionJson();
                json.namespace = extension.getNamespace().getName();
                json.name = extension.getName();
                json.allVersions = Collections.emptyMap();
                json.allTargetPlatformVersions = Collections.emptyMap();
            } else {
                json = local.toExtensionVersionJson(latest, null, false);
                json.allTargetPlatformVersions = versions.getVersionsTrxn(extension).stream()
                        .collect(Collectors.groupingBy(ExtensionVersion::getVersion, Collectors.mapping(ExtensionVersion::getTargetPlatform, Collectors.toList())));
            }
            json.active = extension.isActive();
            return ResponseEntity.ok(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(ExtensionJson.class);
        }
    }

    @PostMapping(
        path = "/admin/extension/{namespaceName}/{extensionName}/delete",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> deleteExtension(@PathVariable String namespaceName,
                                                      @PathVariable String extensionName,
                                                      @RequestBody(required = false) List<TargetPlatformVersionJson> targetVersions) {
        try {
            ResultJson result;
            var adminUser = admins.checkAdminUser();
            if(targetVersions == null) {
                result = admins.deleteExtension(namespaceName, extensionName, adminUser);
            } else {
                var results = new ArrayList<ResultJson>();
                for(var targetVersion : targetVersions) {
                    results.add(admins.deleteExtension(namespaceName, extensionName, targetVersion.targetPlatform, targetVersion.version, adminUser));
                }

                result = new ResultJson();
                result.error = results.stream().map(r -> r.error).filter(Objects::nonNull).collect(Collectors.joining("\n"));
                result.success = results.stream().map(r -> r.success).filter(Objects::nonNull).collect(Collectors.joining("\n"));
            }

            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(
        path = "/admin/namespace/{namespaceName}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NamespaceJson> getNamespace(@PathVariable String namespaceName) {
        try {
            admins.checkAdminUser();

            var namespace = local.getNamespace(namespaceName);
            var serverUrl = UrlUtil.getBaseUrl();
            namespace.membersUrl = UrlUtil.createApiUrl(serverUrl, "admin", "namespace", namespace.name, "members");
            namespace.roleUrl = UrlUtil.createApiUrl(serverUrl, "admin", "namespace", namespace.name, "change-member");
            return ResponseEntity.ok(namespace);
        } catch (NotFoundException exc) {
            var json = NamespaceJson.error("Namespace not found: " + namespaceName);
            return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(NamespaceJson.class);
        }
    }

    @PostMapping(
        path = "/admin/create-namespace",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> createNamespace(@RequestBody NamespaceJson namespace) {
        try {
            admins.checkAdminUser();
            var json = admins.createNamespace(namespace);
            var serverUrl = UrlUtil.getBaseUrl();
            var url = UrlUtil.createApiUrl(serverUrl, "admin", "namespace", namespace.name);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(url))
                    .body(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @PostMapping(
            path = "/admin/change-namespace",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> changeNamespace(@RequestBody ChangeNamespaceJson json) {
        try {
            admins.checkAdminUser();
            admins.changeNamespace(json);
            return ResponseEntity.ok(ResultJson.success("Scheduled namespace change from '" + json.oldNamespace + "' to '" + json.newNamespace + "'.\nIt can take 15 minutes to a couple hours for the change to become visible."));
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(
        path = "/admin/namespace/{namespaceName}/members",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<NamespaceMembershipListJson> getNamespaceMembers(@PathVariable String namespaceName) {
        try{
            admins.checkAdminUser();
            var namespace = repositories.findNamespace(namespaceName);
            var memberships = repositories.findMemberships(namespace);
            var membershipList = new NamespaceMembershipListJson();
            membershipList.namespaceMemberships = memberships.map(membership -> membership.toJson()).toList();
            return ResponseEntity.ok(membershipList);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(NamespaceMembershipListJson.class);
        }
    }

    @PostMapping(
        path = "/admin/namespace/{namespaceName}/change-member",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> editNamespaceMember(@PathVariable String namespaceName,
                                                          @RequestParam("user") String userName,
                                                          @RequestParam(required = false) String provider,
                                                          @RequestParam String role) {
        try {
            var adminUser = admins.checkAdminUser();
            var result = admins.editNamespaceMember(namespaceName, userName, provider, role, adminUser);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }

    @GetMapping(
        path = "/admin/publisher/{provider}/{loginName}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserPublishInfoJson> getUserPublishInfo(@PathVariable String provider, @PathVariable String loginName) {
        try {
            admins.checkAdminUser();
            var userPublishInfo = admins.getUserPublishInfo(provider, loginName);
            return ResponseEntity.ok(userPublishInfo);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(UserPublishInfoJson.class);
        }
    }

    @PostMapping(
        path = "/admin/publisher/{provider}/{loginName}/revoke",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResultJson> revokePublisherContributions(@PathVariable String loginName, @PathVariable String provider) {
        try {
            var adminUser = admins.checkAdminUser();
            var result = admins.revokePublisherContributions(provider, loginName, adminUser);
            return ResponseEntity.ok(result);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
        }
    }
    
}