/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

import * as React from 'react';
import { Extension } from '../../extension-registry-types';
import { withStyles, createStyles } from '@material-ui/styles';
import { Box, Theme, Typography, WithStyles } from '@material-ui/core';
import { PublishExtensionDialog } from './publish-extension-dialog';
import { UserExtensionList } from './user-extension-list';
import { isError } from '../../extension-registry-types';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { MainContext } from '../../context';

const extensionsStyle = (theme: Theme) => createStyles({
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            alignItems: 'center'
        }
    },
    buttons: {
        display: 'flex',
        flexWrap: 'wrap',
        [theme.breakpoints.down('sm')]: {
            justifyContent: 'center'
        }
    }
});

class UserSettingsExtensionsComponent extends React.Component<UserSettingsExtensionsComponent.Props, UserSettingsExtensionsComponent.State> {

    static contextType = MainContext;
    declare context: MainContext;

    protected abortController = new AbortController();

    constructor(props: UserSettingsExtensionsComponent.Props) {
        super(props);

        this.state = {
            loading: true,
            extensions: []
        };
    }

    componentDidMount() {
        this.updateExtensions();
    }

    protected handleExtensionPublished = () => {
        this.setState({ loading: true });
        this.updateExtensions();
    };

    render() {
        return <React.Fragment>
            <Box className={this.props.classes.header}>
                <Box>
                    <Typography variant='h5' gutterBottom>Extensions</Typography>
                </Box>
                <Box className={this.props.classes.buttons}>
                    <Box mr={1} mb={1}>
                        <PublishExtensionDialog extensionPublished={this.handleExtensionPublished}/>
                    </Box>
                </Box>
            </Box>
            <Box mt={2}>
                <DelayedLoadIndicator loading={this.state.loading} />
                {
                    this.state.extensions && this.state.extensions.length > 0
                    ? <UserExtensionList extensions={this.state.extensions} loading={this.state.loading} />
                    : <Typography  variant='body1'>No extensions published under this namespace yet.</Typography>
                }
            </Box>
        </React.Fragment>;
    }

    protected async updateExtensions(): Promise<void> {
        if (!this.context.user) {
            return;
        }
        try {
            const response = await this.context.service.getExtensions(this.abortController);
            if (isError(response)) {
                throw response;
            }

            const extensions = response as Extension[];
            this.setState({ extensions, loading: false });
        } catch (err) {
            this.context.handleError(err);
            this.setState({ loading: false });
        }
    }
}

export namespace UserSettingsExtensionsComponent {
    export interface Props extends WithStyles<typeof extensionsStyle> {
    }

    export interface State {
        loading: boolean;
        extensions: Extension[];
    }
}

export const UserSettingsExtensions = withStyles(extensionsStyle)(UserSettingsExtensionsComponent);