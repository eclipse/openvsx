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

import { useMemo, useRef } from 'react';

const DEFAULT_DELAY_MS = 300;

/**
 * Returns a stable function that delays invoking `callback` until `delay` ms
 * have elapsed since the last call. Always invokes the most recent `callback`.
 */
export function useDebouncedCallback<T extends (...args: any[]) => void>(
    callback: T,
    delay: number = DEFAULT_DELAY_MS
): (...args: Parameters<T>) => void {
    const callbackRef = useRef(callback);
    callbackRef.current = callback;

    return useMemo(() => {
        let timer: ReturnType<typeof setTimeout> | undefined;
        return (...args: Parameters<T>) => {
            clearTimeout(timer);
            timer = setTimeout(() => callbackRef.current(...args), delay);
        };
    }, [delay]);
}
