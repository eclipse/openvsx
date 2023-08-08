/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { Divider } from "@mui/material";

export const TextDivider: FunctionComponent<TextDividerProps> = props => {
    const height =  props.collapseSmall ? { xs: 0, sm: 0, md: '1em', lg: '1em', xl: '1em' } : '1em';
    const marginX = props.collapseSmall ? { xs: 0.25, sm: 0.25, md: 1, lg: 1, xl: 1 } : 1;
    const marginY = props.collapseSmall ? { xs: 0.25, sm: 0.25, md: 0, lg: 0, xl: 0 } : 0;
    const backgroundColor = props.themeType === 'dark' ? '#edf5ea' : '#151515';
    return <Divider
        orientation='vertical'
        sx={{
            alignSelf: 'center',
            height: height,
            mx: marginX,
            my: marginY,
            bgcolor: backgroundColor
        }}
    />;
};

export interface TextDividerProps {
    themeType?: 'light' | 'dark';
    collapseSmall?: boolean;
}