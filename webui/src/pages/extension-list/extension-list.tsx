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
import { Grid, Box, Theme, createStyles, withStyles, WithStyles, CircularProgress, Container } from "@material-ui/core";
import { ExtensionListItem } from "./extension-list-item";
import { ExtensionRaw, SearchResult, isError, ErrorResult } from "../../extension-registry-types";
import { ExtensionRegistryService, ExtensionFilter } from "../../extension-registry-service";
import { debounce, handleError } from "../../utils";
import { PageSettings } from "../../page-settings";
import * as InfiniteScroll from "react-infinite-scroller";

const itemStyles = (theme: Theme) => createStyles({
    container: {
        justifyContent: 'center'
    },
    loader: {
        display: 'flex',
        justifyContent: 'center',
        margin: 5
    }
});

export class ExtensionListComponent extends React.Component<ExtensionListComponent.Props, ExtensionListComponent.State> {

    protected cancellationToken: { cancel?: () => void, timeout?: number } = {};

    constructor(props: ExtensionListComponent.Props) {
        super(props);

        this.state = {
            extensions: [],
            hasMore: false
        };
    }

    componentDidMount() {
        this.getExtensions(this.props.filter).then(this.handleSearchResult, handleError);
    }

    componentDidUpdate(prevProps: ExtensionListComponent.Props, prevState: ExtensionListComponent.State) {
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
            this.setState({ extensions: result.extensions, hasMore: result.extensions.length < result.totalSize });
        }
    }

    protected loadMore = async (p: number) => {
        const filter: ExtensionFilter = this.props.filter;
        filter.offset = (p * (filter.size || 18));
        const result = await this.getExtensions(filter);
        if (isError(result)) {
            handleError(result);
        } else {
            const extensions = this.state.extensions;
            extensions.push(...result.extensions);
            this.setState({ extensions, hasMore: extensions.length < result.totalSize });
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
        return <Box height='100%' overflow='auto'>
            <InfiniteScroll
                loadMore={this.loadMore}
                hasMore={this.state.hasMore}
                loader={<div key='vsx-list-loader' className={this.props.classes.loader}><CircularProgress size={20} color='secondary' /></div>}
                threshold={50}
                useWindow={false}
            >
                <Container>
                    <Grid container spacing={2} className={this.props.classes.container}>
                        {extensionList}
                    </Grid>
                </Container>
            </InfiniteScroll>
        </Box>;
    }
}

export namespace ExtensionListComponent {
    export interface Props extends WithStyles<typeof itemStyles> {
        filter: ExtensionFilter;
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }
    export interface State {
        extensions: ExtensionRaw[];
        hasMore: boolean;
    }
}

export const ExtensionList = withStyles(itemStyles)(ExtensionListComponent);