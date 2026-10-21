/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.publisheragreement;

import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;

import org.eclipse.openvsx.entities.PublisherAgreement;
import org.eclipse.openvsx.entities.SemanticVersion;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.PublisherAgreementEntryJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;

/**
 * Records each signing of the agreement, with its semantic version, as an append-only history. A signature
 * stays valid within the major version of the configured one; raising the major version makes it outdated. A
 * revocation blocks the user from signing again until an admin reinstates them. A blank version disables
 * agreements: everyone may publish. Users without a login provider, such as the development super_user, are
 * not held to the agreement unless it was revoked.
 */
public class SimplePublisherAgreementService implements PublisherAgreementService {

    private enum Status {
        NONE, SIGNED, OUTDATED, REVOKED
    }

    private static final String MARKER_VERSION = "0.0.0";

    private final RepositoryService repositories;
    private final String versionText;
    private final SemanticVersion currentVersion;

    /** @throws IllegalArgumentException if the version is neither blank nor a semantic version */
    public SimplePublisherAgreementService(RepositoryService repositories, String version) {
        this.repositories = repositories;
        this.versionText = version == null ? null : version.trim();
        this.currentVersion = versionText == null || versionText.isEmpty() ? null : parse(versionText);
    }

    private static SemanticVersion parse(String version) {
        if (!SemanticVersion.VERSION_PARSE_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("Invalid ovsx.publisher-agreement.version: '" + version + "'");
        }
        return SemanticVersion.parse(version);
    }

    private boolean enabled() {
        return currentVersion != null;
    }

    private PublisherAgreement latest(UserData user) {
        return repositories.findPublisherAgreementHistory(user.getId()).stream().findFirst().orElse(null);
    }

    private Status status(PublisherAgreement latest) {
        if (latest == null) {
            return Status.NONE;
        }
        if (latest.getRevokedTimestamp() != null) {
            return latest.getReinstatedTimestamp() == null ? Status.REVOKED : Status.NONE;
        }
        return SemanticVersion.parse(latest.getVersion()).getMajor() >= currentVersion.getMajor()
                ? Status.SIGNED
                : Status.OUTDATED;
    }

    @Override
    @Transactional
    public boolean hasPublisherAgreement(UserData user) {
        if (!enabled()) {
            return true;
        }
        var status = status(latest(user));
        // A user created directly in the database has no login to sign with, so only a revocation holds.
        return status == Status.SIGNED || (user.getProvider() == null && status != Status.REVOKED);
    }

    @Override
    @Transactional
    public void checkPublisherAgreement(UserData user) {
        if (!hasPublisherAgreement(user)) {
            throw new ErrorResultException(
                    "You must sign the current publisher agreement before publishing.",
                    HttpStatus.FORBIDDEN);
        }
    }

    @Override
    @Transactional
    public void enrichUserJson(UserJson json, UserData user) {
        if (!enabled()) {
            return;
        }
        var signed = latest(user);
        var status = status(signed);
        var agreement = new UserJson.PublisherAgreement();
        agreement.setStatus(status.name().toLowerCase());
        if (status == Status.SIGNED || status == Status.OUTDATED) {
            agreement.setVersion(signed.getVersion());
            agreement.setTimestamp(TimeUtil.toUTCString(signed.getSignedTimestamp()));
        }
        json.setPublisherAgreement(agreement);
    }

    @Override
    @Transactional(rollbackOn = ErrorResultException.class)
    public boolean signPublisherAgreement(UserData user, UserJson json) {
        if (!enabled()) {
            throw new ErrorResultException("Publisher agreements are not supported.", HttpStatus.NOT_FOUND);
        }
        var status = status(latest(user));
        if (status == Status.REVOKED) {
            throw new ErrorResultException(
                    "Your publisher agreement was revoked; contact an administrator.",
                    HttpStatus.FORBIDDEN);
        }
        var signedNow = status != Status.SIGNED;
        if (signedNow) {
            var signed = new PublisherAgreement();
            signed.setUserDataId(user.getId());
            signed.setVersion(versionText);
            signed.setSignedTimestamp(TimeUtil.getCurrentUTC());
            repositories.savePublisherAgreement(signed);
        }
        enrichUserJson(json, user);
        return signedNow;
    }

    @Override
    @Transactional
    public void adminEnrichUserJson(UserJson json, UserData user) {
        enrichUserJson(json, user);
    }

    @Override
    @Transactional
    public void revokePublisherAgreement(UserData user, UserData admin) {
        var now = TimeUtil.getCurrentUTC();
        var history = repositories.findPublisherAgreementHistory(user.getId());
        var active = history.stream().filter(a -> a.getRevokedTimestamp() == null).toList();
        if (active.isEmpty()) {
            if (status(history.stream().findFirst().orElse(null)) == Status.REVOKED) {
                return;
            }
            // Nothing signed to revoke: a revoked marker row keeps the user blocked.
            var marker = new PublisherAgreement();
            marker.setUserDataId(user.getId());
            marker.setVersion(MARKER_VERSION);
            marker.setSignedTimestamp(now);
            active = List.of(marker);
        }
        for (var signed : active) {
            signed.setRevokedTimestamp(now);
            signed.setRevokedById(admin.getId());
            repositories.savePublisherAgreement(signed);
        }
    }

    @Override
    @Transactional
    public void reinstatePublisherAgreement(UserData user) {
        var signed = latest(user);
        if (status(signed) == Status.REVOKED) {
            signed.setReinstatedTimestamp(TimeUtil.getCurrentUTC());
            repositories.savePublisherAgreement(signed);
        }
    }

    @Override
    @Transactional
    public List<PublisherAgreementEntryJson> publisherAgreementHistory(UserData user) {
        return repositories.findPublisherAgreementHistory(user.getId()).stream()
                .map(
                        a -> new PublisherAgreementEntryJson(
                                a.getVersion(),
                                TimeUtil.toUTCString(a.getSignedTimestamp()),
                                a.getRevokedTimestamp() == null ? null : TimeUtil.toUTCString(a.getRevokedTimestamp()),
                                a.getReinstatedTimestamp() == null
                                        ? null
                                        : TimeUtil.toUTCString(a.getReinstatedTimestamp())))
                .toList();
    }
}
