/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, PropsWithChildren } from 'react';
import { Drawer, List } from '@mui/material';
import { Theme } from '@mui/material/styles';

export interface SidepanelProps {}

export const Sidepanel: FunctionComponent<PropsWithChildren<SidepanelProps>> = props => {
    return (
        <Drawer
            variant='permanent'
            PaperProps={{ elevation: 3 }}
            sx={(theme: Theme) => ({
                '& .MuiDrawer-paper': {
                    position: 'relative',
                    justifyContent: 'space-between',
                    transition: theme.transitions.create('width', {
                        easing: theme.transitions.easing.sharp,
                        duration: theme.transitions.duration.enteringScreen
                    }),
                    overflowX: { xs: 'hidden', sm: 'hidden', md: 'none', lg: 'none', xl: 'none' },
                    width: { xs: theme.spacing(7) + 1, sm: theme.spacing(9) + 1, md: 240 },
                }
            })}
        >
            <List>
                {props.children}
            </List>
        </Drawer>
    );
};