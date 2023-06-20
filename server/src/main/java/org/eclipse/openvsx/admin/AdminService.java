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

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.openvsx.entities.FileResource.*;

@Component
public class AdminService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    ExtensionService extensions;

    @Autowired
    VersionService versions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

    @Autowired
    ExtensionValidator validator;

    @Autowired
    SearchUtilService search;

    @Autowired
    EclipseService eclipse;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    CacheService cache;

    @Autowired
    JobRequestScheduler scheduler;

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        var jobRequest = new HandlerJobRequest<>(MonthlyAdminStatisticsJobRequestHandler.class);
        scheduler.scheduleRecurrently("MonthlyAdminStatistics", Cron.monthly(1, 0, 3), ZoneId.of("UTC"), jobRequest);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(String namespaceName, String extensionName, UserData admin)
            throws ErrorResultException {
        var extension = repositories.findExtension(extensionName, namespaceName);
        if (extension == null) {
            var extensionId = NamingUtil.toExtensionId(namespaceName, extensionName);
            throw new ErrorResultException("Extension not found: " + extensionId, HttpStatus.NOT_FOUND);
        }
        return deleteExtension(extension, admin);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(String namespaceName, String extensionName, String targetPlatform, String version, UserData admin)
            throws ErrorResultException {
        var extVersion = repositories.findVersion(version, targetPlatform, extensionName, namespaceName);
        if (extVersion == null) {
            var message = "Extension not found: " + NamingUtil.toLogFormat(namespaceName, extensionName, targetPlatform, version);

            throw new ErrorResultException(message, HttpStatus.NOT_FOUND);
        }

        return deleteExtension(extVersion, admin);
    }

    protected ResultJson deleteExtension(Extension extension, UserData admin) throws ErrorResultException {
        var namespace = extension.getNamespace();
        var bundledRefs = repositories.findBundledExtensionsReference(extension);
        if (!bundledRefs.isEmpty()) {
            throw new ErrorResultException("Extension " + NamingUtil.toExtensionId(extension)
                    + " is bundled by the following extension packs: "
                    + bundledRefs.stream()
                        .map(NamingUtil::toFileFormat)
                        .collect(Collectors.joining(", ")));
        }
        var dependRefs = repositories.findDependenciesReference(extension);
        if (!dependRefs.isEmpty()) {
            throw new ErrorResultException("The following extensions have a dependency on " + NamingUtil.toExtensionId(extension) + ": "
                    + dependRefs.stream()
                        .map(NamingUtil::toFileFormat)
                        .collect(Collectors.joining(", ")));
        }

        cache.evictExtensionJsons(extension);
        for (var extVersion : repositories.findVersions(extension)) {
            removeExtensionVersion(extVersion);
        }
        for (var review : repositories.findAllReviews(extension)) {
            entityManager.remove(review);
        }

        entityManager.remove(extension);
        search.removeSearchEntry(extension);

        var result = ResultJson.success("Deleted " + NamingUtil.toExtensionId(extension));
        logAdminAction(admin, result);
        return result;
    }

    protected ResultJson deleteExtension(ExtensionVersion extVersion, UserData admin) {
        var extension = extVersion.getExtension();
        if (repositories.countVersions(extension) == 1) {
            return deleteExtension(extension, admin);
        }

        removeExtensionVersion(extVersion);
        extension.getVersions().remove(extVersion);
        extensions.updateExtension(extension);

        var result = ResultJson.success("Deleted " + NamingUtil.toLogFormat(extVersion));
        logAdminAction(admin, result);
        return result;
    }

    private void removeExtensionVersion(ExtensionVersion extVersion) {
        repositories.findFiles(extVersion).forEach(file -> {
            enqueueRemoveFileJob(file);
            entityManager.remove(file);
        });
        entityManager.remove(extVersion);
    }

    private void enqueueRemoveFileJob(FileResource resource) {
        if(!resource.getStorageType().equals(STORAGE_DB)) {
            scheduler.enqueue(new RemoveFileJobRequest(resource));
        }
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson editNamespaceMember(String namespaceName, String userName, String provider, String role,
            UserData admin) throws ErrorResultException {
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new ErrorResultException("Namespace not found: " + namespaceName);
        }
        var user = repositories.findUserByLoginName(provider, userName);
        if (user == null) {
            throw new ErrorResultException("User not found: " + provider + "/" + userName);
        }

        ResultJson result;
        if (role.equals("remove")) {
            result = users.removeNamespaceMember(namespace, user);
        } else {
            result = users.addNamespaceMember(namespace, user, role);
        }
        for (var extension : repositories.findActiveExtensions(namespace)) {
            search.updateSearchEntry(extension);
        }
        logAdminAction(admin, result);
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson createNamespace(NamespaceJson json) {
        var namespaceIssue = validator.validateNamespace(json.name);
        if (namespaceIssue.isPresent()) {
            throw new ErrorResultException(namespaceIssue.get().toString());
        }

        var namespace = repositories.findNamespace(json.name);
        if (namespace != null) {
            throw new ErrorResultException("Namespace already exists: " + namespace.getName());
        }
        namespace = new Namespace();
        namespace.setName(json.name);
        entityManager.persist(namespace);
        return ResultJson.success("Created namespace " + namespace.getName());
    }

    public void changeNamespace(ChangeNamespaceJson json) {
        if (StringUtils.isEmpty(json.oldNamespace)) {
            throw new ErrorResultException("Old namespace must have a value");
        }
        if (StringUtils.isEmpty(json.newNamespace)) {
            throw new ErrorResultException("New namespace must have a value");
        }

        var oldNamespace = repositories.findNamespace(json.oldNamespace);
        if (oldNamespace == null) {
            throw new ErrorResultException("Old namespace doesn't exists: " + json.oldNamespace);
        }

        var newNamespace = repositories.findNamespace(json.newNamespace);
        if (newNamespace != null && !json.mergeIfNewNamespaceAlreadyExists) {
            throw new ErrorResultException("New namespace already exists: " + json.newNamespace);
        }
        if (newNamespace != null) {
            var newExtensions = repositories.findExtensions(newNamespace).stream()
                    .collect(Collectors.toMap(Extension::getName, e -> e));
            var oldExtensions = repositories.findExtensions(oldNamespace).stream()
                    .collect(Collectors.toMap(Extension::getName, e -> e));

            var duplicateExtensions = oldExtensions.keySet().stream()
                    .filter(newExtensions::containsKey)
                    .collect(Collectors.joining("','"));
            if(!duplicateExtensions.isEmpty()) {
                var message = "Can't merge namespaces, because new namespace '" +
                        json.newNamespace +
                        "' and old namespace '" +
                        json.oldNamespace +
                        "' have " +
                        (duplicateExtensions.indexOf(',') == -1 ? "a " : "") +
                        "duplicate extension" +
                        (duplicateExtensions.indexOf(',') == -1 ? "" : "s") +
                        ": '" +
                        duplicateExtensions +
                        "'.";

                throw new ErrorResultException(message);
            }
        }

        scheduler.enqueue(new ChangeNamespaceJobRequest(json));
    }
    
    public UserPublishInfoJson getUserPublishInfo(String provider, String loginName) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException("User not found: " + loginName, HttpStatus.NOT_FOUND);
        }

        var userPublishInfo = new UserPublishInfoJson();
        userPublishInfo.user = user.toUserJson();
        eclipse.enrichUserJson(userPublishInfo.user, user);
        userPublishInfo.activeAccessTokenNum = (int) repositories.countActiveAccessTokens(user);
        userPublishInfo.extensions = repositories.findExtensions(user).stream()
                .map(e -> versions.getLatestTrxn(e, null, false, false))
                .map(latest -> {
                    var json = latest.toExtensionJson();
                    json.preview = latest.isPreview();
                    json.active = latest.getExtension().isActive();
                    json.files = storageUtil.getFileUrls(latest, UrlUtil.getBaseUrl(),
                            DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG, VSIXMANIFEST);

                    return json;
                })
                .sorted(Comparator.<ExtensionJson, String>comparing(j -> j.namespace)
                                .thenComparing(j -> j.name)
                                .thenComparing(j -> j.version)
                )
                .collect(Collectors.toList());

        return userPublishInfo;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson revokePublisherContributions(String provider, String loginName, UserData admin) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException("User not found: " + loginName, HttpStatus.NOT_FOUND);
        }

        // Send a DELETE request to the Eclipse publisher agreement API
        if (eclipse.isActive() && user.getEclipseData() != null
                && user.getEclipseData().publisherAgreement != null
                && user.getEclipseData().publisherAgreement.isActive) {
            eclipse.revokePublisherAgreement(user, admin);
        }

        var accessTokens = repositories.findAccessTokens(user);
        var affectedExtensions = new LinkedHashSet<Extension>();
        var deactivatedTokenCount = 0;
        var deactivatedExtensionCount = 0;
        for (var accessToken : accessTokens) {
            // Deactivate the user's access tokens
            if (accessToken.isActive()) {
                accessToken.setActive(false);
                deactivatedTokenCount++;
            }

            // Deactivate all published extension versions
            var versions = repositories.findVersionsByAccessToken(accessToken, true);
            for (var version : versions) {
                version.setActive(false);
                affectedExtensions.add(version.getExtension());
                deactivatedExtensionCount++;
            }
        }
        
        // Update affected extensions
        for (var extension : affectedExtensions) {
            extensions.updateExtension(extension);
        }

        var result = ResultJson.success("Deactivated " + deactivatedTokenCount
                + " tokens and deactivated " + deactivatedExtensionCount + " extensions of user "
                + provider + "/" + loginName + "."); 
        logAdminAction(admin, result);
        return result;
    }

    public UserData checkAdminUser() {
        var user = users.findLoggedInUser();
        if (user == null || !UserData.ROLE_ADMIN.equals(user.getRole())) {
            throw new ErrorResultException("Administration role is required.", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    @Transactional
    public void logAdminAction(UserData admin, ResultJson result) {
        if (result.success != null) {
            var log = new PersistedLog();
            log.setUser(admin);
            log.setTimestamp(TimeUtil.getCurrentUTC());
            log.setMessage(result.success);
            entityManager.persist(log);
        }
    }

    public AdminStatistics getAdminStatistics(int year, int month) throws ErrorResultException {
        validateYearAndMonth(year, month);
        var statistics = repositories.findAdminStatisticsByYearAndMonth(year, month);
        if(statistics == null) {
            throw new NotFoundException();
        }

        return statistics;
    }

    private void validateYearAndMonth(int year, int month) {
        if(year < 0) {
            throw new ErrorResultException("Year can't be negative", HttpStatus.BAD_REQUEST);
        }
        if(month < 1 || month > 12) {
            throw new ErrorResultException("Month must be a value between 1 and 12", HttpStatus.BAD_REQUEST);
        }

        var now = TimeUtil.getCurrentUTC();
        if(year > now.getYear() || (year == now.getYear() && month >= now.getMonthValue())) {
            throw new ErrorResultException("Combination of year and month lies in the future", HttpStatus.BAD_REQUEST);
        }
    }
}