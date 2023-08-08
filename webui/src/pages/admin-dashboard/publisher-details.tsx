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
import { Box, Typography } from '@mui/material';
import { PublisherRevokeDialog } from './publisher-revoke-dialog';

interface PublisherDetailsProps {
    publisherInfo: PublisherInfo;
}

export const PublisherDetails: FunctionComponent<PublisherDetailsProps> = props => {
    return <Box mt={2}>
        <UserSettingsProfile user={props.publisherInfo.user} isAdmin={true} />
        <Box mt={2}>
            <Typography variant='h5'>Access Tokens</Typography>
            <Typography variant='body1'>
                {props.publisherInfo.activeAccessTokenNum} active access token{props.publisherInfo.activeAccessTokenNum !== 1 ? 's' : ''}.
            </Typography>
        </Box>
        <Box mt={2}>
            <Typography variant='h5'>Extensions</Typography>
            {
                props.publisherInfo.extensions.length > 0 ?
                <UserExtensionList extensions={props.publisherInfo.extensions} loading={false} />
                :
                <Typography  variant='body1'>This user has not published any extensions.</Typography>
            }
        </Box>
        <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
            <PublisherRevokeDialog publisherInfo={props.publisherInfo} />
        </Box>
    </Box>;
};