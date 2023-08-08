/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ComponentType, FunctionComponent, useContext, useEffect, useState } from 'react';
import { Menu, IconButton, useTheme } from '@mui/material';
import useMediaQuery from '@mui/material/useMediaQuery';
import MenuIcon from '@mui/icons-material/Menu';
import { MainContext } from './context';
import { useLocation } from 'react-router-dom';

export const HeaderMenu: FunctionComponent = () => {
    const theme = useTheme();
    const { pageSettings } = useContext(MainContext);
    const {
        defaultMenuContent: DefaultMenuContent,
        mobileMenuContent: MobileMenuContent
    } = pageSettings.elements;
    const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
    if (isMobile && MobileMenuContent) {
        return <MobileHeaderMenu menuContent={MobileMenuContent} />;
    } else if (DefaultMenuContent) {
        return <DefaultMenuContent />;
    } else {
        return null;
    }
};

export const MobileHeaderMenu: FunctionComponent<MobileHeaderMenuProps> = props => {

    const location = useLocation();
    const [open, setOpen] = useState(false);
    const [anchorEl, setAnchorEl] = useState<Element>();

    useEffect(() => {
        setOpen(false);
    }, [location]);

    const MenuContent = props.menuContent;
    return <>
        <IconButton
            title='Menu'
            aria-label='Menu'
            onClick={(event) => {
                setAnchorEl(event.currentTarget);
                setOpen(!open);
            }}>
            <MenuIcon />
        </IconButton>
        <Menu
            open={open}
            anchorEl={anchorEl}
            transformOrigin={{ vertical: 'top', horizontal: 'right' }}
            onClose={() => setOpen(false)} >
            <MenuContent />
        </Menu>
    </>;
};

export interface MobileHeaderMenuProps {
    menuContent: ComponentType;
}