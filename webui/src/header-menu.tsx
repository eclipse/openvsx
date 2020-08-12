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
import { Menu, IconButton } from '@material-ui/core';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import MenuIcon from '@material-ui/icons/Menu';
import { PageSettings } from './page-settings';

export const HeaderMenu: React.FunctionComponent<{ pageSettings: PageSettings; }> = ({ pageSettings }) => {
    const {
        defaultMenuContent: DefaultMenuContent,
        mobileMenuContent: MobileMenuContent
    } = pageSettings.elements;
    const isMobile = useMediaQuery('(max-width: 700px)');
    if (isMobile && MobileMenuContent) {
        return <MobileHeaderMenu menuContent={MobileMenuContent} />;
    } else if (DefaultMenuContent) {
        return <DefaultMenuContent />;
    } else {
        return null;
    }
};

export const MobileHeaderMenu: React.FunctionComponent<{ menuContent: React.ComponentType; }> = ({ menuContent }) => {
    const [isOpen, setOpen] = React.useState(false);
    const [anchorEl, setAnchorEl] = React.useState<Element | null>(null);
    const MenuContent = menuContent;
    return <React.Fragment>
        <IconButton
            title='Menu'
            aria-label='Menu'
            onClick={event => {
                setAnchorEl(event.currentTarget);
                setOpen(!isOpen);
            }} >
            <MenuIcon />
        </IconButton>
        <Menu
            open={isOpen}
            anchorEl={anchorEl}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            onClose={() => setOpen(false)} >
            <MenuContent />
        </Menu>
    </React.Fragment>;
};
