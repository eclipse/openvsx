import React, { FunctionComponent, useState, useEffect } from 'react';
import { Box, Typography, Button, Paper, makeStyles } from '@material-ui/core';
import { UserNamespaceMember } from './user-namespace-member-component';
import { UserNamespaceExtensionList } from './user-namespace-extension-list';
import { Namespace, UserData, NamespaceMembership, MembershipRole, isError } from '../../extension-registry-types';
import { ExtensionRegistryService, PageSettings } from '../..';
import { ErrorResponse } from '../../server-request';
import { AddMemberDialog } from './add-namespace-member-dialog';

const useStyles = makeStyles((theme) => ({
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
    }
}));

export interface NamespaceProps {
    namespace: Namespace;
    user: UserData;
    service: ExtensionRegistryService;
    handleError: (err: Error | Partial<ErrorResponse>) => void;
    pageSettings: PageSettings;
    setLoadingState: (loading: boolean) => void;
}

export const NamespaceDetail: FunctionComponent<NamespaceProps> = props => {
    const classes = useStyles();
    const [addDialogIsOpen, setAddDialogIsOpen] = useState(false);
    const [members, setMembers] = useState<NamespaceMembership[]>([]);
    useEffect(() => {
        fetchMembers();
    }, [props.namespace]);

    const fetchMembers = async () => {
        const members = await props.service.getNamespaceMembers(props.namespace);
        setMembers(members);
    };

    const handleOpenAddDialog = () => {
        setAddDialogIsOpen(true);
    };

    const handleCloseAddDialog = async () => {
        setAddDialogIsOpen(false);
        fetchMembers();
    };

    const changeRole = async (membership: NamespaceMembership, role: MembershipRole | 'remove') => {
        try {
            props.setLoadingState(true);
            const endpoint = props.namespace.roleUrl;
            const result = await props.service.setNamespaceMember(endpoint, membership.user, role);
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

    return <>
        <Box className={classes.memberContainer}>
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
                        user={props.user}
                        service={props.service}
                        onChangeRole={role => changeRole(member, role)}
                        onRemoveUser={() => changeRole(member, 'remove')}
                        handleError={props.handleError} />)}
            </Paper>
            <UserNamespaceExtensionList
                namespace={props.namespace}
                service={props.service}
                setError={props.handleError}
                pageSettings={props.pageSettings}
            />
            <AddMemberDialog
                handleError={props.handleError}
                members={members}
                namespace={props.namespace}
                service={props.service}
                user={props.user}
                onClose={handleCloseAddDialog}
                open={addDialogIsOpen}
                setLoadingState={props.setLoadingState}>
            </AddMemberDialog>
        </Box>
    </>;
};
