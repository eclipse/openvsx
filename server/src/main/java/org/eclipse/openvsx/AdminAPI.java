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

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.StatsJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.SemanticVersion;
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
    UserService users;

    @Autowired
    EntityManager entityManager;

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
                .collect(Collectors.joining("\n"));
    }

    private String toString(PersistedLog log) {
        return log.getTimestamp() + "\t" + log.getUser().getLoginName() + "\t" + log.getMessage();
    }

    @PostMapping(
        path = "/admin/namespace-member",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
    public ResultJson addNamespaceMember(@RequestParam("token") String tokenValue,
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
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            return ResultJson.error("Namespace not found: " + namespaceName);
        }
        if (Strings.isNullOrEmpty(provider)) {
            provider = "github";
        }
        var user = repositories.findUserByLoginName(provider, userName);
        if (user == null) {
            return ResultJson.error("User not found: " + provider + "/" + userName);
        }

        if (Strings.isNullOrEmpty(role)) {
            return removeNamespaceMember(namespace, user, token.getUser());
        } else {
            if (!(role.equals(NamespaceMembership.ROLE_OWNER)
                    || role.equals(NamespaceMembership.ROLE_CONTRIBUTOR))) {
                return ResultJson.error("Invalid role: " + role);
            }
            return addNamespaceMember(namespace, user, role, token.getUser());
        }
    }

    public ResultJson removeNamespaceMember(Namespace namespace, UserData user, UserData admin) {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            return ResultJson.error("User " + user.getLoginName() + " is not a member of " + namespace.getName());
        }
        entityManager.remove(membership);
        return logAdminAction(admin, "Removed " + user.getLoginName() + " from namespace " + namespace.getName());
    }

    public ResultJson addNamespaceMember(Namespace namespace, UserData user, String role, UserData admin) {
        var membership = repositories.findMembership(user, namespace);
        if (membership != null) {
            membership.setRole(role);
            return logAdminAction(admin, "Changed role of " + user.getLoginName() + " in " + namespace.getName() + " to " + role);
        }
        membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        membership.setRole(role);
        entityManager.persist(membership);
        return logAdminAction(admin, "Added " + user.getLoginName() + " as " + role + " of " + namespace.getName());
    }

    @PostMapping(
        path = "/admin/delete-extension",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Transactional
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
        
        if (Strings.isNullOrEmpty(version)) {
            var extension = repositories.findExtension(extensionName, namespaceName);
            if (extension == null) {
                return ResultJson.error("Extension not found: " + namespaceName + "." + extension);
            }
            return deleteExtension(extension, token.getUser());
        } else {
            var extVersion = repositories.findVersion(version, extensionName, namespaceName);
            if (extVersion == null) {
                return ResultJson.error("Extension not found: " + namespaceName + "." + extensionName + " version " + version);
            }
            return deleteExtension(extVersion, token.getUser());
        }
    }

    public ResultJson deleteExtension(Extension extension, UserData admin) {
        var namespace = extension.getNamespace();
        var bundledRefs = repositories.findBundledExtensionsReference(extension);
        if (!bundledRefs.isEmpty()) {
            return ResultJson.error("Extension " + namespace.getName() + "." + extension.getName()
                    + " is bundled by the following extension packs: "
                    + bundledRefs.stream()
                        .map(ev -> ev.getExtension().getNamespace().getName() + "." + ev.getExtension().getName() + "@" + ev.getVersion())
                        .collect(Collectors.joining(", ")));
        }
        var dependRefs = repositories.findDependenciesReference(extension);
        if (!dependRefs.isEmpty()) {
            return ResultJson.error("The following extensions have a dependency on " + namespace.getName() + "." + extension.getName() + ": "
                    + dependRefs.stream()
                        .map(ev -> ev.getExtension().getNamespace().getName() + "." + ev.getExtension().getName() + "@" + ev.getVersion())
                        .collect(Collectors.joining(", ")));
        }
        for (var extVersion : extension.getVersions()) {
            removeExtensionVersion(extVersion);
        }
        for (var review : repositories.findAllReviews(extension)) {
            entityManager.remove(review);
        }
        entityManager.remove(extension);
        return logAdminAction(admin, "Deleted " + namespace.getName() + "." + extension.getName());
    }

    public ResultJson deleteExtension(ExtensionVersion extVersion, UserData admin) {
        var extension = extVersion.getExtension();
        if (extension.getVersions().size() == 1) {
            return deleteExtension(extension, admin);
        }
        removeExtensionVersion(extVersion);
        if (extVersion.equals(extension.getLatest())) {
            var versions = extension.getVersions();
            versions.remove(extVersion);
            extension.setLatest(getLatestVersion(versions));
        }
        return logAdminAction(admin, "Deleted " + extension.getNamespace().getName() + "." + extension.getName() + " version " + extVersion.getVersion());
    }

    private void removeExtensionVersion(ExtensionVersion extVersion) {
        var binary = repositories.findBinary(extVersion);
        if (binary != null)
            entityManager.remove(binary);
        var icon = repositories.findIcon(extVersion);
        if (icon != null)
            entityManager.remove(icon);
        var license = repositories.findLicense(extVersion);
        if (license != null)
            entityManager.remove(license);
        var readme = repositories.findReadme(extVersion);
        if (readme != null)
            entityManager.remove(readme);
        entityManager.remove(extVersion);
    }

    private ExtensionVersion getLatestVersion(Iterable<ExtensionVersion> versions) {
        ExtensionVersion latest = null;
        SemanticVersion latestSemver = null;
        for (var extVer : versions) {
            var semver = new SemanticVersion(extVer.getVersion());
            if (latestSemver == null || latestSemver.compareTo(semver) < 0) {
                latest = extVer;
                latestSemver = semver;
            }
        }
        return latest;
    }

    protected ResultJson logAdminAction(UserData admin, String message) {
        var log = new PersistedLog();
        log.setUser(admin);
        log.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        log.setMessage(message);
        entityManager.persist(log);
        return ResultJson.success(message);
    }

}