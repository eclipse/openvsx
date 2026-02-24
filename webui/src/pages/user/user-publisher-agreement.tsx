/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, useContext, useState, useEffect, useRef, ReactNode } from 'react';
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
    const [agreementText, setAgreementText] = useState('');
    const abortController = useRef<AbortController>(new AbortController());

    useEffect(() => {
        return () => {
            abortController.current.abort();
        };
    }, []);

    useEffect(() => {
        if (dialogOpen) {
            onDialogOpened();
        }
    }, [dialogOpen]);

    const closePublisherAgreement = () => {
        setDialogOpen(false);
    };

    const signPublisherAgreement = async (): Promise<void> => {
        try {
            setWorking(true);
            const result = await service.signPublisherAgreement(abortController.current);
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
        if (!pageSettings.urls.publisherAgreement) {
            handleError({ error: 'Publisher agreement text is not available.' });
        } else {
            setDialogOpen(true);
        }
    };

    const onDialogOpened = async () => {
        const agreementURL = pageSettings.urls.publisherAgreement;
        if (agreementURL) {
            try {
                const agreementMd = await service.getStaticContent(abortController.current, agreementURL);
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

    const publisherAgreementName = pageSettings?.publisherAgreement?.name ?? '';
    const publisherAgreementSigned = user.publisherAgreement.status == 'signed';

    let content: ReactNode;
    if (publisherAgreementSigned) {
        content = <Box display='flex' justifyContent='space-between' alignItems='start'>
            <Typography variant='body1'>
            {
                user.publisherAgreement.timestamp
                    ? <>You signed the {publisherAgreementName} Publisher Agreement <Timestamp value={user.publisherAgreement.timestamp} />.</>
                    : <>You signed the {publisherAgreementName} Publisher Agreement.</>
            }
            </Typography>
            <Box display='flex' justifyContent='flex-end'>
                <Button onClick={openPublisherAgreement} variant='outlined' color='secondary'>
                    Show Publisher Agreement
                </Button>
            </Box>
        </Box>;
    } else if (user.additionalLogins?.find(login => login.provider === 'eclipse')) {
        content = <>
            <Typography variant='body1'>
                You need to sign the {publisherAgreementName} Publisher Agreement before you can publish
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
                You need to sign the {publisherAgreementName} Publisher Agreement before you can publish
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
        </>;
    }

    return <>
        <Paper sx={{ p: 2 }} elevation={3}>{content}</Paper>
        <Dialog
            open={dialogOpen}
            onClose={onClose}
            maxWidth='xl'
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
                                <Button onClick={closePublisherAgreement}>
                                    Close
                                </Button>
                                { !publisherAgreementSigned &&
                                    <ButtonWithProgress working={working} onClick={signPublisherAgreement}>
                                        Agree
                                    </ButtonWithProgress>
                                }
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