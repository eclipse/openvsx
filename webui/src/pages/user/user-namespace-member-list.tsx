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
import { Box, Typography, Button, Paper, makeStyles } from '@material-ui/core';
import { UserNamespaceMember } from './user-namespace-member-component';
import { Namespace, NamespaceMembership, MembershipRole, isError, UserData } from '../../extension-registry-types';
import { AddMemberDialog } from './add-namespace-member-dialog';
import { MainContext } from '../../context';

const useStyles = makeStyles((theme) => ({
    addButton: {
        [theme.breakpoints.down('md')]: {
            marginLeft: theme.spacing(2)
        }
    },
    memberListHeader: {
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: theme.spacing(1),
        [theme.breakpoints.down('sm')]: {
            flexDirection: 'column',
            alignItems: 'center'
        }
    }
}));

export const UserNamespaceMemberList: FunctionComponent<UserNamespaceMemberList.Props> = props => {
    const classes = useStyles();
    const { service, user, handleError } = useContext(MainContext);
    const [members, setMembers] = useState<NamespaceMembership[]>([]);
    useEffect(() => {
        fetchMembers();
    }, [props.namespace]);

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
            const membershipList = await service.getNamespaceMembers(props.namespace);
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
            const result = await service.setNamespaceMember(endpoint, membership.user, role);
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
        <Box className={classes.memberListHeader}>
            <Typography variant='h5'>Members in {props.namespace.name}</Typography>
            <Button className={classes.addButton} variant='outlined' onClick={handleOpenAddDialog}>
                Add Namespace Member
            </Button>
        </Box>
        {members.length ?
            <Paper>
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

export namespace UserNamespaceMemberList {
    export interface Props {
        namespace: Namespace;
        setLoadingState: (loadingState: boolean) => void;
        filterUsers: (user: UserData) => boolean;
        fixSelf: boolean;
    }
}
