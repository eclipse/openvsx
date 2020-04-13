/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
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

    render() {
        return (
            <Dialog open={this.props.isErrorDialogOpen}>
                <DialogTitle>Error</DialogTitle>
                <DialogContent>
                    <DialogContentText>
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
        errorMessage: string
        isErrorDialogOpen: boolean
        handleCloseDialog: any
    }
}