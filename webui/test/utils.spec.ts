/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import 'mocha';
import { expect } from 'chai';
import { toRelativeTime } from '../src/utils';

describe('toRelativeTime', () => {
    const msPerMinute = 60 * 1000;
    const msPerHour = msPerMinute * 60;
    const msPerDay = msPerHour * 24;
    const msPerMonth = msPerDay * 30.4;
    const msPerYear = msPerDay * 365;
    it('should report "now" for seconds', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10000).toString())).to.equal('now');
        expect(toRelativeTime(new Date(now).toString())).to.equal('now');
    });
    it('should correctly report minutes', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerMinute).toString())).to.equal('10 minutes ago');
        expect(toRelativeTime(new Date(now - msPerMinute).toString())).to.equal('1 minute ago');
    });
    it('should correctly report hours', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerHour).toString())).to.equal('10 hours ago');
        expect(toRelativeTime(new Date(now - msPerHour).toString())).to.equal('1 hour ago');
    });
    it('should correctly report days', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerDay).toString())).to.equal('10 days ago');
        expect(toRelativeTime(new Date(now - msPerDay).toString())).to.equal('1 day ago');
    });
    it('should correctly report months', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerMonth).toString())).to.equal('10 months ago');
        expect(toRelativeTime(new Date(now - msPerMonth).toString())).to.equal('1 month ago');
    });
    it('should correctly report years', () => {
        const now = Date.now();
        expect(toRelativeTime(new Date(now - 10 * msPerYear).toString())).to.equal('10 years ago');
        expect(toRelativeTime(new Date(now - msPerYear).toString())).to.equal('1 year ago');
    });
});
