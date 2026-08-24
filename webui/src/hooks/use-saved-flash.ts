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

import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Drives the SaveButton's `saved` confirmation: `flash()` turns the flag on and
 * back off after `duration` ms, calling `onDone` when it runs out. `clear()`
 * drops the flag early (e.g. when the user resumes editing) without `onDone`.
 */
export const useSavedFlash = (duration: number, onDone?: () => void) => {
    const [saved, setSaved] = useState(false);
    const timer = useRef<ReturnType<typeof setTimeout>>();
    const onDoneRef = useRef(onDone);
    onDoneRef.current = onDone;

    useEffect(() => () => clearTimeout(timer.current), []);

    const flash = useCallback(() => {
        setSaved(true);
        clearTimeout(timer.current);
        timer.current = setTimeout(() => {
            setSaved(false);
            onDoneRef.current?.();
        }, duration);
    }, [duration]);

    const clear = useCallback(() => {
        clearTimeout(timer.current);
        setSaved(false);
    }, []);

    return { saved, flash, clear };
};
