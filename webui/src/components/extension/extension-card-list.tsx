/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { DelayedLoadIndicator } from '../delayed-load-indicator';
import { ExtensionCardListItem } from './extension-card-list-item';
import { Extension } from '../../extension-registry-types';

interface ExtensionCardListProps {
    extensions?: Extension[];
    loading: boolean;
    canDelete?: boolean;
    // Base route each card links to; forwarded to the list items. Supplied by the caller.
    routePrefix: string;
}

export const ExtensionCardList: FunctionComponent<ExtensionCardListProps> = props => {
    return (
        <Box
            component='div'
            sx={{
                display: 'grid',
                gridTemplateColumns: `repeat(auto-fit, minmax(300px, 1fr))`,
                gap: '.5rem',
                mt: '1rem',
                mb: '1rem'
            }}>
            <DelayedLoadIndicator loading={props.loading} />
            {props.extensions && props.extensions.length > 0
                ? props.extensions.map((extension: Extension) => (
                      <ExtensionCardListItem
                          key={`${extension.namespace}.${extension.name}-${extension.version}`}
                          extension={extension}
                          canDelete={props.canDelete}
                          routePrefix={props.routePrefix}
                      />
                  ))
                : null}
        </Box>
    );
};
