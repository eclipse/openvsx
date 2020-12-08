/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useContext, useState } from 'react';
import {
    Box, Typography, Paper, Button, makeStyles, Dialog, DialogContent, DialogContentText, Link
} from '@material-ui/core';
import { UserData, isError, ReportedError } from '../../extension-registry-types';
import { SanitizedMarkdown } from '../../components/sanitized-markdown';
import { Timestamp } from '../../components/timestamp';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { createAbsoluteURL } from '../../utils';
import { MainContext } from '../../context';
import CircularProgress from '@material-ui/core/CircularProgress';

const useStyles = makeStyles(theme => ({
    paper: {
        padding: theme.spacing(2)
    },
    dialogScrollPaper: {
        height: '75%',
        width: '100%'
    }
}));

export const UserPublisherAgreement: FunctionComponent<UserPublisherAgreement.Props> = props => {
    const classes = useStyles();
    const { service, pageSettings, updateUser, handleError } = useContext(MainContext);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [working, setWorking] = useState(false);

    const signPublisherAgreement = async (): Promise<void> => {
        try {
            setWorking(true);
            const result = await service.signPublisherAgreement();
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
                const agreementMd = await service.getStaticContent(agreementURL);
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
        <Paper classes={{ root: classes.paper }} elevation={3}>
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
            onEntered={onDialogOpened}
            onEscapeKeyDown={onClose}
            onBackdropClick={onClose}
            maxWidth='md'
            classes={{ paperScrollPaper: classes.dialogScrollPaper }}>
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

export namespace UserPublisherAgreement {
    export interface Props {
        user: UserData;
    }
}
