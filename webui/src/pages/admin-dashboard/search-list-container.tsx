import React, { FunctionComponent, ReactNode } from 'react';
import { Grid, makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
    containerRoot: {
        height: '100%'
    },
}));

interface SearchListContainerProps {
    searchContainer: ReactNode;
    listContainer: ReactNode;
}

export const SearchListContainer: FunctionComponent<SearchListContainerProps> = props => {
    const classes = useStyles();
    return (<>
        <Grid container direction='column' spacing={2} classes={{ root: classes.containerRoot }}>
            <Grid style={{ flex: 1 }} item container alignItems='flex-end' justify='center'>
                <Grid item xs={8}>
                    {props.searchContainer}
                </Grid>
            </Grid>
            <Grid style={{ flex: 4 }} item container justify='center'>
                <Grid item xs={8}>
                    {props.listContainer}
                </Grid>
            </Grid>
        </Grid>
    </>);
};