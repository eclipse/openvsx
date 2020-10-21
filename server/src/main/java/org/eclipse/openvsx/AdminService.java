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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.UserPublishInfoJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.SemanticVersion;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AdminService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserService users;

    @Autowired
    ExtensionValidator validator;

    @Autowired
    SearchService search;

    @Autowired
    EclipseService eclipse;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(String namespaceName, String extensionName, String version, UserData admin)
            throws ErrorResultException {
        if (Strings.isNullOrEmpty(version)) {
            var extension = repositories.findExtension(extensionName, namespaceName);
            if (extension == null) {
                throw new ErrorResultException("Extension not found: " + namespaceName + "." + extensionName,
                        HttpStatus.NOT_FOUND);
            }
            return deleteExtension(extension, admin);
        } else {
            var extVersion = repositories.findVersion(version, extensionName, namespaceName);
            if (extVersion == null) {
                throw new ErrorResultException("Extension not found: " + namespaceName + "." + extensionName + " version " + version,
                        HttpStatus.NOT_FOUND);
            }
            return deleteExtension(extVersion, admin);
        }
    }

    protected ResultJson deleteExtension(Extension extension, UserData admin) throws ErrorResultException {
        var namespace = extension.getNamespace();
        var bundledRefs = repositories.findBundledExtensionsReference(extension);
        if (!bundledRefs.isEmpty()) {
            throw new ErrorResultException("Extension " + namespace.getName() + "." + extension.getName()
                    + " is bundled by the following extension packs: "
                    + bundledRefs.stream()
                        .map(ev -> ev.getExtension().getNamespace().getName() + "." + ev.getExtension().getName() + "@" + ev.getVersion())
                        .collect(Collectors.joining(", ")));
        }
        var dependRefs = repositories.findDependenciesReference(extension);
        if (!dependRefs.isEmpty()) {
            throw new ErrorResultException("The following extensions have a dependency on " + namespace.getName() + "."
                    + extension.getName() + ": "
                    + dependRefs.stream()
                        .map(ev -> ev.getExtension().getNamespace().getName() + "." + ev.getExtension().getName() + "@" + ev.getVersion())
                        .collect(Collectors.joining(", ")));
        }
        extension.setLatest(null);
        extension.setPreview(null);
        for (var extVersion : repositories.findVersions(extension)) {
            removeExtensionVersion(extVersion);
        }
        for (var review : repositories.findAllReviews(extension)) {
            entityManager.remove(review);
        }
        entityManager.remove(extension);
        search.removeSearchEntry(extension);

        var result = ResultJson.success("Deleted " + namespace.getName() + "." + extension.getName());
        logAdminAction(admin, result);
        return result;
    }

    protected ResultJson deleteExtension(ExtensionVersion extVersion, UserData admin) {
        var extension = extVersion.getExtension();
        var versions = Lists.newArrayList(repositories.findVersions(extension));
        if (versions.size() == 1) {
            return deleteExtension(extension, admin);
        }
        removeExtensionVersion(extVersion);
        versions.remove(extVersion);
        if (extVersion.equals(extension.getLatest())) {
            extension.setLatest(getLatestVersion(versions, false));
            if (extension.getLatest() == null)
                extension.setLatest(getLatestVersion(versions, true));
        }
        if (extVersion.equals(extension.getPreview())) {
            extension.setPreview(getLatestVersion(versions, true));
        }

        var result = ResultJson.success("Deleted " + extension.getNamespace().getName() + "." + extension.getName()
                + " version " + extVersion.getVersion());
        logAdminAction(admin, result);
        return result;
    }

    private void removeExtensionVersion(ExtensionVersion extVersion) {
        repositories.findFiles(extVersion).forEach(file -> {
            if (file.getStorageType().equals(FileResource.STORAGE_GOOGLE) && googleStorage.isEnabled()) {
                googleStorage.removeFile(file.getName(), extVersion);
            }
            entityManager.remove(file);
        });
        entityManager.remove(extVersion);
    }

    private ExtensionVersion getLatestVersion(Iterable<ExtensionVersion> versions, boolean preview) {
        ExtensionVersion latest = null;
        SemanticVersion latestSemver = null;
        for (var extVer : versions) {
            if (extVer.isPreview() == preview) {
                var semver = extVer.getSemanticVersion();
                if (latestSemver == null || latestSemver.compareTo(semver) < 0) {
                    latest = extVer;
                    latestSemver = semver;
                }
            }
        }
        return latest;
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
        for (var extension : repositories.findExtensions(namespace)) {
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
    
    public UserPublishInfoJson getUserPublishInfo(String provider, String loginName) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException("User not found: " + loginName, HttpStatus.NOT_FOUND);
        }
        var serverUrl = UrlUtil.getBaseUrl();
        var accessTokens = repositories.findAccessTokens(user);
        List<ExtensionJson> versionJsons = new ArrayList<>();
        var activeAccessTokenNum = 0;
        for (var accessToken : accessTokens) {
            if (accessToken.isActive()) {
                activeAccessTokenNum++;
            }
            var versions = repositories.findVersionsByAccessToken(accessToken);
            for (var version : versions) {
                var json = version.toExtensionJson();
                json.files = Maps.newLinkedHashMapWithExpectedSize(5);
                storageUtil.addFileUrls(version, serverUrl, json.files, FileResource.DOWNLOAD, FileResource.MANIFEST,
                        FileResource.ICON, FileResource.README, FileResource.LICENSE);
                versionJsons.add(json);
            }
        }
        var userPublishInfo = new UserPublishInfoJson();
        userPublishInfo.user = user.toUserJson();
        userPublishInfo.extensions = versionJsons;
        userPublishInfo.activeAccessTokenNum = activeAccessTokenNum;
        return userPublishInfo;
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson revokePublisherAgreement(String provider, String loginName, UserData admin) {
        var user = repositories.findUserByLoginName(provider, loginName);
        if (user == null) {
            throw new ErrorResultException("User not found: " + loginName, HttpStatus.NOT_FOUND);
        }

        // Send a DELETE request to the Eclipse API
        if (eclipse.isActive()) {
            eclipse.revokePublisherAgreement(user);
        }

        var accessTokens = repositories.findAccessTokens(user);
        var deactivatedTokenCount = 0;
        var deletedExtensionCount = 0;
        for (var accessToken : accessTokens) {
            // Deactivate the user's access tokens
            if (accessToken.isActive()) {
                accessToken.setActive(false);
                deactivatedTokenCount++;
            }

            // Delete all published extensions
            var versions = repositories.findVersionsByAccessToken(accessToken);
            for (var version : versions) {
                deleteExtension(version, admin);
                deletedExtensionCount++;
            }
        }

        var result = ResultJson.success("Deactivated " + deactivatedTokenCount
                + " tokens and deleted " + deletedExtensionCount + " extensions of user "
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
}