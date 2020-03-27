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
import { Container } from "@material-ui/core";
import { RouteComponentProps } from "react-router-dom";
import { createRoute, addQuery } from "../../utils";
import { ExtensionCategory } from "../../extension-registry-types";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { PageSettings } from "../../page-settings";
import { ExtensionList } from "./extension-list";
import { ExtensionListHeader } from "./extension-list-header";

export namespace ExtensionListRoutes {
    export const MAIN = createRoute([]);
}

export class ExtensionListContainer extends React.Component<ExtensionListContainer.Props, ExtensionListContainer.State> {

    constructor(props: ExtensionListContainer.Props) {
        super(props);
        this.state = {
            searchQuery: '',
            category: ''
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
        this.updateURL(searchQuery, this.state.category);
    }
    protected onCategoryChanged = (category: ExtensionCategory) => {
        this.setState({ category });
        this.updateURL(this.state.searchQuery, category);
    }

    protected updateURL(searchQuery: string, category: ExtensionCategory | '') {
        const queries: { key: string, value: string }[] = [
            { key: 'search', value: searchQuery },
            { key: 'category', value: category }
        ];
        const url = addQuery('', queries) || location.pathname || '/';
        history.replaceState(null, '', url);
    }

    render() {
        return <React.Fragment>
            <Container>
                <ExtensionListHeader
                    searchQuery={this.state.searchQuery}
                    category={this.state.category}
                    onSearchChanged={this.onSearchChanged}
                    onCategoryChanged={this.onCategoryChanged}
                    pageSettings={this.props.pageSettings}
                    service={this.props.service} />
                <ExtensionList
                    service={this.props.service}
                    pageSettings={this.props.pageSettings}
                    filter={{ query: this.state.searchQuery, category: this.state.category }} />
            </Container>
        </React.Fragment>;
    }
}

export namespace ExtensionListContainer {
    export interface Props extends RouteComponentProps {
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }
    export interface State {
        searchQuery: string,
        category: ExtensionCategory | ''
    }
}
