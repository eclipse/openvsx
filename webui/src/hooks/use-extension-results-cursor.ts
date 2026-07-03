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

import { useCallback } from 'react';
import { ResultsNavAction, useSearchFocus } from '../context/search/search-focus-context';
import { GridCursor, useGridCursor } from './use-grid-cursor';
import { useSignalEffect } from './use-signal-effect';

/**
 * Wires the generic grid cursor to the search experience: the cursor ring is
 * visible while the search field has focus, the field's arrow keys drive the
 * cursor through `resultsNavigationSignal` ('open' clicks the card under it),
 * and ArrowUp from the first row hands focus back to the search field.
 */
export function useExtensionResultsCursor(itemCount: number): GridCursor {
    const { searchFocusSignal, resultsNavigationSignal, searchFocused } = useSearchFocus();
    const cursor = useGridCursor(itemCount, {
        cursorVisible: searchFocused,
        onExitTop: searchFocusSignal.emit
    });

    const { move, openActive } = cursor;
    useSignalEffect(
        resultsNavigationSignal,
        useCallback(
            (action: ResultsNavAction) => {
                if (action === 'open') {
                    openActive();
                } else {
                    move(action);
                }
            },
            [move, openActive]
        )
    );

    return cursor;
}
