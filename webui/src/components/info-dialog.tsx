/********************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, Button, DialogContentText, DialogActions } from '@mui/material';

export const InfoDialog: FunctionComponent<InfoDialogProps> = props => {

    useEffect(() => {
        document.addEventListener('keydown', handleEnter);
        return () => document.removeEventListener('keydown', handleEnter);
    }, []);

    const handleEnter = (event: KeyboardEvent): void => {
        if (event.code ===  'Enter') {
            props.handleCloseDialog();
        }
    };

    return <Dialog
        open={props.isInfoDialogOpen}
        onClose={props.handleCloseDialog} >
        <DialogTitle>Info</DialogTitle>
        <DialogContent>
            <DialogContentText sx={{ color: 'text.primary' }}>
                {props.infoMessage}
            </DialogContentText>
        </DialogContent>
        <DialogActions>
            <Button onClick={props.handleCloseDialog}>
                Close
            </Button>
        </DialogActions>
    </Dialog>;
};

export interface InfoDialogProps {
    infoMessage: string;
    isInfoDialogOpen: boolean;
    handleCloseDialog: () => void;
}