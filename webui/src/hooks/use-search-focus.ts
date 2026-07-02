/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { useContext } from 'react';
import { SearchFocusContext, SearchFocusContextValue } from '../context/search/search-focus-context';

/**
 * Access the search focus-coordination signals. Emit with `focusSearch()` /
 * `focusResults()`; subscribe to `focusSearchSignal` / `focusResultsSignal`
 * through useSignalEffect.
 */
export function useSearchFocus(): SearchFocusContextValue {
    return useContext(SearchFocusContext);
}
