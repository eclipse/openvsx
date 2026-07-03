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

import { useEffect, useRef } from 'react';
import { useKeyboardShortcuts } from '../context/keyboard-shortcuts-context';

export interface UseShortcutOptions {
    key: string;
    label: string;
    callback: () => void;
    order?: number;
    enabled?: boolean;
}

/**
 * Register a keyboard shortcut for the lifetime of the calling component.
 * The shortcut is automatically unregistered when the component unmounts.
 * Callback is kept fresh via a ref — no re-registration on re-render.
 */
export function useShortcut({ key, label, callback, order = 99, enabled = true }: UseShortcutOptions): void {
    const { register, unregister } = useKeyboardShortcuts();

    // Keep callback fresh without forcing re-registration on every render
    const callbackRef = useRef(callback);
    callbackRef.current = callback;

    useEffect(() => {
        if (!enabled) return;
        register({ key, label, order, callback: () => callbackRef.current() });
        return () => unregister(key);
    }, [key, label, order, enabled]);
}
