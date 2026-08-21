/********************************************************************************
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
 ********************************************************************************/

import { describe, expect, it } from 'vitest';
import {
    findRegistrationPathPair,
    orderRegistrationKeys,
    providerKind
} from '../../../../../src/pages/user/trusted-publishing/registration-fields';

describe('providerKind', () => {
    it('maps provider ids to their family', () => {
        expect(providerKind('github')).toBe('github');
        expect(providerKind('gitlab')).toBe('gitlab');
        expect(providerKind('eclipse-gitlab')).toBe('gitlab');
        expect(providerKind('bitbucket')).toBe('generic');
    });
});

describe('orderRegistrationKeys', () => {
    it('imposes the canonical order regardless of server order', () => {
        expect(orderRegistrationKeys(['environment', 'workflow', 'repo', 'owner'])).toEqual([
            'owner',
            'repo',
            'workflow',
            'environment'
        ]);
    });

    it('appends unknown keys alphabetically after the known ones', () => {
        expect(orderRegistrationKeys(['zeta', 'workflow', 'alpha', 'owner'])).toEqual([
            'owner',
            'workflow',
            'alpha',
            'zeta'
        ]);
    });

    it('does not mutate its input', () => {
        const keys = ['workflow', 'owner'];
        orderRegistrationKeys(keys);
        expect(keys).toEqual(['workflow', 'owner']);
    });
});

describe('findRegistrationPathPair', () => {
    it('finds the owner/repo pair when both keys are present', () => {
        expect(findRegistrationPathPair(['owner', 'repo', 'workflow'])).toEqual(['owner', 'repo']);
    });

    it('finds the namespace/project pair for GitLab-style inputs', () => {
        expect(findRegistrationPathPair(['namespace', 'project'])).toEqual(['namespace', 'project']);
    });

    it('returns undefined when a pair is incomplete', () => {
        expect(findRegistrationPathPair(['owner', 'workflow'])).toBeUndefined();
    });
});
