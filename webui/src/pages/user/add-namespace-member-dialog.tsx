/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { ChangeEvent, FunctionComponent, KeyboardEvent, useState, useContext } from 'react';
import { UserData } from '../..';
import {
    Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Button, Popper, Fade, Paper,
    Box, Avatar
} from '@mui/material';
import { Namespace, NamespaceMembership } from '../../extension-registry-types';
import { NamespaceDetailConfigContext } from './user-settings-namespace-detail';
import { MainContext } from '../../context';
import { apiSlice, useSetNamespaceMemberMutation } from '../../store/api';

export interface AddMemberDialogProps {
    open: boolean;
    onClose: () => void;
    filterUsers: (user: UserData) => boolean;
    namespace: Namespace;
    members: NamespaceMembership[];
}

export const AddMemberDialog: FunctionComponent<AddMemberDialogProps> = props => {
    const { open } = props;
    const config = useContext(NamespaceDetailConfigContext);
    const { handleError } = useContext(MainContext);
    const [foundUsers, setFoundUsers] = useState<UserData[]>([]);
    const [showUserPopper, setShowUserPopper] = useState(false);
    const [popperTarget, setPopperTarget] = useState<HTMLInputElement | undefined>(undefined);
    const [setNamespaceMember] = useSetNamespaceMemberMutation();
    const [getUserByName] = apiSlice.useLazyGetUserByNameQuery();

    const addUser = async (user: UserData) => {
        if (!props.namespace) {
            return;
        }
        if (props.members.find(m => m.user.loginName === user.loginName && m.user.provider === user.provider)) {
            setShowUserPopper(false);
            handleError({ message: `User ${user.loginName} is already a member of ${props.namespace.name}.` });
            return;
        }
        const endpoint = props.namespace.roleUrl;
        await setNamespaceMember({ endpoint, user, role: config.defaultMemberRole ?? 'contributor' });
        onClose();
    };

    const onClose = () => {
        setShowUserPopper(false);
        props.onClose();
    };

    const handleUserSearch = async (e: ChangeEvent<HTMLInputElement>) => {
        const popperTarget = e.currentTarget;
        setPopperTarget(popperTarget);
        const val = popperTarget.value;
        let showUserPopper = false;
        let foundUsers: UserData[] = [];
        if (val) {
            const { data: users } = await getUserByName(val);
            if (users) {
                showUserPopper = true;
                foundUsers = users;
            }
        }
        setShowUserPopper(showUserPopper);
        setFoundUsers(foundUsers);
    };

    return <>
        <Dialog onClose={onClose} open={open} aria-labelledby='form-dialog-title'>
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
                    onChange={handleUserSearch}
                    onKeyDown={(e: KeyboardEvent) => {
                        if (e.key === "Enter" && foundUsers.length === 1) {
                            e.preventDefault();
                            addUser(foundUsers[0]);
                        }
                    }}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose} color='secondary'>
                    Cancel
                </Button>
            </DialogActions>
        </Dialog>
        <Popper
            sx={{ zIndex: 'tooltip' }}
            open={showUserPopper}
            anchorEl={popperTarget}
            placement='bottom'
            transition>
            {({ TransitionProps }) => (
                <Fade {...TransitionProps} timeout={350}>
                    <Paper sx={{ display: 'flex', flexDirection: 'column', width: 350 }}>
                        {
                            foundUsers.filter(props.filterUsers).map(foundUser => {
                                return <Box
                                    onClick={() => addUser(foundUser)}
                                    key={'found' + foundUser.loginName}
                                    sx={{
                                        display: 'flex',
                                        height: 60,
                                        alignItems: 'center',
                                        '&:hover': {
                                            cursor: 'pointer',
                                            bgcolor: 'action.hover'
                                        }
                                    }}
                                >
                                    <Box flex='1' marginLeft='10px'>
                                        <Box fontWeight='bold'>
                                            {foundUser.loginName}
                                        </Box>
                                        <Box fontSize='0.75rem'>
                                            {foundUser.fullName}
                                        </Box>
                                    </Box>
                                    <Box flex='1'>
                                        <Avatar variant='rounded' src={foundUser.avatarUrl} />
                                    </Box>
                                </Box>;
                            })
                        }
                    </Paper>
                </Fade>
            )}
        </Popper>
    </>;
};