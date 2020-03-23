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
import { Grid } from "@material-ui/core";
import { ExtensionListItem } from "./extension-list-item";
import { ExtensionRaw, SearchResult, isError, ErrorResult } from "../../extension-registry-types";
import { ExtensionRegistryService, ExtensionFilter } from "../../extension-registry-service";
import { debounce, handleError } from "../../utils";
import { PageSettings } from "../../page-settings";

export class ExtensionList extends React.Component<ExtensionList.Props, ExtensionList.State> {

    protected extensions: ExtensionRaw[];

    protected cancellationToken: { cancel?: () => void, timeout?: number } = {};

    constructor(props: ExtensionList.Props) {
        super(props);

        this.state = {
            extensions: []
        };
    }

    componentDidMount() {
        this.getExtensions(this.props.filter).then(this.handleSearchResult, handleError);
    }

    componentDidUpdate(prevProps: ExtensionList.Props, prevState: ExtensionList.State) {
        const prevFilter = prevProps.filter;
        const newFilter = this.props.filter;
        if (prevFilter.category !== newFilter.category || prevFilter.query !== newFilter.query) {
            if (this.cancellationToken.cancel) {
                this.cancellationToken.cancel();
                this.cancellationToken.cancel = undefined;
            }
            debounce(
                () => this.props.service.search(newFilter).then(this.handleSearchResult, handleError),
                this.cancellationToken
            );
        }
    }

    protected getExtensions(filter: ExtensionFilter): Promise<SearchResult | ErrorResult> {
        return new Promise((resolve, reject) => {
            this.cancellationToken.cancel = reject;
            this.props.service.search(filter).then(resolve, reject);
        });
    }

    protected handleSearchResult = (result: SearchResult | ErrorResult) => {
        if (isError(result)) {
            handleError(result);
        } else {
            this.setState({ extensions: result.extensions });
        }
    }

    render() {
        const extensionList = this.state.extensions.map((ext, idx) => {
            return <ExtensionListItem
                idx={idx}
                extension={ext}
                service={this.props.service}
                pageSettings={this.props.pageSettings}
                key={`${ext.namespace}.${ext.name}`} />;
        });
        return <Grid container spacing={2}>
            {extensionList}
        </Grid>;
    }
}

export namespace ExtensionList {
    export interface Props {
        filter: ExtensionFilter;
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }
    export interface State {
        extensions: ExtensionRaw[];
    }
}
