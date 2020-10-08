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
import { Typography, makeStyles } from '@material-ui/core';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { UserNamespaceExtensionListItem } from './user-namespace-extension-list-item';
import { Extension } from '../../extension-registry-types';


const extensionListStyles = makeStyles(theme => ({
    extensions: {
        display: 'grid',
        gridTemplateColumns: `repeat(auto-fit, minmax(200px, 1fr))`,
        gap: `.5rem`,
        marginTop: '1rem',
    }
}));

interface UserExtensionListProps {
    extensions?: Extension[];
    loading: boolean;
}

export const UserExtensionList: FunctionComponent<UserExtensionListProps> = props => {
    const classes = extensionListStyles();
    return <>
        <Typography variant='h5'>Extensions</Typography>
        <div className={classes.extensions}>
            <DelayedLoadIndicator loading={props.loading} />
            {
                props.extensions ? props.extensions.map((extension: Extension, i: number) => <UserNamespaceExtensionListItem
                    key={`${i}${extension.displayName}`}
                    extension={extension}
                />) : null
            }
        </div>
    </>;
};

