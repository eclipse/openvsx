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
import java.time.ZoneId;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.PersistedLog;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.SemanticVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdminService {

    @Autowired
    RepositoryService repositories;

    @Autowired
    EntityManager entityManager;

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson deleteExtension(String namespaceName, String extensionName, String version, UserData admin)
            throws ErrorResultException {
        if (Strings.isNullOrEmpty(version)) {
            var extension = repositories.findExtension(extensionName, namespaceName);
            if (extension == null) {
                throw new ErrorResultException("Extension not found: " + namespaceName + "." + extensionName);
            }
            return deleteExtension(extension, admin);
        } else {
            var extVersion = repositories.findVersion(version, extensionName, namespaceName);
            if (extVersion == null) {
                throw new ErrorResultException("Extension not found: " + namespaceName + "." + extensionName + " version " + version);
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
            throw new ErrorResultException("The following extensions have a dependency on " + namespace.getName() + "." + extension.getName() + ": "
                    + dependRefs.stream()
                        .map(ev -> ev.getExtension().getNamespace().getName() + "." + ev.getExtension().getName() + "@" + ev.getVersion())
                        .collect(Collectors.joining(", ")));
        }
        extension.setLatest(null);
        for (var extVersion : extension.getVersions()) {
            removeExtensionVersion(extVersion);
        }
        for (var review : repositories.findAllReviews(extension)) {
            entityManager.remove(review);
        }
        entityManager.remove(extension);
        return logAdminAction(admin, "Deleted " + namespace.getName() + "." + extension.getName());
    }

    protected ResultJson deleteExtension(ExtensionVersion extVersion, UserData admin) {
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

    @Transactional
    public ResultJson logAdminAction(UserData admin, String message) {
        if(admin == null) {
            return null;
        }
        var log = new PersistedLog();
        log.setUser(admin);
        log.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        log.setMessage(message);
        entityManager.persist(log);
        return ResultJson.success(message);
    }

}