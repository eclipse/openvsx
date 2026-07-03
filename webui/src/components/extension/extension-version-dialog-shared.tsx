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

import { ChangeEvent } from 'react';
import { TargetPlatformVersion } from '../../extension-registry-types';

export const VERSION_DIALOG_WILDCARD = '*';

export const buildVersionDialogItems = (targetPlatforms: string[]): TargetPlatformVersion[] => [
    { targetPlatform: VERSION_DIALOG_WILDCARD, version: VERSION_DIALOG_WILDCARD, checked: true },
    ...targetPlatforms.map(tp => ({ targetPlatform: tp, version: VERSION_DIALOG_WILDCARD, checked: true }))
];

export const handleVersionDialogChange = (
    event: ChangeEvent<HTMLInputElement>,
    prev: TargetPlatformVersion[]
): TargetPlatformVersion[] => {
    const { name, checked } = event.target;
    if (name === VERSION_DIALOG_WILDCARD) {
        return prev.map(item => ({ ...item, checked }));
    }
    const next = prev.map(item => (item.targetPlatform === name ? { ...item, checked } : item));
    const allChecked = next.filter(i => i.targetPlatform !== VERSION_DIALOG_WILDCARD).every(i => i.checked);
    return next.map(i => (i.targetPlatform === VERSION_DIALOG_WILDCARD ? { ...i, checked: allChecked } : i));
};
