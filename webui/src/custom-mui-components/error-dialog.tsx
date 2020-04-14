/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Dialog, DialogTitle, DialogContent, Button, DialogContentText, DialogActions } from '@material-ui/core';

export class ErrorDialog extends React.Component<ErrorDialog.Props, {}> {

    handleEnter = (e: KeyboardEvent) => {
        if (e.code ===  'Enter') {
            this.props.handleCloseDialog();
        }
    }

    componentDidMount() {
        document.addEventListener('keydown', this.handleEnter);
    }

    componentWillUnmount() {
        document.removeEventListener('keydown', this.handleEnter);
    }

    render() {
        return (
            <Dialog
                open={this.props.isErrorDialogOpen}
                onClose={this.props.handleCloseDialog}
            >
                <DialogTitle>Error</DialogTitle>
                <DialogContent>
                    <DialogContentText style={{ color: '#f15374' }}>
                        {this.props.errorMessage}
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.props.handleCloseDialog}>
                        Close
                    </Button>
                </DialogActions>
            </Dialog>
        );
    }
}

export namespace ErrorDialog {
    export interface Props {
        errorMessage: string;
        isErrorDialogOpen: boolean;
        handleCloseDialog: () => void;
    }
}