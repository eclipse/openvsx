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
import { getExtensionStatus } from '../../../../src/components/extension/extension-status';
import { Extension } from '../../../../src/extension-registry-types';

const extension = (overrides: Partial<Extension> = {}) =>
    ({ name: 'bar', namespace: 'foo', ...overrides }) as Extension;

describe('getExtensionStatus', () => {
    it('leaves a plain published extension without a status', () => {
        expect(getExtensionStatus(extension())).toBeUndefined();
        expect(getExtensionStatus(extension({ active: true, reviewStatus: 'published' }))).toBeUndefined();
    });

    it('reports each publishing state on its own', () => {
        expect(getExtensionStatus(extension({ removed: true }))?.label).toBe('Deleted');
        expect(getExtensionStatus(extension({ reviewStatus: 'rejected' }))?.label).toBe('Rejected');
        expect(getExtensionStatus(extension({ reviewStatus: 'under_review' }))?.label).toBe('Under review');
        expect(getExtensionStatus(extension({ active: false }))?.label).toBe('Deactivated');
        expect(getExtensionStatus(extension({ deprecated: true }))?.label).toBe('Deprecated');
    });

    it('ranks a removed extension above every other state', () => {
        const status = getExtensionStatus(
            extension({ removed: true, reviewStatus: 'under_review', active: false, deprecated: true })
        );
        expect(status?.label).toBe('Deleted');
    });

    it('ranks a review verdict above deactivation and deprecation', () => {
        const status = getExtensionStatus(extension({ reviewStatus: 'rejected', active: false, deprecated: true }));
        expect(status?.label).toBe('Rejected');
    });

    it('ranks deactivation above deprecation', () => {
        expect(getExtensionStatus(extension({ active: false, deprecated: true }))?.label).toBe('Deactivated');
    });
});
