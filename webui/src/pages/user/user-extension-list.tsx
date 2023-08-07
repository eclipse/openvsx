/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { UserNamespaceExtensionListItem } from './user-namespace-extension-list-item';
import { Extension } from '../../extension-registry-types';

interface UserExtensionListProps {
    extensions?: Extension[];
    loading: boolean;
}

export const UserExtensionList: FunctionComponent<UserExtensionListProps> = props => {
    return <Box
        component='div'
        sx={{
            display: 'grid',
            gridTemplateColumns: `repeat(auto-fit, minmax(200px, 1fr))`,
            gap: '.5rem',
            mt: '1rem'
        }}
    >
        <DelayedLoadIndicator loading={props.loading} />
        {
            props.extensions && props.extensions.length > 0 ?
            props.extensions.map((extension: Extension) => <UserNamespaceExtensionListItem
                key={`${extension.namespace}.${extension.name}-${extension.version}`}
                extension={extension}
            />)
            : null
        }
    </Box>;
};

