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
import {
    Dialog, DialogTitle, DialogContent, Button, DialogContentText, DialogActions, Box, Link,
    Theme, createStyles, withStyles, WithStyles
} from '@material-ui/core';
import { MainContext } from '../context';

const dialogStyles = (theme: Theme) => createStyles({
    lightTheme: {
        color: '#c54a64'
    },
    darkTheme: {
        color: '#ff849e'
    },
    link: {
        textDecoration: 'underline',
        color: theme.palette.primary.contrastText
    }
});

export class ErrorDialogComponent extends React.Component<ErrorDialogComponent.Props> {

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
        const codeContent = this.getContentForCode();
        const themeClass = this.context.pageSettings.themeType === 'dark'
            ? this.props.classes.darkTheme : this.props.classes.lightTheme;
        return <Dialog
                open={this.props.isErrorDialogOpen}
                onClose={this.props.handleCloseDialog} >
            <DialogTitle>Error</DialogTitle>
            <DialogContent>
                <DialogContentText
                    className={themeClass} >
                    {this.props.errorMessage}
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
                <Button onClick={this.props.handleCloseDialog}>
                    Close
                </Button>
            </DialogActions>
        </Dialog>;
    }

    protected getContentForCode(): React.ReactNode {
        if (!this.props.errorCode) {
            return null;
        }
        const classes = this.props.classes;
        switch (this.props.errorCode) {
            case 'eclipse-missing-github-id':
                return <>
                    Please fill in the &ldquo;GitHub Username&rdquo; field
                    in <Link href='https://accounts.eclipse.org/user/edit' target='_blank' className={classes.link}>
                        your Eclipse account
                    </Link> and try again.
                </>;
            case 'eclipse-mismatch-github-id':
                return <>
                    Please edit the &ldquo;GitHub Username&rdquo; field
                    in <Link href='https://accounts.eclipse.org/user/edit' target='_blank' className={classes.link}>
                        your Eclipse account
                    </Link> or log in with a different GitHub account.
                </>;
            case 'publisher-agreement-problem':
                return <>
                    Please contact <Link
                        href='mailto:webmaster@eclipse.org?subject=Problem%20With%20open-vsx.org%20Publisher%20Agreement'
                        className={classes.link} >
                        webmaster@eclipse.org
                    </Link> if this problem persists.
                </>;
            default:
                return null;
        }
    }
}

export namespace ErrorDialogComponent {
    export interface Props extends WithStyles<typeof dialogStyles> {
        errorMessage: string;
        errorCode?: number | string;
        isErrorDialogOpen: boolean;
        handleCloseDialog: () => void;
    }
}

export const ErrorDialog = withStyles(dialogStyles)(ErrorDialogComponent);
