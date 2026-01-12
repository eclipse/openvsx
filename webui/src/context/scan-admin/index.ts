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

// Context and Provider
export { ScanProvider, useScanContext } from './scan-context';

// Context Types
export type { ScanContextValue, ScanActions, DerivedData, ScanProviderProps } from './scan-context-types';

// Domain Types
export * from './scan-types';

// Reducer
export { scanReducer } from './scan-reducer';

// Helpers
export { getDateRangeParams, getFileDateRange } from './scan-helpers';

// API Effects (for testing or advanced use cases)
export {
    useFilterOptionsEffect,
    useScansEffect,
    useScanCountsEffect,
    useFilesEffect,
    useFileCountsEffect,
    useAutoRefreshEffect,
} from './scan-api-effects';

// API Actions (for testing or advanced use cases)
export { useConfirmAction, useFileAction } from './scan-api-actions';

// Actions Factory (for testing or advanced use cases)
export { useScanActions } from './scan-actions';
