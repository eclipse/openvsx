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
import { ExtensionListHeader } from "./extension-list-header";
import { ExtensionCategory } from "../../extension-registry-types";
import { ExtensionList } from "./extension-list";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { createURL } from "../../utils";
import { RouteComponentProps } from "react-router-dom";

export const EXTENSION_LIST_COMPONENT_NAME = 'extension-list';

export namespace ExtensionListRoutes {
    export const EXTENSION_LIST_LINK = createURL([EXTENSION_LIST_COMPONENT_NAME]);
}

export class ExtensionListContainer extends React.Component<ExtensionListContainer.Props, ExtensionListContainer.State> {

    constructor(props: ExtensionListContainer.Props) {
        super(props);
        this.state = {
            searchTerm: '',
            category: ''
        };
    }

    componentDidMount() {
        const searchParams = new URLSearchParams(this.props.location.search);
        const search = searchParams.get('search');
        const category = searchParams.get('category') as ExtensionCategory;
        this.setState({
            searchTerm: search || '',
            category: category || ''
        });
    }

    protected onSearchChanged = (searchTerm: string) => {
        this.setState({ searchTerm });
    }
    protected onCategoryChanged = (category: ExtensionCategory) => {
        this.setState({ category });
    }

    render() {
        return <React.Fragment>
            <Container>
                <ExtensionListHeader
                    searchTerm={this.state.searchTerm}
                    category={this.state.category}
                    onSearchChanged={this.onSearchChanged}
                    onCategoryChanged={this.onCategoryChanged}
                    listHeaderTitle={this.props.listHeaderTitle} />
                <ExtensionList service={this.props.service} filter={{ query: this.state.searchTerm, category: this.state.category }} />
            </Container>
        </React.Fragment>;
    }
}

export namespace ExtensionListContainer {
    export interface Props extends RouteComponentProps {
        service: ExtensionRegistryService;
        listHeaderTitle: string;
    }
    export interface State {
        searchTerm: string,
        category: ExtensionCategory
    }
}