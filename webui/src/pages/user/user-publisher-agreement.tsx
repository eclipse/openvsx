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
import { UserData, isError } from '../../extension-registry-types';
import { Box, Typography, Paper, Button, makeStyles, Dialog, DialogContent, DialogContentText, Fade, Link } from '@material-ui/core';
import { Timestamp } from '../../components/timestamp';
import { createAbsoluteURL } from '../../utils';
import { MainContext } from '../../context';
import CircularProgress from '@material-ui/core/CircularProgress';
import * as MarkdownIt from 'markdown-it';

const useStyles = makeStyles(theme => ({
    paper: {
        padding: theme.spacing(2)
    },
    dialogScrollPaper: {
        height: '75%',
        width: '550px'
    }
}));

interface UserPublisherAgreementProps {
    user: UserData;
}

export const UserPublisherAgreement: FunctionComponent<UserPublisherAgreementProps> = props => {
    const classes = useStyles();
    const userDefault = props.user;
    const { service, pageSettings, handleError } = useContext(MainContext);
    const [dialogOpen, setDialogOpen] = useState(false);

    const [user, setUser] = useState(userDefault);

    const signPublisherAgreement = async (): Promise<void> => {
        try {
            const newUser = await service.signPublisherAgreement();
            if (isError(newUser)) {
                throw (newUser.error);
            }
            setUser(newUser);
            setDialogOpen(false);
        } catch (err) {
            handleError(err);
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
        const markdownIt = new MarkdownIt({ html: true, breaks: true, linkify: true, typographer: true });
        const agreementURL = pageSettings.urls.publisherAgreement;
        if (agreementURL) {
            try {
                const agreementMd = await service.getStaticContent(agreementURL);
                setAgreementText(markdownIt.render(agreementMd));
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
                                ? <>You signed the Eclipse publisher agreement <Timestamp value={user.publisherAgreement.timestamp} />.</>
                                : 'You signed the Eclipse publisher agreement.'
                        }
                    </Typography>
                    :
                    !user.additionalLogins || !user.additionalLogins.find(login => login.provider === 'eclipse') ?
                        <>
                            <Typography variant='body1'>
                                You need to sign a publisher agreement before you can publish any extension to this registry.
                                To start the signing process, please log in with an Eclipse Foundation account.
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
                                You need to sign a publisher agreement before you can publish any extension to this registry.
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
            classes={{ paperScrollPaper: classes.dialogScrollPaper }}>
            <DialogContent>
                {
                    agreementText ?
                        <DialogContentText component='div'>
                            <span dangerouslySetInnerHTML={{ __html: agreementText }} />
                            <Box display='flex' justifyContent='flex-end' >
                                <Button onClick={signPublisherAgreement} variant='contained' color='secondary'>
                                    Agree
                                    </Button>
                            </Box>
                        </DialogContentText>
                        :
                        <Box height={1} display='flex' justifyContent='center' alignItems='center'>
                            <Fade
                                in={!agreementText}
                                style={{
                                    transitionDelay: !agreementText ? '800ms' : '0ms',
                                }}
                            >
                                <CircularProgress color='secondary' />
                            </Fade>
                        </Box>
                }
            </DialogContent>
        </Dialog>
    </>;

};