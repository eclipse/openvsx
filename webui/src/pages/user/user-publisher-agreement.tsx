/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useState, useEffect } from 'react';
import {
    Box, Typography, Paper, Button, Dialog, DialogContent, DialogContentText, Link
} from '@mui/material';
import { UserData, isError, ReportedError } from '../../extension-registry-types';
import { SanitizedMarkdown } from '../../components/sanitized-markdown';
import { Timestamp } from '../../components/timestamp';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { createAbsoluteURL } from '../../utils';
import { MainContext } from '../../context';
import CircularProgress from '@mui/material/CircularProgress';

export const UserPublisherAgreement: FunctionComponent<UserPublisherAgreementProps> = props => {
    const { service, pageSettings, updateUser, handleError } = useContext(MainContext);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [working, setWorking] = useState(false);
    const abortController = new AbortController();
    useEffect(() => {
        return () => {
            abortController.abort();
        };
    }, []);

    useEffect(() => {
        if (dialogOpen) {
            onDialogOpened();
        }
    }, [dialogOpen]);

    const signPublisherAgreement = async (): Promise<void> => {
        try {
            setWorking(true);
            const result = await service.signPublisherAgreement(abortController);
            if (isError(result)) {
                throw result;
            }
            updateUser();
            setDialogOpen(false);
        } catch (err) {
            if (!(err as ReportedError).code) {
                Object.assign(err, { code: 'publisher-agreement-problem' });
            }
            handleError(err);
        } finally {
            setWorking(false);
        }
    };

    const openPublisherAgreement = () => {
        if (!pageSettings || !pageSettings.urls.publisherAgreement) {
            handleError({ error: 'Publisher agreement text is not available.' });
        } else {
            setDialogOpen(true);
        }
    };

    const [agreementText, setAgreementText] = useState('');
    const onDialogOpened = async () => {
        const agreementURL = pageSettings.urls.publisherAgreement;
        if (agreementURL) {
            try {
                const agreementMd = await service.getStaticContent(abortController, agreementURL);
                setAgreementText(agreementMd);
            } catch (err) {
                handleError(err);
            }
        } else {
            setAgreementText('Publisher agreement text is not available.');
        }
    };

    const onClose = () => {
        setDialogOpen(false);
    };

    const user = props.user;
    if (!user.publisherAgreement) {
        return null;
    }
    return <>
        <Paper sx={{ p: 2 }} elevation={3}>
            {
                user.publisherAgreement.status === 'signed' ?
                    <Typography variant='body1'>
                        {
                            user.publisherAgreement.timestamp
                                ? <>You signed the Eclipse Foundation Open VSX Publisher Agreement <Timestamp value={user.publisherAgreement.timestamp} />.</>
                                : 'You signed the Eclipse Foundation Open VSX Publisher Agreement.'
                        }
                    </Typography>
                    :
                    !user.additionalLogins || !user.additionalLogins.find(login => login.provider === 'eclipse') ?
                        <>
                            <Typography variant='body1'>
                                You need to sign the Eclipse Foundation Open VSX Publisher Agreement before you can publish
                                any extension to this registry. To start the signing process, please log in with
                                an Eclipse Foundation account.
                            </Typography>
                            <Box mt={2} display='flex' justifyContent='flex-end'>
                                <Link href={createAbsoluteURL([service.serverUrl, 'oauth2', 'authorization', 'eclipse'])}>
                                    <Button variant='outlined' color='secondary'>
                                        Log in with Eclipse
                                    </Button>
                                </Link>
                            </Box>
                        </>
                        :
                        <>
                            <Typography variant='body1'>
                                You need to sign the Eclipse Foundation Open VSX Publisher Agreement before you can publish
                                any extension to this registry.
                            </Typography>
                            <Box mt={2} display='flex' justifyContent='flex-end'>
                                <Button onClick={openPublisherAgreement} variant='outlined' color='secondary'>
                                    Show Publisher Agreement
                                </Button>
                            </Box>
                        </>}
        </Paper>
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
                                <ButtonWithProgress working={working} onClick={signPublisherAgreement}>
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