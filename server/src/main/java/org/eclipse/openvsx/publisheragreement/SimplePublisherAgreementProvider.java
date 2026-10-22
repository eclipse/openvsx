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
import java.util.regex.Pattern;

import org.eclipse.openvsx.entities.PublisherAgreementEvent;
import org.eclipse.openvsx.entities.SemanticVersion;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;

/**
 * The agreement kept in this database: a user's signing is the {@code SIGNED} event that
 * {@link PublisherAgreementService} records after {@link #sign}, and a later {@code REVOKED} event ends it. A
 * version is a semantic version in which the minor and patch numbers may be left out ({@code 1}, {@code 1.0}). A
 * signature stays valid within the major version of the configured one; raising the major version makes it
 * outdated. A blank version disables agreements.
 */
public class SimplePublisherAgreementProvider implements PublisherAgreementProvider {

    private static final Pattern SHORT_VERSION = Pattern.compile("\\d+(?:\\.\\d+){0,2}");
    private static final List<PublisherAgreementEvent.Type> SIGNING_STEPS = List.of(
            PublisherAgreementEvent.Type.SIGNED,
            PublisherAgreementEvent.Type.REVOKED);

    private final RepositoryService repositories;
    private final String versionText;
    private final SemanticVersion currentVersion;

    /** @throws IllegalArgumentException if the version is neither blank nor like 1, 1.0 or a semantic version */
    public SimplePublisherAgreementProvider(RepositoryService repositories, String version) {
        this.repositories = repositories;
        this.versionText = version == null ? null : version.trim();
        this.currentVersion = versionText == null || versionText.isEmpty() ? null : parse(versionText);
    }

    /** Reads a version, in which 1 and 1.0 stand for 1.0.0, besides a full semantic version. */
    private static SemanticVersion parse(String version) {
        var missing = 2 - version.chars().filter(c -> c == '.').count();
        var full = SHORT_VERSION.matcher(version).matches() ? version + ".0".repeat((int) missing) : version;
        if (!SemanticVersion.VERSION_PARSE_PATTERN.matcher(full).matches()) {
            throw new IllegalArgumentException("Invalid ovsx.publisher-agreement.version: '" + version + "'");
        }
        try {
            return SemanticVersion.parse(full);
        } catch (NumberFormatException exc) {
            throw new IllegalArgumentException("Invalid ovsx.publisher-agreement.version: '" + version + "'", exc);
        }
    }

    @Override
    public boolean enabled() {
        return currentVersion != null;
    }

    @Override
    public AgreementStatus status(UserData user) {
        var step = repositories.findLatestPublisherAgreementEvent(user.getId(), SIGNING_STEPS);
        if (step == null || step.getType() == PublisherAgreementEvent.Type.REVOKED) {
            return AgreementStatus.of(AgreementStatus.State.NONE);
        }
        var state = parse(step.getVersion()).getMajor() >= currentVersion.getMajor()
                ? AgreementStatus.State.SIGNED
                : AgreementStatus.State.OUTDATED;
        return new AgreementStatus(state, step.getVersion(), step.getTimestamp(), false);
    }

    /** The service records the signing, which is all there is to keep. */
    @Override
    public AgreementStatus sign(UserData user) {
        return new AgreementStatus(AgreementStatus.State.SIGNED, versionText, TimeUtil.getCurrentUTC(), false);
    }

    /** The service's {@code REVOKED} event ends the signing. */
    @Override
    public void withdraw(UserData user, UserData admin) {
    }
}
