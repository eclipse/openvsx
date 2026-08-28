/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { describe, it, expect, vi, afterEach } from 'vitest';
import { formatFileSize, handleError, toRelativeTime } from '../../src/utils';

describe('toRelativeTime', () => {
    const msPerMinute = 60 * 1000;
    const msPerHour = msPerMinute * 60;
    const msPerDay = msPerHour * 24;
    const msPerMonth = msPerDay * 30.4;
    const msPerYear = msPerDay * 365;
    it('should report "now" for seconds', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10000).toString())).toBe('now');
        expect(toRelativeTime(new Date(now).toString())).toBe('now');
    });
    it('should correctly report minutes', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerMinute).toString())).toBe('10 minutes ago');
        expect(toRelativeTime(new Date(now - msPerMinute).toString())).toBe('1 minute ago');
    });
    it('should correctly report hours', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerHour).toString())).toBe('10 hours ago');
        expect(toRelativeTime(new Date(now - msPerHour).toString())).toBe('1 hour ago');
    });
    it('should correctly report days', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerDay).toString())).toBe('10 days ago');
        expect(toRelativeTime(new Date(now - msPerDay).toString())).toBe('1 day ago');
    });
    it('should correctly report months', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerMonth).toString())).toBe('10 months ago');
        expect(toRelativeTime(new Date(now - msPerMonth).toString())).toBe('1 month ago');
    });
    it('should correctly report years', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerYear).toString())).toBe('10 years ago');
        expect(toRelativeTime(new Date(now - msPerYear).toString())).toBe('1 year ago');
    });
});

describe('handleError', () => {
    // The formatter logs every error it is handed; the assertions below are about its return value.
    afterEach(() => vi.restoreAllMocks());
    const silenced = () => vi.spyOn(console, 'error').mockImplementation(() => {});

    it('keeps both halves of a server error response', () => {
        silenced();
        expect(handleError({ error: 'Bad Request', message: 'Extension too large' })).toBe(
            'Bad Request (Extension too large)'
        );
    });

    it('reports whichever half the response carries', () => {
        silenced();
        expect(handleError({ error: 'Unknown publisher: foo' })).toBe('Unknown publisher: foo');
        expect(handleError({ message: 'Not found' })).toBe('Not found');
    });

    it('takes a thrown value of any shape', () => {
        silenced();
        expect(handleError(new Error('boom'))).toBe('An unexpected error occurred: boom');
        expect(handleError('boom')).toBe('An unexpected error occurred.');
        expect(handleError(undefined)).toBe('An unexpected error occurred.');
    });

    it('stays quiet about an aborted request', () => {
        const logged = silenced();
        const aborted = new Error('The operation was aborted.');
        aborted.name = 'AbortError';
        expect(handleError(aborted)).toBe('');
        expect(logged).not.toHaveBeenCalled();
    });
});

describe('formatFileSize', () => {
    it('counts plain bytes below a kilobyte', () => {
        expect(formatFileSize(0)).toBe('0 B');
        expect(formatFileSize(1023)).toBe('1023 B');
    });

    it('steps up a unit every 1024, to two decimals', () => {
        expect(formatFileSize(1024)).toBe('1.00 kB');
        expect(formatFileSize(1536)).toBe('1.50 kB');
        expect(formatFileSize(512 * 1024 * 1024)).toBe('512.00 MB');
    });

    it('stops at the largest unit it knows', () => {
        expect(formatFileSize(1024 ** 5)).toBe('1,024.00 TB');
    });
});
