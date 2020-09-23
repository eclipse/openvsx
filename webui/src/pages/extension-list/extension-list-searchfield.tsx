import React, { FunctionComponent } from 'react';
import SearchIcon from '@material-ui/icons/Search';
import { Paper, InputBase, IconButton, makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
    search: {
        flex: 2,
        display: 'flex',
        marginRight: theme.spacing(1),
        [theme.breakpoints.down('sm')]: {
            marginRight: 0,
            marginBottom: theme.spacing(2),
        },
    },
    inputBase: {
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
}));

interface ExtensionListSearchfieldProps {
    onSearchChanged: (s: string) => void;
    searchQuery?: string;
}

export const ExtensionListSearchfield: FunctionComponent<ExtensionListSearchfieldProps> = props => {
    const classes = useStyles();

    const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        props.onSearchChanged(event.target.value);
    };

    return (<>
        <Paper className={classes.search}>
            <InputBase
                autoFocus
                value={props.searchQuery || ''}
                onChange={handleSearchChange}
                className={classes.inputBase}
                placeholder='Search by Name, Tag, or Description'
                id='search-input' />
            <label
                htmlFor='search-input'
                className='visually-hidden' >
                Search for Name, Tags or Description
                                </label>
            <IconButton color='primary' aria-label='Search' classes={{ root: classes.iconButton }}>
                <SearchIcon />
            </IconButton>
        </Paper>
    </>);
};