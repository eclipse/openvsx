/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.eclipse;

import java.util.LinkedHashSet;

import jakarta.persistence.EntityManager;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class PublisherComplianceChecker {

    protected final Logger logger = LoggerFactory.getLogger(PublisherComplianceChecker.class);

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    ExtensionService extensions;

    @Autowired
    EclipseService eclipseService;

    @Value("${ovsx.eclipse.check-compliance-on-start:false}")
    boolean checkCompliance;

    @EventListener
    public void checkPublishers(ApplicationStartedEvent event) {
        if (!checkCompliance || !eclipseService.isActive())
            return;

        repositories.findAllUsers().forEach(user -> {
            var accessTokens = repositories.findAccessTokens(user);
            if (!accessTokens.isEmpty() && !isCompliant(user)) {
                // Found a non-compliant publisher: deactivate all extension versions
                transactions.<Void>execute(status -> {
                    deactivateExtensions(accessTokens);
                    return null;
                });
            }
        });
    }

    private boolean isCompliant(UserData user) {
        // Users without authentication provider have been created directly in the DB,
        // so we skip the agreement check in this case.
        if (user.getProvider() == null) {
            return true;
        }
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || eclipseData.personId == null) {
            // The user has never logged in with Eclipse
            return false;
        }
        if (eclipseData.publisherAgreement == null || !eclipseData.publisherAgreement.isActive) {
            // We don't have any active PA in our DB, let's check their Eclipse profile
            var profile = eclipseService.getPublicProfile(eclipseData.personId);
            if (profile.publisherAgreements == null || profile.publisherAgreements.openVsx == null
                    || profile.publisherAgreements.openVsx.version == null) {
                return false;
            }
        }
        return true;
    }

    private void deactivateExtensions(Streamable<PersonalAccessToken> accessTokens) {
        var affectedExtensions = new LinkedHashSet<Extension>();
        for (var accessToken : accessTokens) {
            var versions = repositories.findVersionsByAccessToken(accessToken, true);
            for (var version : versions) {
                version.setActive(false);
                entityManager.merge(version);
                var extension = version.getExtension();
                affectedExtensions.add(extension);
                logger.info("Deactivated: " + accessToken.getUser().getLoginName() + " - " + NamingUtil.toLogFormat(version));
            }
        }
        
        // Update affected extensions
        for (var extension : affectedExtensions) {
            extensions.updateExtension(extension);
            entityManager.merge(extension);
        }
    }
    
}