import React, { FunctionComponent } from 'react';
import { Box, Container, makeStyles, CssBaseline } from '@material-ui/core';
import { createRoute } from '../../utils';
import { Sidepanel } from '../sidepanel/sidepanel';
import { NavigationItem } from '../sidepanel/navigation-item';
import AssignmentIndIcon from '@material-ui/icons/AssignmentInd';
import ExtensionSharpIcon from '@material-ui/icons/ExtensionSharp';
import { Route, Switch } from 'react-router-dom';
import { NamespaceAdmin } from './namespace-admin';
import { ExtensionAdmin } from './extension-admin';
// import { ServiceContext } from '../../default/default-app';

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
    }
}));

export const AdminDashboard: FunctionComponent = props => {
    const classes = useStyles();
    // const service = useContext(ServiceContext);
    return <>
        <CssBaseline />
        <Box display='flex' height='100vh'>
            <Sidepanel>
                <NavigationItem label='Namespaces' icon={<AssignmentIndIcon />} route={AdminDashboardRoutes.NAMESPACE_ADMIN}></NavigationItem>
                <NavigationItem label='Extensions' icon={<ExtensionSharpIcon />} route={AdminDashboardRoutes.EXTENSION_ADMIN}></NavigationItem>
            </Sidepanel>
            <Box overflow='auto' flex={1} >
                <Container className={classes.container} maxWidth='lg'>
                    <Switch>
                        <Route path={AdminDashboardRoutes.NAMESPACE_ADMIN} component={NamespaceAdmin} />
                        <Route path={AdminDashboardRoutes.EXTENSION_ADMIN} component={ExtensionAdmin} />
                    </Switch>
                </Container>
            </Box>
        </Box>
    </>;
};