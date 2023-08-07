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
import { Grid } from '@mui/material';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';

export const SearchListContainer: FunctionComponent<SearchListContainerProps> = props => {
    return (<>
        <Grid container direction='column' spacing={2} sx={{ flexWrap: 'nowrap', mb: 4 }}>
            <Grid style={{ flex: 1 }} item container direction='column' spacing={1} justifyContent='flex-end'>
                {props.searchContainer.map((searchField, key) => {
                    return <Grid key={key} container item justifyContent='center'>
                        <Grid item xs={8}>
                            {searchField}
                        </Grid>
                    </Grid>;
                })}
            </Grid>
            <Grid style={{ flex: 4, overflow: 'hidden' }} item container justifyContent='center'>
                <Grid style={{ height: '100%' }} item xs={8}>
                    <DelayedLoadIndicator loading={props.loading} />
                    {props.listContainer}
                </Grid>
            </Grid>
        </Grid>
    </>);
};

export interface SearchListContainerProps {
    searchContainer: ReactNode[];
    listContainer: ReactNode;
    loading: boolean;
}