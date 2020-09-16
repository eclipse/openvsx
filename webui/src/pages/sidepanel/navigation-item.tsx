import React, { FunctionComponent } from 'react';
import { ListItem, ListItemText, Collapse, List, ListItemIcon, makeStyles } from '@material-ui/core';
import ExpandLess from '@material-ui/icons/ExpandLess';
import { useHistory } from 'react-router';

const useStyles = makeStyles((theme) => ({
    nested: {
        paddingLeft: theme.spacing(4),
    }
}));

export interface NavigationProps {
    route?: string;
    icon?: React.ReactNode;
    label: string;
}

export const NavigationItem: FunctionComponent<NavigationProps> = props => {
    const classes = useStyles();
    const [open, setOpen] = React.useState(false);
    const history = useHistory();

    const handleClick = () => {
        if (props.children) {
            setOpen(!open);
        } else if (props.route) {
            history.push(props.route);
        }
    };

    return (<>
        <ListItem button onClick={handleClick}>
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