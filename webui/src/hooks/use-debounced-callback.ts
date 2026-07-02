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

import { useEffect, useMemo, useRef } from 'react';

const DEFAULT_DELAY_MS = 300;

export interface DebouncedCallback<T extends (...args: any[]) => void> {
    (...args: Parameters<T>): void;
    // Drop any pending invocation.
    cancel: () => void;
}

/**
 * Returns a stable function that delays invoking `callback` until `delay` ms
 * have elapsed since the last call. Always invokes the most recent `callback`.
 * The returned function also exposes `cancel` to drop a pending invocation.
 */
export function useDebouncedCallback<T extends (...args: any[]) => void>(
    callback: T,
    delay: number = DEFAULT_DELAY_MS
): DebouncedCallback<T> {
    const callbackRef = useRef(callback);
    callbackRef.current = callback;

    const debounced = useMemo(() => {
        let timer: ReturnType<typeof setTimeout> | undefined;

        const fn = ((...args: Parameters<T>) => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                timer = undefined;
                callbackRef.current(...args);
            }, delay);
        }) as DebouncedCallback<T>;

        fn.cancel = () => {
            clearTimeout(timer);
            timer = undefined;
        };

        return fn;
    }, [delay]);

    useEffect(() => debounced.cancel, [debounced]);

    return debounced;
}
