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
import { UserData } from '../../extension-registry-types';
import { Box, Typography, Paper, Button, makeStyles, Dialog, DialogContent, DialogContentText, Fade } from '@material-ui/core';
import { Timestamp } from '../../components/timestamp';
import { handleError } from '../../utils';
import { ServiceContext, PageSettingsContext } from '../../default/default-app';
import { ErrorHandlerContext } from '../../main';
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
    const service = useContext(ServiceContext);
    const pageSettings = useContext(PageSettingsContext);
    const errorHandler = useContext(ErrorHandlerContext);
    const [dialogOpen, setDialogOpen] = useState(false);

    const [user, setUser] = useState(userDefault);
    const callOAuth = () => {
        // window.open(createAbsoluteURL([service.serverUrl, 'oauth2', 'authorization', 'eclipse']));
        // **** This is mock stuff TODO: delete later **** //
        const additionalUser: UserData = Object.assign({}, user);
        additionalUser.provider = 'eclipse';
        const newUser = Object.assign({}, user);
        newUser.additionalLogins = [];
        newUser.additionalLogins.push(additionalUser);
        // **** That was mock stuff TODO: delete later **** //
        setUser(newUser);
    };

    const signPublisherAgreement = async (): Promise<void> => {
        try {
            await service.signPublisherAgreement();
            // TODO fetch new user data
            // **** This is mock stuff TODO: delete later **** //
            const newUser = Object.assign({}, user);
            newUser.publisherAgreement = "signed";
            newUser.publisherAgreementTimestamp = new Date().toISOString();
            // **** That was mock stuff TODO: delete later **** //
            setUser(newUser);
            setDialogOpen(false);
        } catch (err) {
            errorHandler ? errorHandler.handleError(err) : handleError(err);
        }
    };

    const openPublisherAgreement = () => {
        setDialogOpen(true);
    };

    const [agreementText, setAgreementText] = useState('');
    const onDialogOpened = async () => {
        const markdownIt = new MarkdownIt({ html: true, breaks: true, linkify: true, typographer: true });
        const agreementURL = pageSettings && pageSettings.urls.publisherAgreement || undefined;
        let agreementMd: string = 'Agreement text not found';
        if (agreementURL) {
            agreementMd = await service.getStaticContent(agreementURL);
            setAgreementText(markdownIt.render(agreementMd));
        }
    };

    const onClose = () => {
        setDialogOpen(false);
    };

    useEffect(() => {
        if (user.publisherAgreement !== 'signed' && user.additionalLogins && user.additionalLogins.find(login => login.provider === 'eclipse')) {
            setDialogOpen(true);
        }
    }, [user]);

    return <>
        <Paper classes={{ root: classes.paper }} elevation={3}>
            {
                !user.publisherAgreement ?
                    null :
                    user.publisherAgreement === 'signed' ?
                        <Typography variant='body1'>
                            {
                                user.publisherAgreementTimestamp
                                    ? <>You signed the Eclipse publisher agreement <Timestamp value={user.publisherAgreementTimestamp} />.</>
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
                                    <Button onClick={callOAuth} variant='outlined' color='secondary'>
                                        Log in with Eclipse
                                    </Button>
                                </Box>
                            </>
                            :
                            <>
                                <Typography variant='body1'>
                                    You need to sign a publisher agreement before you can publish any extension to this registry.
                                </Typography>
                                <Box mt={2} display='flex' justifyContent='flex-end'>
                                    <Button onClick={openPublisherAgreement} variant='outlined' color='secondary'>
                                        Show publisher agreement
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