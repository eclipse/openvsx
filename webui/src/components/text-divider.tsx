/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";
import { Theme, createStyles, WithStyles, withStyles } from "@material-ui/core/styles";
import { Divider } from "@material-ui/core";


const dividerStyles = (theme: Theme) => createStyles({
    root: {
        alignSelf: 'center',
        height: '1em',
        margin: `0 ${theme.spacing(1)}px`
    },
    collapseSmall: {
        [theme.breakpoints.down('sm')]: {
            height: 0,
            margin: `${theme.spacing(0.25)}px`
        }
    },
    lightTheme: {
        backgroundColor: '#333'
    },
    darkTheme: {
        backgroundColor: '#edf5ea'
    }
});

export class TextDividerComponent extends React.Component<TextDividerComponent.Props> {
    render(): React.ReactNode {
        const classes = [this.props.classes.root];
        if (this.props.themeType === 'dark') {
            classes.push(this.props.classes.darkTheme);
        } else if (this.props.themeType === 'light') {
            classes.push(this.props.classes.lightTheme);
        }
        if (this.props.collapseSmall) {
            classes.push(this.props.classes.collapseSmall);
        }
        return <Divider orientation='vertical' classes={{ root: classes.join(' ') }} />;
    }
}

export namespace TextDividerComponent {
    export interface Props extends WithStyles<typeof dividerStyles> {
        themeType?: 'light' | 'dark';
        collapseSmall?: boolean;
    }
}

export const TextDivider = withStyles(dividerStyles)(TextDividerComponent);
