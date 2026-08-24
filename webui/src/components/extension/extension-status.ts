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

import { Extension } from '../../extension-registry-types';

export interface ExtensionStatus {
    label: string;
    /** Palette path for `sx` (e.g. `color: status.color`). */
    color: string;
}

/** Most relevant publishing state of an extension, or undefined when it's simply public. */
export const getExtensionStatus = (extension: Extension): ExtensionStatus | undefined => {
    // A removed extension is a tombstone, so it outranks every other state.
    if (extension.removed) {
        return { label: 'Deleted', color: 'error.main' };
    }
    // Usually the reason the extension is deactivated in the first place, so it outranks that and
    // every review-related state below: it names the actual, actionable cause.
    if (extension.namespaceOwnershipConflict) {
        return { label: 'Namespace not verified', color: 'warningAccent' };
    }
    switch (extension.reviewStatus) {
        case 'rejected':
            return { label: 'Rejected', color: 'error.main' };
        case 'under_review':
            return { label: 'Under review', color: 'warningAccent' };
    }
    if (extension.active === false) {
        return { label: 'Deactivated', color: 'text.disabled' };
    }
    if (extension.deprecated) {
        return { label: 'Deprecated', color: 'warningAccent' };
    }
    return undefined;
};
