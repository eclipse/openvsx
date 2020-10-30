import React, { FunctionComponent } from 'react';
import clsx from 'clsx';
import {
    Paper, Card, Grid, Button, Divider, CardActions, CardContent, Hidden, Collapse
} from '@material-ui/core';
import makeStyles from '@material-ui/core/styles/makeStyles';

const useStyles = makeStyles((theme) => ({
    root: {
        display: 'block',
        width: '100%',
        marginLeft: 'auto',
        marginRight: 'auto',
    },
    cardContent: {
        paddingBottom: theme.spacing(1),
        paddingRight: theme.spacing(1),
        paddingLeft: theme.spacing(2),
        paddingTop: theme.spacing(1) + 2
    },
    flex: {
        flexGrow: 1,
    },
    buttons: {
        whiteSpace: 'nowrap',
        alignSelf: 'flex-end',
        paddingLeft: '90px !important',
    },
    label: {
        alignSelf: 'center',
    }
}));

interface BannerProps {
    open: boolean,
    showDismissButton?: boolean,
    dismissButtonLabel?: string,
    dismissButtonOnClick?: () => void
}

export const Banner: FunctionComponent<BannerProps> = props => {
    const classes = useStyles();

    const renderButtons = <>
        <span className={classes.flex} />

        <Grid item className={classes.buttons}>
            <Button
                variant='outlined'
                onClick={props.dismissButtonOnClick}
            >
                {props.dismissButtonLabel ?? 'Close'}
            </Button>
        </Grid>
    </>;

    return <>
        <Collapse in={props.open}>
            <Paper elevation={0} className={classes.root}>
                <Card elevation={0}>
                    <CardContent
                        className={clsx(
                            classes.cardContent
                        )}
                    >
                        <Grid
                            container
                            wrap='nowrap'
                            spacing={2}
                            direction='row'
                            justify='flex-start'
                            alignItems='flex-start'
                        >
                            <Grid item className={classes.label}>
                                {props.children}
                            </Grid>

                            <Hidden smDown>
                                {renderButtons}
                            </Hidden>
                        </Grid>
                    </CardContent>

                    <Hidden mdUp>
                        <CardActions>
                            {renderButtons}
                        </CardActions>
                    </Hidden>

                    <Hidden smDown>
                        <div />
                    </Hidden>
                </Card>

                <Divider />
            </Paper>
        </Collapse>
    </>;
};
