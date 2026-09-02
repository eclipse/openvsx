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
import { isError, isSuccess } from '../../src/extension-registry-types';

describe('isError', () => {
    it('reports a result that carries an error message', () => {
        expect(isError({ error: 'Namespace not found' })).toBe(true);
    });

    it('reports an aggregate result where some items succeeded and others failed', () => {
        expect(isError({ success: 'Deleted 1.0.0', error: 'Failed to delete 2.0.0' })).toBe(true);
    });

    it('ignores an empty error reported alongside a success', () => {
        expect(isError({ success: 'Deleted 2 versions', error: '' })).toBe(false);
    });

    it('answers a null or undefined body instead of throwing', () => {
        expect(isError(null)).toBe(false);
        expect(isError(undefined)).toBe(false);
    });

    it('ignores a result with no error at all', () => {
        expect(isError({ success: 'ok' })).toBe(false);
        expect(isError('not a result')).toBe(false);
    });
});

describe('isSuccess', () => {
    it('reports a result that carries a success message', () => {
        expect(isSuccess({ success: 'ok' })).toBe(true);
    });

    it('reports an empty success message as a success, unlike an empty error', () => {
        expect(isSuccess({ success: '' })).toBe(true);
    });

    it('answers a null body instead of throwing', () => {
        expect(isSuccess(null)).toBe(false);
    });
});
