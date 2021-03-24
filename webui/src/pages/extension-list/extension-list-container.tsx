/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import { Box } from '@material-ui/core';
import { RouteComponentProps } from 'react-router-dom';
import { createRoute, addQuery } from '../../utils';
import { ExtensionCategory, SortOrder, SortBy } from '../../extension-registry-types';
import { ExtensionList } from './extension-list';
import { ExtensionListHeader } from './extension-list-header';
import { MainContext } from '../../context';

export namespace ExtensionListRoutes {
    export const MAIN = createRoute([]);
}

export class ExtensionListContainer extends React.Component<ExtensionListContainer.Props, ExtensionListContainer.State> {

    static contextType = MainContext;
    declare context: MainContext;

    constructor(props: ExtensionListContainer.Props) {
        super(props);
        this.state = {
            searchQuery: '',
            category: '',
            resultNumber: 0,
            sortBy: 'relevance',
            sortOrder: 'desc',
            searchDebounceTime: 0
        };
    }

    componentDidMount(): void {
        document.title = this.context.pageSettings.pageTitle;
        const searchParams = new URLSearchParams(this.props.location.search);
        const search = searchParams.get('search');
        const category = searchParams.get('category') as ExtensionCategory;
        const sortBy = searchParams.get('sortBy') as SortBy;
        const sortOrder = searchParams.get('sortOrder') as SortOrder;
        this.setState({
            searchQuery: search || '',
            category: category || '',
            sortBy: sortBy || 'relevance',
            sortOrder: sortOrder || 'desc'
        });
    }

    protected onSearchChanged = (searchQuery: string): void => {
        this.setState({ searchQuery, searchDebounceTime: 1000 });
        this.updateURL(searchQuery, this.state.category, this.state.sortBy, this.state.sortOrder);
    };
    protected onSearchSubmit =  (searchQuery: string): void => {
        this.setState({ searchQuery, searchDebounceTime: 0 });
    };
    protected onCategoryChanged = (category: ExtensionCategory): void => {
        this.setState({ category });
        this.updateURL(this.state.searchQuery, category, this.state.sortBy, this.state.sortOrder);
    };
    protected onSortByChanged = (sortBy: SortBy): void => {
        this.setState({ sortBy });
        this.updateURL(this.state.searchQuery, this.state.category, sortBy, this.state.sortOrder);
    };
    protected onSortOrderChanged = (sortOrder: SortOrder): void => {
        this.setState({ sortOrder });
        this.updateURL(this.state.searchQuery, this.state.category, this.state.sortBy, sortOrder);
    };

    protected updateURL(searchQuery: string, category: ExtensionCategory | '', sortBy?: SortBy, sortOrder?: SortOrder): void {
        const queries: { key: string, value: string }[] = [
            { key: 'search', value: searchQuery },
            { key: 'category', value: category }
        ];
        if (sortBy) {
            queries.push({ key: 'sortBy', value: sortBy });
        }
        if (sortOrder) {
            queries.push({ key: 'sortOrder', value: sortOrder });
        }
        const url = addQuery('', queries) || location.pathname || '/';
        history.replaceState(null, '', url);
    }

    protected handleUpdate = (resultNumber: number): void => this.doHandleUpdate(resultNumber);
    protected doHandleUpdate(resultNumber: number): void {
        this.setState({ resultNumber });
    }

    render(): React.ReactNode {
        return <Box display='flex' flexDirection='column' >
            <ExtensionListHeader
                resultNumber={this.state.resultNumber}
                searchQuery={this.state.searchQuery}
                category={this.state.category}
                sortBy={this.state.sortBy}
                sortOrder={this.state.sortOrder}
                onSearchChanged={this.onSearchChanged}
                onSearchSubmit={this.onSearchSubmit}
                onCategoryChanged={this.onCategoryChanged}
                onSortByChanged={this.onSortByChanged}
                onSortOrderChanged={this.onSortOrderChanged} />
            <ExtensionList
                filter={{
                    query: this.state.searchQuery, category: this.state.category, offset: 0, size: 10,
                    sortBy: this.state.sortBy, sortOrder: this.state.sortOrder
                }}
                debounceTime={this.state.searchDebounceTime}
                onUpdate={this.handleUpdate}
            />
        </Box>;
    }
}

export namespace ExtensionListContainer {
    export interface Props extends RouteComponentProps {
    }
    export interface State {
        searchQuery: string,
        category: ExtensionCategory | '',
        resultNumber: number,
        sortBy: SortBy,
        sortOrder: SortOrder,
        searchDebounceTime: number
    }
}
