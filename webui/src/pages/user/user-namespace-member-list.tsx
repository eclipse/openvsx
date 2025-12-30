/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useState } from 'react';
import { Box, Typography, Button, Paper } from '@mui/material';
import { UserNamespaceMember } from './user-namespace-member-component';
import { Namespace, NamespaceMembership, MembershipRole, UserData } from '../../extension-registry-types';
import { AddMemberDialog } from './add-namespace-member-dialog';
import { useGetNamespaceMembersQuery, useGetUserQuery, useSetNamespaceMemberMutation } from '../../store/api';

export const UserNamespaceMemberList: FunctionComponent<UserNamespaceMemberListProps> = props => {
    const [addDialogIsOpen, setAddDialogIsOpen] = useState(false);
    const { data: user } = useGetUserQuery();
    const { data: membershipList } = useGetNamespaceMembersQuery(props.namespace);
    const [setNamespaceMember] = useSetNamespaceMemberMutation();


    const handleCloseAddDialog = async () => {
        setAddDialogIsOpen(false);
    };
    const handleOpenAddDialog = () => {
        setAddDialogIsOpen(true);
    };

    const changeRole = async (membership: NamespaceMembership, role: MembershipRole | 'remove') => {
        const endpoint = props.namespace.roleUrl;
        await setNamespaceMember({ endpoint, user: membership.user, role });
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
        {membershipList?.namespaceMemberships.length ?
            <Paper elevation={3}>
                {membershipList?.namespaceMemberships.map(member =>
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
            members={membershipList?.namespaceMemberships ?? []}
            namespace={props.namespace}
            onClose={handleCloseAddDialog}
            open={addDialogIsOpen}
            filterUsers={props.filterUsers} />
    </>;
};

export interface UserNamespaceMemberListProps {
    namespace: Namespace;
    filterUsers: (user: UserData) => boolean;
    fixSelf: boolean;
}