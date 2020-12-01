/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, ReactNode } from 'react';
import { Grid, makeStyles } from '@material-ui/core';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';

const useStyles = makeStyles((theme) => ({
    containerRoot: {
        flexWrap: 'nowrap',
        marginBottom: theme.spacing(4),
    },
}));

export const SearchListContainer: FunctionComponent<SearchListContainer.Props> = props => {
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
            <Grid style={{ flex: 4, overflow: 'hidden' }} item container justify='center'>
                <Grid style={{ height: '100%' }} item xs={8}>
                    <DelayedLoadIndicator loading={props.loading} />
                    {props.listContainer}
                </Grid>
            </Grid>
        </Grid>
    </>);
};

export namespace SearchListContainer {
    export interface Props {
        searchContainer: ReactNode[];
        listContainer: ReactNode;
        loading: boolean;
    }
}
