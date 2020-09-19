import React, { FunctionComponent } from 'react';
import { makeStyles, IconButton, Paper, InputBase, Grid, Card, CardHeader, CardContent, Box } from '@material-ui/core';
import SearchIcon from '@material-ui/icons/Search';
import VisibilitySharpIcon from '@material-ui/icons/VisibilitySharp';

const useStyles = makeStyles(theme =>
    ({
        containerRoot: {
            height: '100%'
        },
        root: {
            padding: '2px 4px',
            display: 'flex',
            alignItems: 'center',
            width: '100%',
        },
        input: {
            marginLeft: theme.spacing(1),
            flex: 1,
        },
        iconButton: {
            padding: 10,
        },
        divider: {
            height: 28,
            margin: 4,
        },
    }),
);

export const NamespaceSearch: FunctionComponent = props => {
    const classes = useStyles();
    return (<>
        <Grid container direction='column' spacing={2} classes={{ root: classes.containerRoot }}>
            <Grid style={{ flex: 1 }} item container alignItems='flex-end' justify='center'>
                <Grid item xs={8}>
                    <Paper className={classes.root}>
                        <InputBase
                            className={classes.input}
                            placeholder='Search Namespaces'
                        />
                        <IconButton type='submit' className={classes.iconButton}>
                            <SearchIcon />
                        </IconButton>
                    </Paper>
                </Grid>
            </Grid>
            <Grid style={{ flex: 4 }} item container justify='center'>
                <Grid item xs={8}>
                    <Card>
                        <CardHeader title='some-namespace' action={
                            <IconButton>
                                <VisibilitySharpIcon />
                            </IconButton>
                        }/>
                        <CardContent>
                            <Box>Owner: some owner</Box>
                            <Box>Extensions: foo, bar, ...</Box>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>
        </Grid>
    </>);
};