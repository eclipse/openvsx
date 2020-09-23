import React, { FunctionComponent, useState } from 'react';
import { Paper, InputBase, IconButton, makeStyles } from '@material-ui/core';
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
        }
    })
);

interface NamespaceInputProps {
    onSubmit: (namespaceName: string) => void;
    onChange: (namespaceName: string) => void;
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
        props.onSubmit(namespaceName);
    };
    return <Paper className={classes.root}>
        <InputBase
            autoFocus
            className={classes.input}
            placeholder='Namespace'
            onChange={onChangeNamespaceInput}
            onKeyPress={(e: React.KeyboardEvent) => {
                if (e.charCode === 13) {
                    props.onSubmit(namespaceName);
                }
            }}
        />
        <IconButton color='primary' type='submit' className={classes.iconButton} onClick={onSubmit}>
            <SearchIcon />
        </IconButton>
    </Paper>;
};