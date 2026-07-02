/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useCallback, useMemo, useState } from 'react';

export interface Signal {
    /** Monotonically increasing counter; bumped on every emit. */
    signal: number;
    /** Broadcast the signal to subscribers. */
    emit: () => void;
}

/**
 * Creates a one-off broadcast channel backed by a monotonically increasing
 * counter. A component holding the signal calls `emit()` to fire it, and any
 * number of subscribers react through `useSignalEffect` — coordinating
 * imperative actions (e.g. moving focus) across components without global DOM
 * lookups. Share the returned value through context to reach other components.
 */
export function useSignal(): Signal {
    const [signal, setSignal] = useState(0);
    const emit = useCallback(() => setSignal(n => n + 1), []);
    return useMemo(() => ({ signal, emit }), [signal, emit]);
}
