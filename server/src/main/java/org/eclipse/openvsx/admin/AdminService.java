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

import static org.eclipse.openvsx.entities.FileResource.CHANGELOG;
import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.ICON;
import static org.eclipse.openvsx.entities.FileResource.LICENSE;
import static org.eclipse.openvsx.entities.FileResource.MANIFEST;
import static org.eclipse.openvsx.entities.FileResource.README;
import static org.eclipse.openvsx.entities.FileResource.VSIXMANIFEST;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.accesstoken.AccessTokenService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.AdminStatistics;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ChangeNamespaceJson;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.UserRelationshipsJson;
import org.eclipse.openvsx.json.UserPublishInfoJson;
import org.eclipse.openvsx.mail.MailService;
import org.eclipse.openvsx.migration.HandlerJobRequest;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Component
public class AdminService {

    private final RepositoryService repositories;
    private final ExtensionService extensions;
    private final EntityManager entityManager;
    private final UserService users;
    private final AccessTokenService tokens;
    private final ExtensionValidator validator;
    private final SearchUtilService search;
    private final EclipseService eclipse;
    private final StorageUtilService storageUtil;
    private final CacheService cache;
    private final JobRequestScheduler scheduler;
    private final MailService mail;
    private final LogService logs;

    public AdminService(
            RepositoryService repositories,
            ExtensionService extensions,
            EntityManager entityManager,
            UserService users,
            AccessTokenService tokens,
            ExtensionValidator validator,
            SearchUtilService search,
            EclipseService eclipse,
            StorageUtilService storageUtil,
            CacheService cache,
            JobRequestScheduler scheduler,
            MailService mail,
            LogService logs
    ) {
        this.repositories = repositories;
        this.extensions = extensions;
        this.entityManager = entityManager;
        this.users = users;
        this.tokens = tokens;
        this.validator = validator;
        this.search = search;
        this.eclipse = eclipse;
        this.storageUtil = storageUtil;
        this.cache = cache;
        this.scheduler = scheduler;
        this.mail = mail;
        this.logs = logs;
    }

    @EventListener
    public void applicationStarted(ApplicationStartedEvent event) {
        var jobRequest = new HandlerJobRequest<>(MonthlyAdminStatisticsJobRequestHandler.class);
        scheduler.scheduleRecurrently("MonthlyAdminStatistics", Cron.monthly(1, 0, 3), ZoneId.of("UTC"), jobRequest);
    }

    /**
     * Deletes the given extension together with all bundled or dependent extensions.
     * <p>
     * No further checks are made if the extension is referenced by bundles or as a dependency.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public void deleteExtensionAndDependencies(UserData admin, String namespaceName, String extensionName) throws ErrorResultException {
        var extension = extensions.lockExtension(namespaceName, extensionName);
        deleteExtensionAndDependencies(admin, extension, 0);
    }

    private void deleteExtensionAndDependencies(UserData admin, Extension extension, int depth) throws ErrorResultException {
        if (depth > 5) {
            throw new ErrorResultException(
                    "Failed to delete extension and its dependencies. Exceeded maximum recursion depth.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        var bundledRefs = repositories.findBundledExtensionsReference(extension);
        for (var bundledRef : bundledRefs) {
            deleteExtensionAndDependencies(admin, bundledRef, depth);
        }

        var dependRefs = repositories.findDependenciesReference(extension);
        for (var dependRef : dependRefs) {
            deleteExtensionAndDependencies(admin, dependRef, depth);
        }

        // We unconditionally delete the extension,
        // not checking if there are dependencies on this extension.
        extensions.deleteExtension(admin, extension, false);
    }

    private void deleteExtensionAndDependencies(UserData admin, ExtensionVersion extVersion, int depth) {
        var extension = extVersion.getExtension();
        if (repositories.countVersions(extension.getNamespace().getName(), extension.getName()) == 1) {
            deleteExtensionAndDependencies(admin, extension, depth + 1);
            return;
        }

        extensions.removeExtensionVersion(extVersion);
        extension.getVersions().remove(extVersion);
        extensions.updateExtension(extension);
        logs.logAction(admin, ResultJson.success("Deleted " + NamingUtil.toLogFormat(extVersion)));
    }

    /**
     * Deletes the given extension unconditionally. No further checks are made if the extension
     * is referenced by bundles or as a dependency.
     * <p>
     * This method is intended for non-user interaction as it will wait till the lock can be acquired.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public void deleteExtension(UserData admin, String namespaceName, String extensionName) throws ErrorResultException {
        var extension = extensions.lockExtension(namespaceName, extensionName);
        extensions.deleteExtension(admin, extension, false);
    }

    /**
     * Delete the provided versions of an extension. If all versions of the extension shall be deleted, the
     * extension as a whole will be removed unless it is referenced by bundles or used as a dependency.
     * <p>
     * The method will try to lock the extension and fail with an {@code ErrorResultException} if it can't acquire it.
     * <p>
     * This method is intended to be used for user interaction as it can fail when the extension is concurrently updated.
     */
    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtensionNoWait(
        UserData user,
        String namespaceName,
        String extensionName,
        TargetPlatformVersion... targetVersions
    ) throws ErrorResultException {
        return extensions.deleteExtension(user, false, namespaceName, extensionName, targetVersions);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteNamespace(String namespaceName, UserData admin) throws ErrorResultException {
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new NotFoundException();
        }
        return deleteNamespace(namespace, admin);
    }

