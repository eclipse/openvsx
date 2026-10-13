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

import { getJson } from './http';
import { TrustedPublishingOptions } from './trusted-publishing-options';
import { configuredTimeout } from './util';

/**
 * Whether an OIDC ID token can be obtained without user interaction.
 */
export function hasIdTokenSource(options: TrustedPublishingOptions): boolean {
    return Boolean(options.idToken) || isGitHubActionsIdTokenAvailable();
}

/**
 * Obtains an OIDC ID token for the given audience from the surrounding CI system.
 */
export async function getIdToken(audience: string, options: TrustedPublishingOptions): Promise<string> {
    // CI systems such as GitLab CI provide the ID token directly as an environment variable
    if (options.idToken) {
        return options.idToken;
    }
    if (isGitHubActionsIdTokenAvailable()) {
        return getGitHubActionsIdToken(audience);
    }
    throw new Error('No OIDC ID token available for trusted publishing.\n'
        + "On GitHub Actions, grant the job the 'id-token: write' permission.\n"
        + 'On other CI systems, pass the ID token via the --idToken argument '
        + 'or the OVSX_ID_TOKEN environment variable.');
}

function isGitHubActionsIdTokenAvailable(): boolean {
    return Boolean(process.env.ACTIONS_ID_TOKEN_REQUEST_URL && process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN);
}

async function getGitHubActionsIdToken(audience: string): Promise<string> {
    // the request URL already carries an api-version query parameter, so keep its query intact
    const url = new URL(process.env.ACTIONS_ID_TOKEN_REQUEST_URL!);
    url.searchParams.set('audience', audience);
    // Sent with only these headers, so none of the registry's credentials reach the CI system's
    // token service. OVSX_TIMEOUT still applies, as there are no registry options to read it from.
    const response = await getJson<GitHubIdTokenResponse>(url, {
        'Authorization': `Bearer ${process.env.ACTIONS_ID_TOKEN_REQUEST_TOKEN}`,
        'Accept': 'application/json'
    }, configuredTimeout());
    if (!response.value) {
        throw new Error('GitHub Actions did not return an OIDC ID token.');
    }
    return response.value;
}

interface GitHubIdTokenResponse {
    count?: number;
    value?: string;
}
