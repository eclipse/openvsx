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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { useSavedFlash } from '../../../src/hooks/use-saved-flash';

beforeEach(() => vi.useFakeTimers());
afterEach(() => vi.useRealTimers());

describe('useSavedFlash', () => {
    it('holds the flag for the given duration, then calls onDone', () => {
        const onDone = vi.fn();
        const { result } = renderHook(() => useSavedFlash(1000, onDone));

        act(() => result.current.flash());
        expect(result.current.saved).toBe(true);

        act(() => vi.advanceTimersByTime(999));
        expect(result.current.saved).toBe(true);
        expect(onDone).not.toHaveBeenCalled();

        act(() => vi.advanceTimersByTime(1));
        expect(result.current.saved).toBe(false);
        expect(onDone).toHaveBeenCalledOnce();
    });

    it('restarts the countdown when flashed again', () => {
        const onDone = vi.fn();
        const { result } = renderHook(() => useSavedFlash(1000, onDone));

        act(() => result.current.flash());
        act(() => vi.advanceTimersByTime(900));
        act(() => result.current.flash());
        act(() => vi.advanceTimersByTime(900));

        expect(result.current.saved).toBe(true);
        expect(onDone).not.toHaveBeenCalled();
    });

    it('drops the flag early on clear, without calling onDone', () => {
        const onDone = vi.fn();
        const { result } = renderHook(() => useSavedFlash(1000, onDone));

        act(() => result.current.flash());
        act(() => result.current.clear());

        expect(result.current.saved).toBe(false);
        act(() => vi.advanceTimersByTime(2000));
        expect(onDone).not.toHaveBeenCalled();
    });

    it('does not call onDone after the hook is unmounted', () => {
        const onDone = vi.fn();
        const { result, unmount } = renderHook(() => useSavedFlash(1000, onDone));

        act(() => result.current.flash());
        unmount();
        act(() => vi.advanceTimersByTime(2000));

        expect(onDone).not.toHaveBeenCalled();
    });

    it('calls the latest onDone, not the one captured when flash was created', () => {
        const first = vi.fn();
        const second = vi.fn();
        const { result, rerender } = renderHook(({ onDone }) => useSavedFlash(1000, onDone), {
            initialProps: { onDone: first }
        });

        act(() => result.current.flash());
        rerender({ onDone: second });
        act(() => vi.advanceTimersByTime(1000));

        expect(first).not.toHaveBeenCalled();
        expect(second).toHaveBeenCalledOnce();
    });
});