    private ResultJson deleteNamespace(Namespace namespace, UserData admin) {
        var namespaceExtensions = repositories.findExtensions(namespace);
        if (!namespaceExtensions.isEmpty()) {
            throw new ErrorResultException("Cannot delete namespaces that contain extensions.", HttpStatus.BAD_REQUEST);
        }

        var memberships = repositories.findMemberships(namespace);
        for (var membership : memberships) {
            entityManager.remove(membership);
        }

        if (namespace.getLogoStorageType() != null) {
            try {
                storageUtil.removeNamespaceLogo(namespace);
            } catch (RuntimeException exc) {
                throw new ErrorResultException("Failed to delete namespace icon: " + exc.getMessage());
            }
        }

        entityManager.remove(namespace);

        // Clear cache for the namespace
        cache.evictNamespaceDetails(namespace);

        var result = ResultJson.success("Deleted namespace " + namespace.getName());
        logs.logAction(admin, result);
        return result;
    }

    private String userNotFoundMessage(String user) {
        return "User not found: " + user;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteReview(String namespace, String extensionName, String loginName, String provider) {
        var extension = repositories.findExtension(extensionName, namespace);
        if (extension == null || !extension.isActive()) {
            var message = "Extension not found: " + NamingUtil.toExtensionId(namespace, extensionName);
            throw new ErrorResultException(message, HttpStatus.NOT_FOUND);
        }

        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException(userNotFoundMessage(provider + "/" + loginName), HttpStatus.NOT_FOUND);
        }

        var reviews = repositories.findActiveReviews(extension, user);
        if (reviews.isEmpty()) {
            var message = "No active review for extension " + NamingUtil.toExtensionId(extension) + " and user " + loginName + " found";
            throw new ErrorResultException(message, HttpStatus.NOT_FOUND);
        }

        for (var extReview : reviews) {
            deleteReview(extReview);
        }

        return ResultJson.success("Deleted review from " + loginName + " for " + NamingUtil.toExtensionId(extension));
    }

    private void deleteReview(ExtensionReview review) {
        entityManager.remove(review);

        var extension = review.getExtension();
        extension.setAverageRating(repositories.getAverageReviewRating(extension));
        extension.setReviewCount(repositories.countActiveReviews(extension));
        search.updateSearchEntry(extension);
        cache.evictExtensionJsons(extension);
        cache.evictLatestExtensionVersion(extension);
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
            throw new ErrorResultException(userNotFoundMessage(provider + "/" + userName));
        }

        var result = role.equals("remove")
                ? users.removeNamespaceMember(namespace, user)
                : users.addNamespaceMember(namespace, user, role);

