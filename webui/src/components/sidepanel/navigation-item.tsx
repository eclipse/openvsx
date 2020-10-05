/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent } from 'react';
import { ListItem, ListItemText, Collapse, List, ListItemIcon, makeStyles } from '@material-ui/core';
import ExpandLess from '@material-ui/icons/ExpandLess';
import { useHistory } from 'react-router';
import clsx from 'clsx';

const useStyles = makeStyles((theme) => ({
    nested: {
        paddingLeft: theme.spacing(4),
    },
    active: {
        background: theme.palette.action.selected
    }
}));

export interface NavigationProps {
    route?: string;
    icon?: React.ReactNode;
    label: string;
    active?: boolean;
    onOpenRoute?: (route: string) => void;
}

export const NavigationItem: FunctionComponent<NavigationProps> = props => {
    const classes = useStyles();
    const [open, setOpen] = React.useState(false);
    const history = useHistory();

    const handleClick = () => {
        if (props.children) {
            setOpen(!open);
        } else if (props.route) {
            props.onOpenRoute && props.onOpenRoute(props.route);
            history.push(props.route);
        }
    };

    return (<>
        <ListItem className={clsx(props.active && classes.active)} button onClick={handleClick}>
            {
                props.icon && <ListItemIcon>{props.icon}</ListItemIcon>
            }
            <ListItemText primary={props.label} />
            {props.children && open && <ExpandLess />}
        </ListItem>
        {
            props.children &&
            <Collapse in={open} timeout='auto' unmountOnExit>
                <List className={classes.nested}>
                    {props.children}
                </List>
            </Collapse>
        }
    </>);
};