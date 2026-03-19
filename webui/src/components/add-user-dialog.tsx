/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import { ChangeEvent, FC, KeyboardEvent, useState, useContext, useEffect, useRef } from 'react';
import {
    Dialog, DialogTitle, DialogContent, DialogContentText, TextField, DialogActions, Button,
    Popper, Fade, Paper, Box, Avatar
} from '@mui/material';
import type { UserData } from '../extension-registry-types';
import { MainContext } from '../context';

export interface AddUserDialogProps {
    open: boolean;
    title: string;
    description: string;
    existingUsers: UserData[];
    onClose: () => void;
    onAddUser: (user: UserData) => void;
}

export const AddUserDialog: FC<AddUserDialogProps> = ({
    open,
    title,
    description,
    existingUsers,
    onClose,
    onAddUser
}) => {
    const { service, handleError } = useContext(MainContext);
    const [foundUsers, setFoundUsers] = useState<UserData[]>([]);
    const [showUserPopper, setShowUserPopper] = useState(false);
    const [popperTarget, setPopperTarget] = useState<HTMLInputElement | undefined>(undefined);
    const abortController = useRef<AbortController>(new AbortController());

    useEffect(() => {
        return () => {
            abortController.current.abort();
        };
    }, []);

    const filterUsers = (user: UserData) =>
        !existingUsers.some(u => u.loginName === user.loginName && u.provider === user.provider);

    const addUser = (user: UserData) => {
        if (!filterUsers(user)) {
            setShowUserPopper(false);
            handleError({ message: `User ${user.loginName} is already added.` });
            return;
        }
        setShowUserPopper(false);
        onAddUser(user);
        onClose();
    };

    const handleClose = () => {
        setShowUserPopper(false);
        setFoundUsers([]);
        onClose();
    };

    const handleUserSearch = async (e: ChangeEvent<HTMLInputElement>) => {
        const target = e.currentTarget;
        setPopperTarget(target);
        const val = target.value;
        if (val) {
            const users = await service.getUserByName(abortController.current, val);
            if (users) {
                setShowUserPopper(true);
                setFoundUsers(users);
            }
        } else {
            setShowUserPopper(false);
            setFoundUsers([]);
        }
    };

    return <>
        <Dialog onClose={handleClose} open={open} maxWidth='sm' fullWidth>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <DialogContentText>{description}</DialogContentText>
                <TextField
                    autoFocus
                    margin='dense'
                    autoComplete='off'
                    label='Search User'
                    fullWidth
                    onChange={handleUserSearch}
                    onKeyDown={(e: KeyboardEvent) => {
                        if (e.key === 'Enter') {
                            const match = foundUsers.find(filterUsers);
                            if (match) {
                                e.preventDefault();
                                addUser(match);
                            }
                        }
                    }}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={handleClose} color='secondary'>
                    Cancel
                </Button>
            </DialogActions>
        </Dialog>
        <Popper
            sx={{ zIndex: 'tooltip' }}
            open={showUserPopper}
            anchorEl={popperTarget}
            placement='bottom'
            transition
        >
            {({ TransitionProps }) => (
                <Fade {...TransitionProps} timeout={350}>
                    <Paper sx={{ display: 'flex', flexDirection: 'column', width: 350 }}>
                        {foundUsers.filter(filterUsers).map(user => (
                            <Box
                                onClick={() => addUser(user)}
                                key={user.loginName + user.provider}
                                sx={{
                                    display: 'flex',
                                    height: 60,
                                    alignItems: 'center',
                                    px: 1.5,
                                    '&:hover': {
                                        cursor: 'pointer',
                                        bgcolor: 'action.hover'
                                    }
                                }}
                            >
                                <Avatar
                                    variant='rounded'
                                    src={user.avatarUrl}
                                    sx={{ mr: 1.5, width: 36, height: 36 }}
                                />
                                <Box>
                                    <Box fontWeight='bold'>{user.loginName}</Box>
                                    <Box fontSize='0.75rem' color='text.secondary'>{user.fullName}</Box>
                                </Box>
                            </Box>
                        ))}
                    </Paper>
                </Fade>
            )}
        </Popper>
    </>;
};
