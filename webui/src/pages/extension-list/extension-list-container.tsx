/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from "react";
import { Box } from "@material-ui/core";
import { RouteComponentProps } from "react-router-dom";
import { createRoute, addQuery } from "../../utils";
import { ExtensionCategory, SortOrder, SortBy } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { PageSettings } from "../../page-settings";
import { ExtensionList } from "./extension-list";
import { ExtensionListHeader } from "./extension-list-header";
import { ErrorResponse } from "../../server-request";

export namespace ExtensionListRoutes {
    export const MAIN = createRoute([]);
}

export class ExtensionListContainer extends React.Component<ExtensionListContainer.Props, ExtensionListContainer.State> {

    constructor(props: ExtensionListContainer.Props) {
        super(props);
        this.state = {
            searchQuery: '',
            category: '',
            resultNumber: 0,
            sortBy: 'relevance',
            sortOrder: 'desc'
        };
    }

    componentDidMount() {
        document.title = this.props.pageSettings.pageTitle;
        const searchParams = new URLSearchParams(this.props.location.search);
        const search = searchParams.get('search');
        const category = searchParams.get('category') as ExtensionCategory;
        this.setState({
            searchQuery: search || '',
            category: category || ''
        });
    }

    protected onSearchChanged = (searchQuery: string) => {
        this.setState({ searchQuery });
        this.updateURL(searchQuery, this.state.category, this.state.sortBy, this.state.sortOrder);
    }
    protected onCategoryChanged = (category: ExtensionCategory) => {
        this.setState({ category });
        this.updateURL(this.state.searchQuery, category, this.state.sortBy, this.state.sortOrder);
    }
    protected onSortByChanged = (sortBy: SortBy) => {
        this.setState({ sortBy });
        this.updateURL(this.state.searchQuery, this.state.category, sortBy, this.state.sortOrder);
    }
    protected onSortOrderChanged = (sortOrder: SortOrder) => {
        this.setState({ sortOrder });
        this.updateURL(this.state.searchQuery, this.state.category, this.state.sortBy, sortOrder);
    }

    protected updateURL(searchQuery: string, category: ExtensionCategory | '', sortBy?: SortBy, sortOrder?: SortOrder) {
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

    protected handleUpdate = (resultNumber: number) => this.doHandleUpdate(resultNumber);
    protected doHandleUpdate(resultNumber: number) {
        this.setState({ resultNumber });
    }

    render() {
        return <Box display='flex' flexDirection='column' >
            <ExtensionListHeader
                resultNumber={this.state.resultNumber}
                searchQuery={this.state.searchQuery}
                category={this.state.category}
                onSearchChanged={this.onSearchChanged}
                onCategoryChanged={this.onCategoryChanged}
                onSortByChanged={this.onSortByChanged}
                onSortOrderChanged={this.onSortOrderChanged}
                pageSettings={this.props.pageSettings}
                service={this.props.service} />
            <ExtensionList
                service={this.props.service}
                pageSettings={this.props.pageSettings}
                filter={{
                    query: this.state.searchQuery, category: this.state.category, offset: 0, size: 10,
                    sortBy: this.state.sortBy, sortOrder: this.state.sortOrder
                }}
                handleError={this.props.handleError}
                onUpdate={this.handleUpdate}
            />
        </Box>;
    }
}

export namespace ExtensionListContainer {
    export interface Props extends RouteComponentProps {
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
        handleError: (err: Error | Partial<ErrorResponse>) => void;
    }
    export interface State {
        searchQuery: string,
        category: ExtensionCategory | '',
        resultNumber: number,
        sortBy: SortBy,
        sortOrder: SortOrder
    }
}
