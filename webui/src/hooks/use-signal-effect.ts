/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { useLayoutEffect, useRef } from 'react';
import { Signal } from './use-signal';

/**
 * Runs `effect` whenever `signal` is emitted, skipping the initial mount. Used
 * to coordinate imperative actions (e.g. moving focus) across components — a
 * component emits the signal to "send" a request and subscribers react without
 * any global DOM lookups. If the signal carries a payload, the latest emitted
 * value is passed to `effect`.
 *
 * Uses a layout effect so focus moves synchronously after a `flushSync`
 * navigation (keeping the mobile keyboard open during the hero → nav morph).
 * Wrap `effect` in `useCallback` so it only re-subscribes when its deps change.
 */
export function useSignalEffect<T = void>(signal: Signal<T>, effect: (payload: T) => void): void {
    const last = useRef(signal.signal);
    useLayoutEffect(() => {
        if (signal.signal === last.current) {
            return;
        }
        last.current = signal.signal;
        // Only reached after an emit, so a carried payload is always set.
        effect(signal.payload as T);
    }, [signal, effect]);
}
