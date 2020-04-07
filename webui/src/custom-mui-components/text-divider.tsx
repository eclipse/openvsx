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
    lightTheme: {
        backgroundColor: theme.palette.primary.contrastText
    },
    darkTheme: {
        backgroundColor: theme.palette.secondary.contrastText
    }
});

export class TextDividerComponent extends React.Component<TextDividerComponent.Props> {
    render() {
        const themeClass = this.props.theme === 'dark' ? this.props.classes.darkTheme : this.props.classes.lightTheme;
        return <Divider orientation='vertical' classes={{
            root: `${this.props.classes.root} ${themeClass}`
        }} />;
    }
}

export namespace TextDividerComponent {
    export interface Props extends WithStyles<typeof dividerStyles> {
        theme?: 'light' | 'dark';
    }
}

export const TextDivider = withStyles(dividerStyles)(TextDividerComponent);
