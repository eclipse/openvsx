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

import { useCallback, useMemo, useState } from 'react';

export interface Signal<T = void> {
    /** Monotonically increasing counter; bumped on every emit. */
    signal: number;
    /** Value passed to the latest emit, if the signal carries one. */
    payload?: T;
    /** Broadcast the signal to subscribers. */
    emit: (payload: T) => void;
}

/**
 * Creates a one-off broadcast channel backed by a monotonically increasing
 * counter. A component holding the signal calls `emit()` to fire it, and any
 * number of subscribers react through `useSignalEffect` — coordinating
 * imperative actions (e.g. moving focus) across components without global DOM
 * lookups. Share the returned value through context to reach other components.
 */
export function useSignal<T = void>(): Signal<T> {
    const [state, setState] = useState<{ signal: number; payload?: T }>({ signal: 0 });
    const emit = useCallback((payload: T) => setState(s => ({ signal: s.signal + 1, payload })), []);
    return useMemo(() => ({ signal: state.signal, payload: state.payload, emit }), [state, emit]);
}
