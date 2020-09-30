import React, { FunctionComponent, ReactNode } from 'react';
import { Grid, makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
    containerRoot: {
        height: '100%'
    },
}));

interface SearchListContainerProps {
    searchContainer: ReactNode[];
    listContainer: ReactNode;
}

export const SearchListContainer: FunctionComponent<SearchListContainerProps> = props => {
    const classes = useStyles();
    return (<>
        <Grid container direction='column' spacing={2} classes={{ root: classes.containerRoot }}>
            <Grid style={{ flex: 1 }} item container direction='column' spacing={1} justify='flex-end'>
                {props.searchContainer.map((searchField, key) => {
                    return <Grid key={key} container item justify='center'>
                        <Grid item xs={8}>
                            {searchField}
                        </Grid>
                    </Grid>;
                })}
            </Grid>
            <Grid style={{ flex: 4 }} item container justify='center'>
                <Grid item xs={8}>
                    {props.listContainer}
                </Grid>
            </Grid>
        </Grid>
    </>);
};