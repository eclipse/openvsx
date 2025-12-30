/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useState, ReactNode } from 'react';
import {
    Box, Typography, Paper, Button, Dialog, DialogContent, DialogContentText, Link
} from '@mui/material';
import { SanitizedMarkdown } from '../../components/sanitized-markdown';
import { Timestamp } from '../../components/timestamp';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { MainContext } from '../../context';
import CircularProgress from '@mui/material/CircularProgress';
import { eclipseLoginUrl, useGetStaticContentQuery, useSignPublisherAgreementMutation } from '../../store/api';
import { UserData } from '../../extension-registry-types';

export const UserPublisherAgreement: FunctionComponent<UserPublisherAgreementProps> = ({ user }) => {
    const { pageSettings, handleError } = useContext(MainContext);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [working, setWorking] = useState(false);
    const { data: agreementText } = useGetStaticContentQuery(pageSettings.urls.publisherAgreement as string, { skip: pageSettings.urls.publisherAgreement == null });
    const [signPublisherAgreement] = useSignPublisherAgreementMutation();

    const onSignPublisherAgreement = async (): Promise<void> => {
        setWorking(true);
        await signPublisherAgreement();
        setDialogOpen(false);
        setWorking(false);
    };

    const openPublisherAgreement = () => {
        if (!pageSettings.urls.publisherAgreement) {
            handleError({ error: 'Publisher agreement text is not available.' });
        } else {
            setDialogOpen(true);
        }
    };

    const onClose = () => {
        setDialogOpen(false);
    };

    if (!user?.publisherAgreement) {
        return null;
    }

    let content: ReactNode;
    if (user.publisherAgreement.status === 'signed') {
        content = <Typography variant='body1'>
            {
                user.publisherAgreement.timestamp
                    ? <>You signed the Eclipse Foundation Open VSX Publisher Agreement <Timestamp value={user.publisherAgreement.timestamp} />.</>
                    : 'You signed the Eclipse Foundation Open VSX Publisher Agreement.'
            }
        </Typography>;
    } else if (user.additionalLogins?.find(login => login.provider === 'eclipse')) {
        content = <>
            <Typography variant='body1'>
                You need to sign the Eclipse Foundation Open VSX Publisher Agreement before you can publish
                any extension to this registry.
            </Typography>
            <Box mt={2} display='flex' justifyContent='flex-end'>
                <Button onClick={openPublisherAgreement} variant='outlined' color='secondary'>
                    Show Publisher Agreement
                </Button>
            </Box>
        </>;
    } else {
        content = <>
            <Typography variant='body1'>
                You need to sign the Eclipse Foundation Open VSX Publisher Agreement before you can publish
                any extension to this registry. To start the signing process, please log in with
                an Eclipse Foundation account.
            </Typography>
            <Box mt={2} display='flex' justifyContent='flex-end'>
                <Link href={eclipseLoginUrl}>
                    <Button variant='outlined' color='secondary'>
                        Log in with Eclipse
                    </Button>
                </Link>
            </Box>
        </>;
    }

    return <>
        <Paper sx={{ p: 2 }} elevation={3}>{content}</Paper>
        <Dialog
            open={dialogOpen}
            onClose={onClose}
            maxWidth='md'
            sx={{ paperScrollPaper: { height: '75%', width: '100%' } }}>
            <DialogContent>
                {
                    agreementText ?
                        <DialogContentText component='div'>
                            <SanitizedMarkdown
                                content={agreementText}
                                sanitize={false}
                                linkify={false} />
                            <Box display='flex' justifyContent='flex-end' >
                                <ButtonWithProgress working={working} onClick={onSignPublisherAgreement}>
                                    Agree
                                </ButtonWithProgress>
                            </Box>
                        </DialogContentText>
                        :
                        <Box height={1} display='flex' justifyContent='center' alignItems='center'>
                            <CircularProgress color='secondary' />
                        </Box>
                }
            </DialogContent>
        </Dialog>
    </>;

};

export interface UserPublisherAgreementProps {
    user: UserData;
}