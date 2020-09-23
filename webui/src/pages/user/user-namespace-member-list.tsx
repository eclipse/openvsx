import React, { FunctionComponent, useEffect, useState, useContext } from 'react';
import { Box, Typography, Button, Paper, makeStyles } from '@material-ui/core';
import { UserNamespaceMember } from './user-namespace-member-component';
import { Namespace, NamespaceMembership, MembershipRole, isError } from '../../extension-registry-types';
import { AddMemberDialog } from './add-namespace-member-dialog';
import { ServiceContext } from '../../default/default-app';
import { UserContext } from '../../main';
import { ErrorResponse } from '../../server-request';

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

interface UserNamespaceMemberListProps {
    namespace: Namespace;
    setLoadingState: (loadingState: boolean) => void;
    handleError: (err: Error | Partial<ErrorResponse>) => void;
}

export const UserNamespaceMemberList: FunctionComponent<UserNamespaceMemberListProps> = props => {
    const classes = useStyles();
    const service = useContext(ServiceContext);
    const user = useContext(UserContext);
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
        setMembers([]);
        const members = await service.getNamespaceMembers(props.namespace);
        setMembers(members as NamespaceMembership[]);
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
            props.handleError(err);
            props.setLoadingState(false);
        }
    };

    return (<>
        {
            user ?
                <>
                    <Box className={classes.memberListHeader}>
                        <Typography variant='h5'>Members in {props.namespace.name}</Typography>
                        <Button className={classes.addButton} variant='outlined' onClick={handleOpenAddDialog}>
                            Add Namespace Member
                        </Button>
                    </Box>
                    <Paper>
                        {members.map(member =>
                            <UserNamespaceMember
                                key={'nspcmbr-' + member.user.loginName + member.user.provider}
                                namespace={props.namespace}
                                member={member}
                                user={user}
                                service={service}
                                onChangeRole={role => changeRole(member, role)}
                                onRemoveUser={() => changeRole(member, 'remove')}
                                handleError={props.handleError} />)}
                    </Paper>
                    <AddMemberDialog
                        handleError={props.handleError}
                        members={members}
                        namespace={props.namespace}
                        service={service}
                        user={user}
                        onClose={handleCloseAddDialog}
                        open={addDialogIsOpen}
                        setLoadingState={props.setLoadingState}>
                    </AddMemberDialog>
                </>
                : ''
        }
    </>);
};