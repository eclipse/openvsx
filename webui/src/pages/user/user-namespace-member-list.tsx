/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useEffect, useState, useContext } from 'react';
import { Box, Typography, Button, Paper } from '@mui/material';
import { UserNamespaceMember } from './user-namespace-member-component';
import { Namespace, NamespaceMembership, MembershipRole, isError, UserData } from '../../extension-registry-types';
import { AddMemberDialog } from './add-namespace-member-dialog';
import { MainContext } from '../../context';

export const UserNamespaceMemberList: FunctionComponent<UserNamespaceMemberListProps> = props => {
    const { service, user, handleError } = useContext(MainContext);
    const [members, setMembers] = useState<NamespaceMembership[]>([]);
    useEffect(() => {
        fetchMembers();
    }, [props.namespace]);

    const abortController = new AbortController();
    useEffect(() => {
        return () => {
            abortController.abort();
        };
    }, []);

    const [addDialogIsOpen, setAddDialogIsOpen] = useState(false);
    const handleCloseAddDialog = async () => {
        setAddDialogIsOpen(false);
        fetchMembers();
    };
    const handleOpenAddDialog = () => {
        setAddDialogIsOpen(true);
    };

    const fetchMembers = async () => {
        try {
            const membershipList = await service.getNamespaceMembers(abortController, props.namespace);
            const members = membershipList.namespaceMemberships;
            setMembers(members);
        } catch (err) {
            handleError(err);
        }
    };

    const changeRole = async (membership: NamespaceMembership, role: MembershipRole | 'remove') => {
        try {
            props.setLoadingState(true);
            const endpoint = props.namespace.roleUrl;
            const result = await service.setNamespaceMember(abortController, endpoint, membership.user, role);
            if (isError(result)) {
                throw result;
            }
            await fetchMembers();
            props.setLoadingState(false);
        } catch (err) {
            handleError(err);
            props.setLoadingState(false);
        }
    };

    if (!user) {
        return null;
    }
    return <>
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                mb: 1,
                flexDirection: { xs: 'column', sm: 'column', md: 'row', lg: 'row', xl: 'row' }
            }}
        >
            <Typography variant='h5'>Members</Typography>
            <Button sx={{ ml: { xs: 2, sm: 2, md: 2, lg: 0, xl: 0 } }} variant='outlined' onClick={handleOpenAddDialog}>
                Add Namespace Member
            </Button>
        </Box>
        {members.length ?
            <Paper elevation={3}>
                {members.map(member =>
                    <UserNamespaceMember
                        key={'nspcmbr-' + member.user.loginName + member.user.provider}
                        namespace={props.namespace}
                        member={member}
                        fixSelf={props.fixSelf}
                        onChangeRole={role => changeRole(member, role)}
                        onRemoveUser={() => changeRole(member, 'remove')} />)}
            </Paper> :
            <Typography variant='body1'>There are no members assigned yet.</Typography>}
        <AddMemberDialog
            members={members}
            namespace={props.namespace}
            onClose={handleCloseAddDialog}
            open={addDialogIsOpen}
            setLoadingState={props.setLoadingState}
            filterUsers={props.filterUsers} />
    </>;
};

export interface UserNamespaceMemberListProps {
    namespace: Namespace;
    setLoadingState: (loadingState: boolean) => void;
    filterUsers: (user: UserData) => boolean;
    fixSelf: boolean;
}