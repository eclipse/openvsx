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

import { getIdToken, hasIdTokenSource } from './oidc';
import { AccessToken, Registry } from './registry';
import { TrustedPublishingOptions } from './trusted-publishing-options';

const tokens = new Map<string, Promise<string>>();

/**
 * Whether the given options ask for trusted publishing. If the user did not decide explicitly,
 * it is used whenever the surrounding CI system can provide an OIDC ID token.
 */
export function useTrustedPublishing(options: TrustedPublishingOptions): boolean {
    return options.trustedPublishing ?? hasIdTokenSource(options);
}

/**
 * Exchanges an OIDC ID token for a short-lived access token that can publish the given extension.
 * The token is not stored, it is only valid for a few minutes.
 */
export function getTrustedPublishingToken(
    registry: Registry,
    namespace: string,
    extension: string,
    options: TrustedPublishingOptions
): Promise<string> {
    // publishing fans out over targets and package paths, but one token is enough per extension
    const key = `${namespace}.${extension}`;
    let token = tokens.get(key);
    if (!token) {
        token = requestToken(registry, namespace, extension, options);
        tokens.set(key, token);
    }

    return token;
}

async function requestToken(
    registry: Registry,
    namespace: string,
    extension: string,
    options: TrustedPublishingOptions
): Promise<string> {
    const audience = options.oidcAudience ?? registry.url;
    const idToken = await getIdToken(audience, options);

    let result: AccessToken;
    try {
        result = await registry.requestTrustedPublishingToken(namespace, extension, idToken);
    } catch (err) {
        throw new Error(`${err.message}\n${registrationHint(registry, namespace, extension)}`);
    }
    if (result.error) {
        throw new Error(`${result.error}\n${registrationHint(registry, namespace, extension)}`);
    }
    if (!result.value) {
        throw new Error('The registry did not return a publishing token.');
    }

    const expires = result.expiresTimestamp ? `, expires at ${result.expiresTimestamp}` : '';
    console.log(`\ud83d\udd10  Trusted publishing token issued for ${namespace}.${extension}${expires}`);
    return result.value;
}

function registrationHint(registry: Registry, namespace: string, extension: string): string {
    return `Check the trusted publishers registered for '${namespace}.${extension}' at `
        + `${registry.url}/user-settings/trusted-publishers.`;
}