        search.updateSearchEntries(repositories.findActiveExtensions(namespace).toList());
        logs.logAction(admin, result);
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson createNamespace(NamespaceJson json) {
        var namespaceIssue = validator.validateNamespace(json.getName());
        if (namespaceIssue.isPresent()) {
            throw new ErrorResultException(namespaceIssue.get().toString());
        }

        var namespaceName = repositories.findNamespaceName(json.getName());
        if (namespaceName != null) {
            throw new ErrorResultException("Namespace already exists: " + namespaceName);
        }
        var namespace = new Namespace();
        namespace.setName(json.getName());
        entityManager.persist(namespace);
        return ResultJson.success("Created namespace " + namespace.getName());
    }

    public void changeNamespace(ChangeNamespaceJson json) {
        if (StringUtils.isEmpty(json.oldNamespace())) {
            throw new ErrorResultException("Old namespace must have a value");
        }
        if (StringUtils.isEmpty(json.newNamespace())) {
            throw new ErrorResultException("New namespace must have a value");
        }

        var oldNamespace = repositories.findNamespace(json.oldNamespace());
        if (oldNamespace == null) {
            throw new ErrorResultException("Old namespace doesn't exists: " + json.oldNamespace());
        }

        var newNamespace = repositories.findNamespace(json.newNamespace());
        if (newNamespace != null && !json.mergeIfNewNamespaceAlreadyExists()) {
            throw new ErrorResultException("New namespace already exists: " + json.newNamespace());
        }
        if (newNamespace != null) {
            var newExtensions = repositories.findExtensions(newNamespace).stream()
                    .collect(Collectors.toMap(Extension::getName, e -> e));
            var oldExtensions = repositories.findExtensions(oldNamespace).stream()
                    .collect(Collectors.toMap(Extension::getName, e -> e));

            var duplicateExtensions = oldExtensions.keySet().stream()
                    .filter(newExtensions::containsKey)
                    .collect(Collectors.joining("','"));
            if (!duplicateExtensions.isEmpty()) {
                var message = "Can't merge namespaces, because new namespace '" +
                        json.newNamespace() +
                        "' and old namespace '" +
                        json.oldNamespace() +
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
            throw new ErrorResultException(userNotFoundMessage(loginName), HttpStatus.NOT_FOUND);
        }

        var userPublishInfo = new UserPublishInfoJson();
        var userJson = user.toUserJson();
        userJson.setRole(user.getRoleAsString());
        userPublishInfo.setUser(userJson);
        eclipse.adminEnrichUserJson(userPublishInfo.getUser(), user);
        userPublishInfo.setActiveAccessTokenNum((int) repositories.countActiveAccessTokens(user));
        var extVersions = repositories.findLatestVersions(user);
        var types = new String[]{DOWNLOAD, MANIFEST, ICON, README, LICENSE, CHANGELOG, VSIXMANIFEST};
        var fileUrls = storageUtil.getFileUrls(extVersions, UrlUtil.getBaseUrl(), types);
        userPublishInfo.setExtensions(extVersions.stream()
                .map(latest -> {
                    var json = latest.toExtensionJson();
                    json.setPreview(latest.isPreview());
                    json.setActive(latest.getExtension().isActive());
                    json.setFiles(fileUrls.get(latest.getId()));

                    return json;
                })
                .sorted(Comparator.<ExtensionJson, String>comparing(ExtensionJson::getNamespace)
                                .thenComparing(ExtensionJson::getName)
                                .thenComparing(ExtensionJson::getVersion)
                )
                .toList());

        return userPublishInfo;
    }

    @Transactional
    public Page<UserRelationshipsJson> searchUsers(String search, String role, Pageable pageable) {
        return repositories.searchUsers(search, role, pageable)
                .map(user -> {
                    var json = new UserRelationshipsJson();
                    var userJson = user.toUserJson();
                    userJson.setRole(user.getRoleAsString());
                    json.setUser(userJson);
                    json.setNamespaces(repositories.findMemberships(user).stream()
                            .map(membership -> membership.getNamespace().toNamespaceDetailsJson())
                            .toList());
                    return json;
                });
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson updateUserRole(String provider, String loginName, String role, UserData admin) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException(userNotFoundMessage(provider + "/" + loginName), HttpStatus.NOT_FOUND);
        }

        var updatedRole = "none".equalsIgnoreCase(role) ? null : parseRole(role);
        if (Objects.equals(user.getRole(), updatedRole)) {
            throw new ErrorResultException("User " + provider + "/" + loginName + " already has the role " + user.getRole() + ".");
        }

        user.setRole(updatedRole);
        var message = updatedRole == null
                ? "Removed role from user " + provider + "/" + loginName + "."
                : "Updated role for user " + provider + "/" + loginName + " to " + updatedRole + ".";
        var result = ResultJson.success(message);
        logs.logAction(admin, result);
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson revokePublisherContributions(String provider, String loginName, UserData admin) {
        return revokePublisherContributions(provider, loginName, admin, null);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson revokePublisherContributions(String provider, String loginName, UserData admin, String reason) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException(userNotFoundMessage(loginName), HttpStatus.NOT_FOUND);
        }

        // Send a DELETE request to the Eclipse publisher agreement API
        if (eclipse.isActive() && user.getEclipsePersonId() != null) {
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
        }

        var versions = repositories.findVersionsByUser(user, true);
        for (var version : versions) {
            // Deactivate all published extension versions
            version.setActive(false);
            affectedExtensions.add(version.getExtension());
            deactivatedExtensionCount++;
        }

        // Update affected extensions
        for (var extension : affectedExtensions) {
            extensions.updateExtension(extension);
        }

        // revoke namespace memberships
        var namespaceMemberships = repositories.findMemberships(user);
        var numberOfNamespaceMemberships = 0L;
        // add a null check due to tests using mocks which return null
        if (namespaceMemberships != null) {
            numberOfNamespaceMemberships = namespaceMemberships.stream().count();
            repositories.deleteMemberships(user);
        }

        var message = "Deactivated " + deactivatedTokenCount + " tokens, "
            + "deactivated " + deactivatedExtensionCount + " extensions, "
            + "removed " + numberOfNamespaceMemberships + " namespace memberships of user "
            + provider + "/" + loginName + ".";

        if (reason != null) {
            message += " Reason: " + reason;
        }
        var result = ResultJson.success(message);
        logs.logAction(admin, result);
        return result;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson revokePublisherTokens(String provider, String loginName, UserData admin) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException(userNotFoundMessage(loginName), HttpStatus.NOT_FOUND);
        }

