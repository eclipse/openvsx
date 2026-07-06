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

import { FunctionComponent } from 'react';
import { Chip, Stack } from '@mui/material';
import { Extension } from '../../extension-registry-types';

export const ExtensionStatusChips: FunctionComponent<ExtensionStatusChipsProps> = ({ extension }) => (
    <Stack direction='row' spacing={1} mb={2}>
        {extension.deprecated && <Chip label='Deprecated' color='warning' size='small' />}
        {extension.active === false && <Chip label='Deactivated' size='small' />}
        {extension.reviewStatus === 'under_review' && <Chip label='Under Review' color='info' size='small' />}
        {extension.reviewStatus === 'rejected' && <Chip label='Rejected' color='error' size='small' />}
    </Stack>
);

export interface ExtensionStatusChipsProps {
    extension: Extension;
}
