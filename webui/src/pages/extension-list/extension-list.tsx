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
import * as InfiniteScroll from "react-infinite-scroller";
import { Grid, Theme, createStyles, withStyles, WithStyles, CircularProgress, Container } from "@material-ui/core";
import { ExtensionListItem } from "./extension-list-item";
import { ExtensionRaw, isError } from "../../extension-registry-types";
import { ExtensionRegistryService, ExtensionFilter } from "../../extension-registry-service";
import { debounce } from "../../utils";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { PageSettings } from "../../page-settings";
import { ErrorResponse } from '../../server-request';

const itemStyles = (theme: Theme) => createStyles({
    container: {
        justifyContent: 'center'
    },
    loader: {
        display: 'flex',
        justifyContent: 'center',
        margin: theme.spacing(3)
    }
});

export class ExtensionListComponent extends React.Component<ExtensionListComponent.Props, ExtensionListComponent.State> {

    protected cancellationToken: { timeout?: number } = {};
    protected filterSize: number;
    protected enableLoadMore: boolean;
    protected lastRequestedPage: number = 0;
    protected pageOffset: number = 0;

    constructor(props: ExtensionListComponent.Props) {
        super(props);
        this.filterSize = this.props.filter.size || 10;
        this.state = {
            extensions: [],
            extensionKeys: new Set(),
            appliedFilter: {},
            hasMore: false,
            loading: true
        };
    }

    componentDidMount() {
        this.enableLoadMore = true;
        this.componentDidUpdate({ filter: {} } as ExtensionListComponent.Props);
    }

    componentWillUnmount() {
        clearTimeout(this.cancellationToken.timeout);
        this.enableLoadMore = false;
    }

    componentDidUpdate(prevProps: ExtensionListComponent.Props) {
        const newFilter = copyFilter(this.props.filter);
        if (isSameFilter(prevProps.filter, newFilter)) {
            return;
        }
        this.filterSize = newFilter.size || this.filterSize;
        debounce(
            async () => {
                try {
                    const result = await this.props.service.search(newFilter);
                    if (isError(result)) {
                        throw result;
                    }
                    this.props.onUpdate(result.totalSize);
                    const actualSize = result.extensions.length;
                    this.pageOffset = this.lastRequestedPage;
                    const extensionKeys = new Set<string>();
                    for (const ext of result.extensions) {
                        extensionKeys.add(`${ext.namespace}.${ext.name}`);
                    }
                    this.setState({
                        extensions: result.extensions,
                        extensionKeys,
                        appliedFilter: newFilter,
                        hasMore: actualSize < result.totalSize && actualSize > 0,
                        loading: false
                    });
                } catch (err) {
                    this.props.handleError(err);
                    this.setState({ loading: false });
                }
            },
            this.cancellationToken
        );
    }

    protected loadMore = async (p: number) => {
        this.lastRequestedPage = p;
        const filter = copyFilter(this.state.appliedFilter);
        if (!isSameFilter(this.props.filter, filter)) {
            return;
        }
        try {
            filter.offset = (p - this.pageOffset) * this.filterSize;
            const result = await this.props.service.search(filter);
            if (isError(result)) {
                throw result;
            }
            if (this.enableLoadMore && isSameFilter(this.props.filter, filter)) {
                const extensions = this.state.extensions;
                const extensionKeys = this.state.extensionKeys;
                // Check for duplicate keys to avoid problems due to asynchronous user edit / loadMore call
                for (const ext of result.extensions) {
                    const key = `${ext.namespace}.${ext.name}`;
                    if (!extensionKeys.has(key)) {
                        extensions.push(ext);
                        extensionKeys.add(key);
                    }
                }
                this.setState({
                    extensions,
                    extensionKeys,
                    hasMore: extensions.length < result.totalSize && result.extensions.length > 0
                });
            }
        } catch (err) {
            this.props.handleError(err);
        }
    }

    render() {
        const extensionList = this.state.extensions.map((ext, idx) => (
            <ExtensionListItem
                idx={idx}
                extension={ext}
                filterSize={this.filterSize}
                pageSettings={this.props.pageSettings}
                key={`${ext.namespace}.${ext.name}`} />
        ));
        const loader = <div key='extension-list-loader' className={this.props.classes.loader}>
            <CircularProgress size='3rem' color='secondary' />
        </div>;
        return <React.Fragment>
            <DelayedLoadIndicator loading={this.state.loading}/>
            <InfiniteScroll
                loadMore={this.loadMore}
                hasMore={this.state.hasMore}
                loader={loader}
                threshold={200} >
                <Container maxWidth="xl">
                    <Grid container spacing={2} className={this.props.classes.container}>
                        {extensionList}
                    </Grid>
                </Container>
            </InfiniteScroll>
        </React.Fragment>;
    }
}

export namespace ExtensionListComponent {
    export interface Props extends WithStyles<typeof itemStyles> {
        filter: ExtensionFilter;
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
        handleError: (err: Error | Partial<ErrorResponse>) => void;
        onUpdate: (resultNumber: number) => void;
    }
    export interface State {
        extensions: ExtensionRaw[];
        extensionKeys: Set<string>;
        appliedFilter: ExtensionFilter;
        hasMore: boolean;
        loading: boolean;
    }
}

export const ExtensionList = withStyles(itemStyles)(ExtensionListComponent);

function isSameFilter(f1: ExtensionFilter, f2: ExtensionFilter): boolean {
    return f1.category === f2.category && f1.query === f2.query && f1.sortBy === f2.sortBy && f1.sortOrder === f2.sortOrder;
}

function copyFilter(f: ExtensionFilter): ExtensionFilter {
    return {
        query: f.query,
        category: f.category,
        size: f.size,
        offset: f.offset,
        sortBy: f.sortBy,
        sortOrder: f.sortOrder
    };
}
