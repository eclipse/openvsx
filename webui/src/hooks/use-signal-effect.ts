/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useLayoutEffect, useRef } from 'react';

/**
 * Runs `effect` whenever `signal` changes to a new value, skipping the initial
 * mount. Used to coordinate imperative focus across components through a
 * monotonically increasing counter held in the search context — a component
 * bumps the counter to "send" a request and subscribers react without any
 * global DOM lookups.
 *
 * Uses a layout effect so focus moves synchronously after a `flushSync`
 * navigation (keeping the mobile keyboard open during the hero → nav morph).
 * Wrap `effect` in `useCallback` so it only re-subscribes when its deps change.
 */
export function useSignalEffect(signal: number, effect: () => void): void {
    const last = useRef(signal);
    useLayoutEffect(() => {
        if (signal === last.current) {
            return;
        }
        last.current = signal;
        effect();
    }, [signal, effect]);
}
