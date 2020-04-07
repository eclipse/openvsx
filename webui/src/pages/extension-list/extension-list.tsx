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
import { debounce, handleError, stringHash } from "../../utils";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { PageSettings } from "../../page-settings";

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
                    const actualSize = result.extensions.length;
                    this.pageOffset = this.lastRequestedPage;
                    this.setState({
                        extensions: result.extensions,
                        appliedFilter: newFilter,
                        hasMore: actualSize < result.totalSize && actualSize >= this.filterSize,
                        loading: false
                    });
                } catch (err) {
                    handleError(err);
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
                extensions.push(...result.extensions);
                this.setState({
                    extensions,
                    hasMore: extensions.length < result.totalSize && result.extensions.length >= this.filterSize
                });
            }
        } catch (err) {
            handleError(err);
        }
    }

    render() {
        const filterHash = stringHash(this.state.appliedFilter.category, this.state.appliedFilter.query);
        const extensionList = this.state.extensions.map((ext, idx) => (
            <ExtensionListItem
                idx={idx}
                extension={ext}
                filterSize={this.filterSize}
                pageSettings={this.props.pageSettings}
                key={`${ext.namespace}.${ext.name}#${filterHash}`} />
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
                threshold={200}
                useWindow={false} >
                <Container>
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
    }
    export interface State {
        extensions: ExtensionRaw[];
        appliedFilter: ExtensionFilter;
        hasMore: boolean;
        loading: boolean;
    }
}

export const ExtensionList = withStyles(itemStyles)(ExtensionListComponent);

function isSameFilter(f1: ExtensionFilter, f2: ExtensionFilter): boolean {
    return f1.category === f2.category && f1.query === f2.query;
}

function copyFilter(f: ExtensionFilter): ExtensionFilter {
    return {
        query: f.query,
        category: f.category,
        size: f.size,
        offset: f.offset
    };
}
