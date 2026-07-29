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

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getPAT, requestPAT } from '../../src/pat';

const storedTokens = new Map<string, string>();

vi.mock('../../src/store', () => ({
    openDefaultStore: async () => ({
        get: async (name: string) => storedTokens.get(name),
        add: async (name: string, value: string) => {
            storedTokens.set(name, value);
        },
        delete: async (name: string) => {
            storedTokens.delete(name);
        }
    })
}));

// Prompting must never be reached in these tests, so the prompt itself fails the test.
function failOnPrompt(): never {
    throw new Error('the user was asked for input');
}

vi.mock('@inquirer/prompts', () => ({
    password: failOnPrompt,
    input: failOnPrompt,
    select: failOnPrompt,
    confirm: failOnPrompt
}));

describe('getPAT', () => {

    beforeEach(() => {
        storedTokens.clear();
    });

    it('prefers the token from the options', async () => {
        expect(await getPAT('testpub', { pat: 'from-options', interactive: false })).toBe('from-options');
    });

    it('falls back to the stored token', async () => {
        storedTokens.set('testpub', 'from-store');

        expect(await getPAT('testpub', { interactive: false })).toBe('from-store');
    });

    it('explains how to provide a token instead of prompting', async () => {
        await expect(getPAT('testpub', { interactive: false })).rejects.toThrow(
            /No personal access token found for namespace 'testpub'/);
        await expect(getPAT('testpub', { interactive: false })).rejects.toThrow(/OVSX_PAT/);
    });

    it('refuses to request a token without user interaction', async () => {
        await expect(requestPAT('testpub', { interactive: false })).rejects.toThrow(
            /without user interaction/);
    });
});
