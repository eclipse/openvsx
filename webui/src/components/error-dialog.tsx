/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { FunctionComponent, ReactNode, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, Button, DialogContentText, DialogActions, Box } from '@mui/material';

export const ErrorDialog: FunctionComponent<ErrorDialogProps> = props => {
    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => document.removeEventListener('keydown', handleEnter);
    }, []);

    const handleEnter = (event: KeyboardEvent): void => {
        if (event.code === 'Enter') {
            props.handleCloseDialog();
        }
    };

    const getContentForCode = (): ReactNode => {
        if (!props.errorCode) {
            return null;
        }

        switch (props.errorCode) {
            case 'publisher-agreement-problem':
                return <>Please contact the administrators of this registry if this problem persists.</>;
            default:
                return null;
        }
    };

    const codeContent = getContentForCode();
    return (
        <Dialog open={props.isErrorDialogOpen} onClose={props.handleCloseDialog}>
            <DialogTitle>Error</DialogTitle>
            <DialogContent>
                <DialogContentText sx={{ color: theme => theme.palette.error.main }}>
                    {props.errorMessage}
                    {codeContent ? <Box mt={2}>{codeContent}</Box> : null}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button onClick={props.handleCloseDialog}>Close</Button>
            </DialogActions>
        </Dialog>
    );
};

export interface ErrorDialogProps {
    errorMessage: string;
    errorCode?: number | string;
    isErrorDialogOpen: boolean;
    handleCloseDialog: () => void;
}
