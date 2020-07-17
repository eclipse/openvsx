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
    withStyles, Box, Paper, Typography,
    Avatar, Button, Dialog, DialogTitle,
    DialogContent, DialogContentText,
    TextField, DialogActions, Popper,
    Fade, Tabs, Tab, useTheme, useMediaQuery, Link
} from '@material-ui/core';
import { UserData, Namespace, NamespaceMembership, isError } from '../../extension-registry-types';
import { ExtensionRegistryService } from '../../extension-registry-service';
import { makeStyles } from '@material-ui/styles';
import { UserNamespaceMember } from './user-namespace-member-component';
import { DelayedLoadIndicator } from '../../custom-mui-components/delayed-load-indicator';
import { UserNamespaceExtensionList } from './user-namespace-extension-list';
import { ErrorResponse } from '../../server-request';
import { PageSettings } from '../../page-settings';


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

    },
    addButton: {
        [theme.breakpoints.down('md')]: {
            marginLeft: theme.spacing(2)
        }
    },

    memberContainer: {
        flex: 5,
        padding: theme.spacing(1),
        [theme.breakpoints.only('md')]: {
            width: '80%'
        },
        [theme.breakpoints.down('sm')]: {
            width: '100%'
        }
    },
    memberListHeader: {
        display: 'flex',
        padding: `0 ${theme.spacing(2)}px ${theme.spacing(2)}px`,
        justifyContent: 'space-between',
        alignItems: 'center',
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            alignItems: 'center'
        }
    },
    foundUserListPopper: {
        zIndex: theme.zIndex.tooltip
    },
    foundUserListContainer: {
        display: 'flex',
        flexDirection: 'column',
        width: 350
    },
    foundUserContainer: {
        display: 'flex',
        height: 60,
        alignItems: 'center',
        '&:hover': {
            cursor: 'pointer',
            background: theme.palette.grey[100]
        }
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
            members: [],
            addDialogIsOpen: false,
            showUserPopper: false,
            foundUsers: [],
            popperTarget: undefined,
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
                            <Box className={this.props.classes.memberContainer}>
                                <Box className={this.props.classes.memberListHeader}>
                                    <Typography variant='h5'>Members in {namespace.name}</Typography>

                                    <Button className={this.props.classes.addButton} variant='outlined' onClick={this.handleOpenAddDialog}>
                                        Add Namespace Member
                                    </Button>
                                </Box>
                                <Paper>
                                    {this.state.members.map(member =>
                                        <UserNamespaceMember
                                            key={'nspcmbr-' + member.user.loginName + member.user.provider}
                                            namespace={namespace}
                                            member={member}
                                            user={this.props.user}
                                            service={this.props.service}
                                            onRemoveUser={() => this.removeUser(member)}
                                            handleError={this.props.handleError} />)}
                                </Paper>
                                <UserNamespaceExtensionList
                                    namespace={namespace}
                                    service={this.props.service}
                                    setError={this.props.handleError}
                                    pageSettings={this.props.pageSettings}
                                />
                            </Box>
                        </Box>
                        {this.renderAddDialog()}
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

    protected renderAddDialog() {
        return <React.Fragment>
            <Dialog onClose={this.handleCloseAddDialog} open={this.state.addDialogIsOpen} aria-labelledby='form-dialog-title'>
                <DialogTitle id='form-dialog-title'>Add User to Namespace</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Enter the Login Name of the User you want to add.
                    </DialogContentText>
                    <TextField
                        autoFocus
                        margin='dense'
                        id='name'
                        autoComplete='off'
                        label='Open VSX User'
                        fullWidth
                        onChange={this.handleUserSearch}
                        onKeyPress={(e: React.KeyboardEvent) => {
                            if (e.charCode === 13) {
                                if (this.state.foundUsers.length === 1) {
                                    this.addUser(this.state.foundUsers[0]);
                                }
                            }
                        }}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={this.handleCloseAddDialog} color='secondary'>
                        Cancel
                    </Button>
                </DialogActions>
            </Dialog>
            <Popper
                className={this.props.classes.foundUserListPopper}
                open={this.state.showUserPopper}
                anchorEl={this.state.popperTarget}
                placement='bottom'
                transition>
                {({ TransitionProps }) => (
                    <Fade {...TransitionProps} timeout={350}>
                        <Paper className={this.props.classes.foundUserListContainer}>
                            {
                                this.state.foundUsers.map(user => {
                                    if (this.props.user.loginName !== user.loginName || this.props.user.provider !== user.provider) {
                                        return <Box
                                            onClick={() => this.addUser(user)}
                                            className={this.props.classes.foundUserContainer}
                                            key={'found' + user.loginName}>
                                            <Box flex='1' marginLeft='10px'>
                                                <Box fontWeight='bold'>
                                                    {user.loginName}
                                                </Box>
                                                <Box fontSize='0.75rem'>
                                                    {user.fullName}
                                                </Box>
                                            </Box>
                                            <Box flex='1'>
                                                <Avatar variant='rounded' src={user.avatarUrl} />
                                            </Box>
                                        </Box>;
                                    } else {
                                        return '';
                                    }
                                })
                            }
                        </Paper>
                    </Fade>
                )}
            </Popper>
        </React.Fragment>;
    }

    protected async addUser(user: UserData) {
        try {
            if (this.state.chosenNamespace) {
                const endpoint = this.state.chosenNamespace.roleUrl;
                const result = await this.props.service.setNamespaceMember(endpoint, user, 'contributor');
                if (isError(result)) {
                    throw result;
                }
                this.setState({ loading: true });
                const members = await this.props.service.getNamespaceMembers(this.state.chosenNamespace);
                this.setState({ members, loading: false });
                this.doCloseAddDialog();
            }
        } catch (err) {
            this.props.handleError(err);
            this.setState({ loading: false });
        }
    }

    protected async removeUser(membership: NamespaceMembership) {
        try {
            if (this.state.chosenNamespace) {
                const endpoint = this.state.chosenNamespace.roleUrl;
                const result = await this.props.service.setNamespaceMember(endpoint, membership.user, 'remove');
                if (isError(result)) {
                    throw result;
                }
                this.setState({ loading: true });
                const members = await this.props.service.getNamespaceMembers(this.state.chosenNamespace);
                this.setState({ members, loading: false });
            }
        } catch (err) {
            this.props.handleError(err);
            this.setState({ loading: false });
        }
    }

    protected handleUserSearch = (ev: React.ChangeEvent<HTMLInputElement>) => this.doHandleUserSearch(ev);
    protected async doHandleUserSearch(ev: React.ChangeEvent<HTMLInputElement>) {
        const popperTarget = ev.currentTarget;
        const val = popperTarget.value;
        let showUserPopper = false;
        let foundUsers: UserData[] = [];
        if (val) {
            const users = await this.props.service.getUserByName(val);
            if (users) {
                showUserPopper = true;
                foundUsers = users;
            }
        }
        this.setState({ showUserPopper, foundUsers, popperTarget });
    }

    protected handleOpenAddDialog = () => this.doOpenAddDialog();
    protected doOpenAddDialog() {
        this.setState({ addDialogIsOpen: true });
    }

    protected handleCloseAddDialog = () => this.doCloseAddDialog();
    protected doCloseAddDialog() {
        this.setState({ addDialogIsOpen: false, showUserPopper: false });
    }

    protected handleChangeNamespace = (event: React.ChangeEvent<{}>, value: Namespace) => {
        this.doHandleChangeNamespace(value);
    };

    protected async doHandleChangeNamespace(chosenNamespace: Namespace) {
        const members = await this.props.service.getNamespaceMembers(chosenNamespace);
        this.setState({ members, chosenNamespace });
    }

    protected async initNamespaces() {
        const namespaces = await this.props.service.getNamespaces();
        const chosenNamespace = namespaces.length ? namespaces[0] : undefined;
        let members: NamespaceMembership[] = [];
        if (chosenNamespace) {
            members = await this.props.service.getNamespaceMembers(chosenNamespace);
        }
        this.setState({ namespaces, chosenNamespace, members, loading: false });
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
        members: NamespaceMembership[];
        addDialogIsOpen: boolean;
        showUserPopper: boolean;
        foundUsers: UserData[];
        popperTarget: any;
        chosenNamespace?: Namespace;
    }
}

export const UserSettingsNamespaces = withStyles(namespacesStyle)(UserSettingsNamespacesComponent);
