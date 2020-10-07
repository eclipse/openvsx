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
import {
    Theme, createStyles, WithStyles,
    withStyles, Box, Typography,
    Tabs, Tab, useTheme, useMediaQuery, Link
} from '@material-ui/core';
import { UserData, Namespace } from '../../extension-registry-types';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { makeStyles } from '@material-ui/styles';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { ErrorResponse } from '../../server-request';
import { PageSettings } from '../../page-settings';
import { NamespaceDetail } from './user-settings-namespace-detail';


const namespacesStyle = (theme: Theme) => createStyles({
    namespaceManagementContainer: {
        display: 'flex',
        width: '100%',
        [theme.breakpoints.down('md')]: {
            flexDirection: 'column',
            alignItems: 'center'
        }
    },
    namespaceContainer: {
        width: '160px',
        [theme.breakpoints.down('md')]: {
            width: '80%'
        }
    },
    namespaceTab: {
        minHeight: '24px'
    },
    namespaceTabWrapper: {
        textTransform: 'none'
    },
    namespace: {

    }
});

interface NamespaceTabProps {
    chosenNamespace: Namespace,
    onChange: (event: React.ChangeEvent<{}>, value: Namespace) => void,
    namespaces: Namespace[]
}

const NamespacesTabs = (props: NamespaceTabProps) => {
    const theme = useTheme();
    const classes = makeStyles(namespacesStyle)();
    const isATablet = useMediaQuery(theme.breakpoints.down('md'));
    return <Tabs
        orientation={isATablet ? 'horizontal' : 'vertical'}
        value={props.chosenNamespace}
        onChange={props.onChange}
        variant={isATablet ? 'scrollable' : 'standard'}
        scrollButtons={isATablet ? 'auto' : 'off'}
        className={classes.namespaceContainer}>
        {
            props.namespaces.map(namespace => {
                return <Tab
                    classes={{
                        root: classes.namespaceTab,
                        wrapper: classes.namespaceTabWrapper
                    }}
                    key={'nmspc-' + namespace.name}
                    className={classes.namespace}
                    value={namespace}
                    label={namespace.name}
                />;
            })
        }
    </Tabs>;
};

class UserSettingsNamespacesComponent extends React.Component<UserSettingsNamespacesComponent.Props, UserSettingsNamespacesComponent.State> {

    constructor(props: UserSettingsNamespacesComponent.Props) {
        super(props);

        this.state = {
            loading: true,
            namespaces: [],
            addDialogIsOpen: false,
            chosenNamespace: undefined
        };
    }

    componentDidMount() {
        this.initNamespaces();
    }

    render() {
        const namespace = this.state.chosenNamespace;
        const namespaceAccessUrl = this.props.pageSettings.urls.namespaceAccessInfo;
        return <React.Fragment>
            <DelayedLoadIndicator loading={this.state.loading} />
            {
                this.state.namespaces.length > 0 && namespace ?
                    <React.Fragment>
                        <Box className={this.props.classes.namespaceManagementContainer}>
                            <NamespacesTabs
                                chosenNamespace={namespace}
                                namespaces={this.state.namespaces}
                                onChange={this.handleChangeNamespace}
                            />
                            <NamespaceDetail
                                namespace={namespace}
                                user={this.props.user}
                                service={this.props.service}
                                handleError={this.props.handleError}
                                pageSettings={this.props.pageSettings}
                                setLoadingState={loading => this.setState({ loading })}></NamespaceDetail>
                        </Box>
                    </React.Fragment>
                    : !this.state.loading ? <Typography variant='body1'>No namespaces available. {
                        namespaceAccessUrl ?
                            <React.Fragment>
                                Read <Link color='secondary' href={namespaceAccessUrl} target='_blank'>here</Link> about claiming namespaces.
                            </React.Fragment>
                            : null
                    }
                    </Typography> : null
            }
        </React.Fragment>;
    }

    protected handleChangeNamespace = (event: React.ChangeEvent<{}>, value: Namespace): void => {
        this.doHandleChangeNamespace(value);
    };

    protected async doHandleChangeNamespace(chosenNamespace: Namespace): Promise<void> {
        this.setState({ chosenNamespace });
    }

    protected async initNamespaces(): Promise<void> {
        const namespaces = await this.props.service.getNamespaces();
        const chosenNamespace = namespaces.length ? namespaces[0] : undefined;
        this.setState({ namespaces, chosenNamespace, loading: false });
    }
}

export namespace UserSettingsNamespacesComponent {
    export interface Props extends WithStyles<typeof namespacesStyle> {
        user: UserData;
        service: ExtensionRegistryService;
        handleError: (err: Error | Partial<ErrorResponse>) => void;
        pageSettings: PageSettings;
    }

    export interface State {
        loading: boolean;
        namespaces: Namespace[];
        addDialogIsOpen: boolean;
        chosenNamespace?: Namespace;
    }
}

export const UserSettingsNamespaces = withStyles(namespacesStyle)(UserSettingsNamespacesComponent);
