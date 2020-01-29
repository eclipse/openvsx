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
        height: '0.7em',
        margin: `0 ${theme.spacing(1)}px`,
        backgroundColor: theme.palette.primary.contrastText
    }
});

export class TextDividerComp extends React.Component<WithStyles<typeof dividerStyles>> {
    render() {
        return <Divider orientation='vertical' classes={{root: this.props.classes.root}} />;
    }
}

export const TextDivider = withStyles(dividerStyles)(TextDividerComp);
