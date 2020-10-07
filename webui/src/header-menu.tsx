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
import { withRouter, RouteComponentProps } from 'react-router-dom';
import { Menu, IconButton, useTheme } from '@material-ui/core';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import MenuIcon from '@material-ui/icons/Menu';
import { PageSettings } from './page-settings';

export const HeaderMenu: React.FunctionComponent<{ pageSettings: PageSettings; }> = ({ pageSettings }) => {
    const theme = useTheme();
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

export class MobileHeaderMenuComponent extends React.Component<MobileHeaderMenu.Props, MobileHeaderMenu.State> {

    constructor(props: MobileHeaderMenu.Props) {
        super(props);
        this.state = { open: false };
    }

    componentDidUpdate(prevProps: MobileHeaderMenu.Props): void {
        const currProps = this.props;
        if (currProps.location !== prevProps.location) {
            this.setState({ open: false });
        }
    }

    render(): React.ReactElement {
        const MenuContent = this.props.menuContent;
        return <React.Fragment>
            <IconButton
                title='Menu'
                aria-label='Menu'
                onClick={event => this.setState({
                    anchorEl: event.currentTarget,
                    open: !this.state.open
                })} >
                <MenuIcon />
            </IconButton>
            <Menu
                open={this.state.open}
                anchorEl={this.state.anchorEl}
                transformOrigin={{ vertical: 'top', horizontal: 'right' }}
                onClose={() => this.setState({ open: false })} >
                <MenuContent />
            </Menu>
        </React.Fragment>;
    }
}

export namespace MobileHeaderMenu {
    export interface Props extends RouteComponentProps {
        menuContent: React.ComponentType;
    }
    export interface State {
        open: boolean;
        anchorEl?: Element;
    }
}

export const MobileHeaderMenu = withRouter(MobileHeaderMenuComponent);
