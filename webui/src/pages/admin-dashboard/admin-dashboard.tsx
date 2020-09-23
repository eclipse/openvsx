import React, { FunctionComponent, useState, useContext } from 'react';
import { Box, Container, makeStyles, CssBaseline, Typography, IconButton } from '@material-ui/core';
import { createRoute } from '../../utils';
import { Sidepanel } from '../sidepanel/sidepanel';
import { NavigationItem } from '../sidepanel/navigation-item';
import AssignmentIndIcon from '@material-ui/icons/AssignmentInd';
import ExtensionSharpIcon from '@material-ui/icons/ExtensionSharp';
import { Route, Switch, useHistory } from 'react-router-dom';
import { NamespaceAdmin } from './namespace-admin';
import { ExtensionAdmin } from './extension-admin';
import { handleError } from '../../utils';
import { ErrorDialog } from '../../components/error-dialog';
import { UserContext } from '../../main';
import HighlightOffIcon from '@material-ui/icons/HighlightOff';

export namespace AdminDashboardRoutes {
    export const ROOT = 'admin-dashboard';
    export const MAIN = createRoute([ROOT]);
    export const NAMESPACE_ADMIN = createRoute([MAIN, 'namespaces']);
    export const EXTENSION_ADMIN = createRoute([MAIN, 'extensions']);
}

const useStyles = makeStyles((theme) => ({
    container: {
        paddingTop: theme.spacing(4),
        paddingBottom: theme.spacing(4),
        height: '100%'
    },
    message: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh'
    }
}));

export const AdminDashboard: FunctionComponent = props => {
    const classes = useStyles();

    const [isErrorDialogOpen, setIsErrorDialogOpen] = useState(false);
    const [error, setError] = useState('');
    const doHandleError = (err: {}) => {
        const error = handleError(err);
        setError(error);
        setIsErrorDialogOpen(true);
    };

    const doHandleDialogClose = () => {
        setIsErrorDialogOpen(false);
    };

    const user = useContext(UserContext);

    const history = useHistory();
    const toMainPage = () => history.push('/');

    return <>
        <CssBaseline />
        {
            user && user.role && user.role === 'admin' ?
                <Box display='flex' height='100vh'>
                    <Sidepanel>
                        <NavigationItem label='Namespaces' icon={<AssignmentIndIcon />} route={AdminDashboardRoutes.NAMESPACE_ADMIN}></NavigationItem>
                        <NavigationItem label='Extensions' icon={<ExtensionSharpIcon />} route={AdminDashboardRoutes.EXTENSION_ADMIN}></NavigationItem>
                    </Sidepanel>
                    <Box overflow='auto' flex={1} >
                        <Container className={classes.container} maxWidth='lg'>
                            <Switch>
                                <Route path={AdminDashboardRoutes.NAMESPACE_ADMIN}>
                                    <NamespaceAdmin handleError={doHandleError} />
                                </Route>
                                <Route path={AdminDashboardRoutes.EXTENSION_ADMIN} component={ExtensionAdmin} />
                            </Switch>
                        </Container>
                    </Box>
                    <Box position='absolute' top='5px' right='5px'>
                        <IconButton onClick={toMainPage}>
                            <HighlightOffIcon></HighlightOffIcon>
                        </IconButton>
                    </Box>
                </Box>
                : user ?
                    <Box className={classes.message}><Typography variant='h6'>You are not authorized as administrator</Typography></Box>
                    :
                    <Box className={classes.message}><Typography variant='h6'>You are not logged in</Typography></Box>
        }
        {
            error ?
                <ErrorDialog
                    errorMessage={error}
                    isErrorDialogOpen={isErrorDialogOpen}
                    handleCloseDialog={doHandleDialogClose} />
                : null
        }
    </>;
};