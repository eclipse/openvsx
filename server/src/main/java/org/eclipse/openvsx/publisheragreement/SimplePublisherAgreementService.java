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
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.PublisherAgreementEntryJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;

/**
 * Records each signing of the agreement, with its version, as an append-only history. Raising the
 * configured version makes every earlier signature outdated; revoking marks signatures revoked. A version of 0 disables agreements: everyone may publish.
 */
public class SimplePublisherAgreementService implements PublisherAgreementService {

    private final RepositoryService repositories;
    private final int currentVersion;

    public SimplePublisherAgreementService(RepositoryService repositories, int currentVersion) {
        this.repositories = repositories;
        this.currentVersion = currentVersion;
    }

    private boolean enabled() {
        return currentVersion > 0;
    }

    private PublisherAgreement latest(UserData user) {
        return repositories.findActivePublisherAgreements(user.getId()).stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public boolean hasPublisherAgreement(UserData user) {
        if (!enabled()) {
            return true;
        }
        var signed = latest(user);
        return signed != null && signed.getVersion() >= currentVersion;
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
        var agreement = new UserJson.PublisherAgreement();
        var signed = latest(user);
        if (signed == null) {
            agreement.setStatus("none");
        } else {
            agreement.setStatus(signed.getVersion() >= currentVersion ? "signed" : "outdated");
            agreement.setVersion(String.valueOf(signed.getVersion()));
            agreement.setTimestamp(TimeUtil.toUTCString(signed.getSignedTimestamp()));
        }
        json.setPublisherAgreement(agreement);
    }

    @Override
    @Transactional(rollbackOn = ErrorResultException.class)
    public void signPublisherAgreement(UserData user, UserJson json) {
        if (!enabled()) {
            throw new ErrorResultException("Publisher agreements are not supported.", HttpStatus.NOT_FOUND);
        }
        if (hasPublisherAgreement(user)) {
            enrichUserJson(json, user);
            return;
        }
        var signed = new PublisherAgreement();
        signed.setUserDataId(user.getId());
        signed.setVersion(currentVersion);
        signed.setSignedTimestamp(TimeUtil.getCurrentUTC());
        repositories.savePublisherAgreement(signed);
        enrichUserJson(json, user);
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
        for (var signed : repositories.findActivePublisherAgreements(user.getId())) {
            signed.setRevokedTimestamp(now);
            signed.setRevokedById(admin.getId());
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
                                a.getRevokedTimestamp() == null ? null : TimeUtil.toUTCString(a.getRevokedTimestamp())))
                .toList();
    }
}
