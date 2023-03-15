/********************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import {
    Dialog, DialogTitle, DialogContent, Button, DialogContentText, DialogActions,
    Theme, createStyles, withStyles, WithStyles
} from '@material-ui/core';
import { MainContext } from '../context';

const dialogStyles = (theme: Theme) => createStyles({
    dialogContent: {
        color: theme.palette.text.primary
    }
});

export class InfoDialogComponent extends React.Component<InfoDialogComponent.Props> {

    static contextType = MainContext;
    declare context: MainContext;

    handleEnter = (event: KeyboardEvent): void => {
        if (event.code ===  'Enter') {
            this.props.handleCloseDialog();
        }
    };

    componentDidMount(): void {
        document.addEventListener('keydown', this.handleEnter);
    }

    componentWillUnmount(): void {
        document.removeEventListener('keydown', this.handleEnter);
    }

    render(): React.ReactNode {
        return <Dialog
                open={this.props.isInfoDialogOpen}
                onClose={this.props.handleCloseDialog} >
            <DialogTitle>Info</DialogTitle>
            <DialogContent>
                <DialogContentText className={this.props.classes.dialogContent}>
                    {this.props.infoMessage}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button onClick={this.props.handleCloseDialog}>
                    Close
                </Button>
            </DialogActions>
        </Dialog>;
    }
}

export namespace InfoDialogComponent {
    export interface Props extends WithStyles<typeof dialogStyles> {
        infoMessage: string;
        isInfoDialogOpen: boolean;
        handleCloseDialog: () => void;
    }
}

export const InfoDialog = withStyles(dialogStyles)(InfoDialogComponent);
