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
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.openvsx.entities.PublisherAgreementEvent;
import org.eclipse.openvsx.entities.SemanticVersion;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;

/**
 * The agreement kept in this database: a user's signing is the {@code SIGNED} event that
 * {@link PublisherAgreementService} records after {@link #sign}, and a later {@code REVOKED} event ends it. A
 * version is a semantic version in which the minor and patch numbers may be left out ({@code 1}, {@code 1.0}).
 * Which signatures are valid depends on the allowed versions: when there are some, a signature is valid if its
 * version is one of them or the configured one; when there are none, it is valid within the major version of the
 * configured one, so only a new major version makes signatures outdated. A blank version disables agreements.
 */
public class SimplePublisherAgreementProvider implements PublisherAgreementProvider {

    private static final String VERSION_PROPERTY = "ovsx.publisher-agreement.version";
    private static final String ALLOWED_VERSIONS_PROPERTY = "ovsx.publisher-agreement.allowed-versions";
    private static final Pattern SHORT_VERSION = Pattern.compile("\\d+(?:\\.\\d+){0,2}");
    private static final List<PublisherAgreementEvent.Type> SIGNING_STEPS = List.of(
            PublisherAgreementEvent.Type.SIGNED,
            PublisherAgreementEvent.Type.REVOKED);

    private final RepositoryService repositories;
    private final String versionText;
    private final SemanticVersion currentVersion;
    /** The allowed versions in their full form, with the configured one; empty to accept the same major. */
    private final Set<String> allowedVersions;

    /**
     * @param allowedVersions the versions a signature may have, besides the configured one; none to accept any
     *        signature of the same major version
     * @throws IllegalArgumentException if a version is neither blank nor like 1, 1.0 or a semantic version
     */
    public SimplePublisherAgreementProvider(
            RepositoryService repositories,
            String version,
            List<String> allowedVersions
    ) {
        this.repositories = repositories;
        this.versionText = version == null ? null : version.trim();
        this.currentVersion = versionText == null || versionText.isEmpty()
                ? null
                : parse(VERSION_PROPERTY, versionText);
        var allowed = allowedVersions.stream()
                .map(allowedVersion -> full(ALLOWED_VERSIONS_PROPERTY, allowedVersion.trim()))
                .collect(Collectors.toSet());
        if (!allowed.isEmpty() && currentVersion != null) {
            allowed.add(full(VERSION_PROPERTY, versionText));
        }
        this.allowedVersions = Set.copyOf(allowed);
    }

    /** Reads a version, in which 1 and 1.0 stand for 1.0.0, besides a full semantic version. */
    private static SemanticVersion parse(String property, String version) {
        return SemanticVersion.parse(full(property, version));
    }

    /** The version with its minor and patch numbers spelled out, checked to be one. */
    private static String full(String property, String version) {
        var missing = 2 - version.chars().filter(c -> c == '.').count();
        var full = SHORT_VERSION.matcher(version).matches() ? version + ".0".repeat((int) missing) : version;
        try {
            if (SemanticVersion.VERSION_PARSE_PATTERN.matcher(full).matches()) {
                SemanticVersion.parse(full);
                return full;
            }
        } catch (NumberFormatException exc) {
            throw new IllegalArgumentException("Invalid " + property + ": '" + version + "'", exc);
        }
        throw new IllegalArgumentException("Invalid " + property + ": '" + version + "'");
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
        var valid = allowedVersions.isEmpty()
                ? parse(VERSION_PROPERTY, step.getVersion()).getMajor() >= currentVersion.getMajor()
                : allowedVersions.contains(full(VERSION_PROPERTY, step.getVersion()));
        var state = valid ? AgreementStatus.State.SIGNED : AgreementStatus.State.OUTDATED;
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
