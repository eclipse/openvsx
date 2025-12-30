/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, ReactNode, useContext, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, Button, DialogContentText, DialogActions, Box, Link } from '@mui/material';
import { MainContext } from '../context';
import { styled, Theme } from '@mui/material/styles';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { hideError, selectError } from '../store/error';

const ErrorLink = styled(Link)(({ theme }: { theme: Theme }) => ({
    textDecoration: 'underline',
    color: theme.palette.primary.contrastText
}));

export const ErrorDialog: FunctionComponent = () => {
    const dispatch = useAppDispatch();
    const error = useAppSelector(selectError);

    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => document.removeEventListener('keydown', handleEnter);
    }, []);

    const handleEnter = (event: KeyboardEvent): void => {
        if (event.code ===  'Enter') {
            handleCloseDialog();
        }
    };

    const handleCloseDialog = () => dispatch(hideError());

    const getContentForCode = (): ReactNode => {
        switch (error.code) {
            case 'eclipse-missing-github-id':
                return <>
                    Please fill in the &ldquo;GitHub Username&rdquo; field
                    in <ErrorLink href='https://accounts.eclipse.org/user/edit' target='_blank'>
                        your Eclipse account
                    </ErrorLink> and try again.
                </>;
            case 'eclipse-mismatch-github-id':
                return <>
                    Please edit the &ldquo;GitHub Username&rdquo; field
                    in <ErrorLink href='https://accounts.eclipse.org/user/edit' target='_blank'>
                        your Eclipse account
                    </ErrorLink> or log in with a different GitHub account.
                </>;
            case 'publisher-agreement-problem':
                return <>
                    Please contact <ErrorLink href='mailto:webmaster@eclipse.org?subject=Problem%20With%20open-vsx.org%20Publisher%20Agreement' >
                        webmaster@eclipse.org
                    </ErrorLink> if this problem persists.
                </>;
            default:
                return null;
        }
    };

    const context = useContext(MainContext);
    const codeContent = getContentForCode();
    return <Dialog
            open={error.show}
            onClose={handleCloseDialog} >
        <DialogTitle>Error</DialogTitle>
        <DialogContent>
            <DialogContentText sx={{ color: context.pageSettings.themeType === 'dark' ? '#ff849e' : '#c54a64' }}>
                {error.message}
                {
                    codeContent ?
                    <Box mt={2}>
                        {codeContent}
                    </Box>
                    : null
                }
            </DialogContentText>
        </DialogContent>
        <DialogActions>
            <Button onClick={handleCloseDialog}>
                Close
            </Button>
        </DialogActions>
    </Dialog>;
};