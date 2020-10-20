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
import { PublisherInfo } from '../../extension-registry-types';
import { UserSettingsProfile } from '../user/user-settings-profile';
import { UserExtensionList } from '../user/user-extension-list';
import { Box, makeStyles, Typography } from '@material-ui/core';
import { PublisherRevokeDialog } from './publisher-revoke-dialog';

const useStyles = makeStyles(theme => ({
    buttonContainer: {
        marginTop: theme.spacing(2),
        display: 'flex',
        justifyContent: 'flex-end'
    }
}));

interface PublisherDetailsProps {
    publisherInfo: PublisherInfo;
}

export const PublisherDetails: FunctionComponent<PublisherDetailsProps> = props => {
    const classes = useStyles();

    return <>
        <UserSettingsProfile user={props.publisherInfo.user} />
        <Typography>
            {props.publisherInfo.activeAccessTokenNum} active access token{props.publisherInfo.activeAccessTokenNum !== 1 ? 's' : ''}.
        </Typography>
        {props.publisherInfo.extensions.length ? <UserExtensionList extensions={props.publisherInfo.extensions} loading={false} /> : ''}
        <Box className={classes.buttonContainer}>
            <PublisherRevokeDialog publisherInfo={props.publisherInfo} />
        </Box>
    </>;
};