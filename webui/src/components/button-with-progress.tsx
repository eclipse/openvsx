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
import { Button, Theme, CircularProgress, createStyles } from '@material-ui/core';
import { withStyles, WithStyles } from '@material-ui/styles';

const buttonStyles = (theme: Theme) => createStyles({
    buttonProgress: {
        color: theme.palette.secondary.main,
        position: 'absolute',
        top: '50%',
        left: '50%',
        marginTop: -12,
        marginLeft: -12,
    },
    buttonWrapper: {
        position: 'relative'
    }
});

export class ButtonComponent extends React.Component<ButtonComponent.Props> {
    render(): React.ReactNode {
        return <div className={this.props.classes.buttonWrapper}>
            <Button
                variant='contained'
                color='secondary'
                disabled={this.props.working || this.props.error}
                autoFocus={this.props.autoFocus}
                onClick={this.props.onClick}
                title={this.props.title}
                disableTouchRipple={true} >
                {this.props.children}
            </Button>
            {
                this.props.working ?
                <CircularProgress size={24} className={this.props.classes.buttonProgress} />
                : null
            }
        </div>;
    }
}

export namespace ButtonComponent {
    export interface Props extends WithStyles<typeof buttonStyles> {
        working: boolean;
        error?: boolean;
        autoFocus?: boolean;
        onClick: React.MouseEventHandler<HTMLButtonElement>;
        title?: string;
    }
}

export const ButtonWithProgress = withStyles(buttonStyles)(ButtonComponent);