        var deactivatedTokenCount = repositories.deactivateAccessTokens(user);
        var result = ResultJson.success("Deactivated " + deactivatedTokenCount + " tokens of user " + provider + "/" + loginName + ".");
        logs.logAction(admin, result);
        mail.scheduleRevokedAccessTokensMail(user);
        return result;
    }

    public UserData checkAdminUser() {
        return checkAdminUser(users.findLoggedInUser());
    }

    public UserData checkAdminUser(String tokenValue) {
        var user = Optional.of(tokenValue)
                .map(tokens::useAccessToken)
                .map(PersonalAccessToken::getUser)
                .orElse(null);

        return checkAdminUser(user);
    }

    private UserData checkAdminUser(UserData user) {
        if (user == null || !UserData.Role.ADMIN.equals(user.getRole())) {
            throw new ErrorResultException("Administration role is required.", HttpStatus.FORBIDDEN);
        }
        return user;
    }

    private UserData.Role parseRole(String role) {
        try {
            return UserData.Role.valueOfIgnoreCase(role);
        } catch (IllegalArgumentException ignored) {
            throw new ErrorResultException("Invalid role: " + role, HttpStatus.BAD_REQUEST);
        }
    }

    public AdminStatistics getAdminStatistics(int year, int month) throws ErrorResultException {
        validateYearAndMonth(year, month);
        var statistics = repositories.findAdminStatisticsByYearAndMonth(year, month);
        if (statistics == null) {
            throw new NotFoundException();
        }

        return statistics;
    }

    private void validateYearAndMonth(int year, int month) {
        if (year < 0) {
            throw new ErrorResultException("Year can't be negative", HttpStatus.BAD_REQUEST);
        }
        if (month < 1 || month > 12) {
            throw new ErrorResultException("Month must be a value between 1 and 12", HttpStatus.BAD_REQUEST);
        }

        var now = TimeUtil.getCurrentUTC();
        if (year > now.getYear() || (year == now.getYear() && month >= now.getMonthValue())) {
            throw new ErrorResultException("Combination of year and month lies in the future", HttpStatus.BAD_REQUEST);
        }
    }
}
