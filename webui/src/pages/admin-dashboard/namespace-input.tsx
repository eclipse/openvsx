import React, { FunctionComponent, useState } from 'react';
import { Paper, IconButton, makeStyles, InputBase } from '@material-ui/core';
import SearchIcon from '@material-ui/icons/Search';

const useStyles = makeStyles(theme =>
    ({
        root: {
            flex: 2,
            display: 'flex',
            marginRight: theme.spacing(1),
            [theme.breakpoints.down('sm')]: {
                marginRight: 0,
                marginBottom: theme.spacing(2),
            },
        },
        input: {
            flex: 1,
            paddingLeft: theme.spacing(1)
        },
        iconButton: {
            backgroundColor: theme.palette.secondary.main,
            borderRadius: '0 4px 4px 0',
            padding: theme.spacing(1),
            transition: 'all 0s',
            '&:hover': {
                filter: 'invert(100%)',
            }
        },
        error: {
            border: `2px solid ${theme.palette.error.main}`
        }
    })
);

interface NamespaceInputProps {
    onSubmit?: (namespaceName: string) => void;
    onChange: (namespaceName: string) => void;
    hideIconButton?: boolean;
    error?: boolean;
    autoFocus?: boolean;
}

export const NamespaceInput: FunctionComponent<NamespaceInputProps> = props => {
    const classes = useStyles();
    const [namespaceName, setNamespaceName] = useState('');
    const onChangeNamespaceInput = (ev: React.ChangeEvent<HTMLInputElement>) => {
        const namespaceName = ev.target.value;
        props.onChange(namespaceName);
        setNamespaceName(namespaceName);
    };
    const onSubmit = () => {
        if (props.onSubmit) {
            props.onSubmit(namespaceName);
        }
    };
    return <Paper className={classes.root} classes={{ root: props.error ? classes.error : '' }}>
        <InputBase
            autoFocus={props.autoFocus !== undefined ? props.autoFocus : true}
            className={classes.input}
            placeholder='Namespace'
            onChange={onChangeNamespaceInput}
            onKeyPress={(e: React.KeyboardEvent) => {
                if (e.charCode === 13 && props.onSubmit) {
                    props.onSubmit(namespaceName);
                }
            }}
        />
        {
            props.hideIconButton ? '' :
                <IconButton color='primary' type='submit' className={classes.iconButton} onClick={onSubmit}>
                    <SearchIcon />
                </IconButton>
        }
    </Paper>;
};