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
package org.eclipse.openvsx.trustedpublishing.gitlab;

import org.eclipse.openvsx.trustedpublishing.TrustedPublishingConfig;

/**
 * GitLab provider for <a href="https://gitlab.com/">GitLab Public Instance</a>.
 */
public class GitLabTrustedPublishingProvider extends GitLabTrustedPublishingProviderSupport {
    public static final String PROVIDER_ID = "gitlab";
    public static final String PROVIDER_URL = "https://gitlab.com";
    private static final String OIDC_ISSUER = "https://gitlab.com";

    public GitLabTrustedPublishingProvider(TrustedPublishingConfig config) {
        super(config, PROVIDER_ID, "GitLab", PROVIDER_URL, OIDC_ISSUER);
    }
}
