import React, { FunctionComponent } from 'react';
import clsx from 'clsx';
import { Drawer, List, makeStyles, useTheme, useMediaQuery } from '@material-ui/core';

const drawerWidth = 240;

const useStyles = makeStyles((theme) => ({
    drawer: {
        position: 'relative',
        justifyContent: 'space-between'
    },
    drawerOpen: {
        width: drawerWidth,
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    drawerClose: {
        transition: theme.transitions.create('width', {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
        overflowX: 'hidden',
        width: theme.spacing(7) + 1,
        [theme.breakpoints.up('sm')]: {
            width: theme.spacing(9) + 1,
        }
    },
    upper: {

    },
    lower: {

    }
}));

export interface SidepanelProps {}

export const Sidepanel: FunctionComponent<SidepanelProps> = props => {
    const classes = useStyles();
    const theme = useTheme();
    const isLarge = useMediaQuery(theme.breakpoints.up('md'));
    return (
        <Drawer
            variant='permanent'
            classes={{
                paper: clsx(
                    classes.drawer,
                    {
                        [classes.drawerOpen]: isLarge,
                        [classes.drawerClose]: !isLarge
                    }),
            }}>
            <List className={classes.upper}>
                {props.children}
            </List>
        </Drawer>
    );
};