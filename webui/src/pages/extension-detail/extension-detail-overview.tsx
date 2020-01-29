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
import { Box, withStyles, Theme, createStyles, WithStyles, Typography, Button, Link } from "@material-ui/core";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension } from "../../extension-registry-types";
import * as MarkdownIt from 'markdown-it';
import { utcToZonedTime } from "date-fns-tz";
import { ExtensionListRoutes } from "../extension-list/extension-list-container";
import { createURL, handleError } from "../../utils";
import { RouteComponentProps } from "react-router-dom";

const overviewStyles = (theme: Theme) => createStyles({
    markdown: {
        '& img': {
            maxWidth: '100%'
        }
    },
    categoryButton: {
        fontWeight: 'normal',
        textTransform: 'none',
        marginRight: theme.spacing(0.5),
        marginBottom: theme.spacing(0.5),
        padding: '1px 6px'
    }
});

class ExtensionDetailOverviewComponent extends React.Component<ExtensionDetailOverview.Props, ExtensionDetailOverview.State> {

    protected markdownIt: MarkdownIt;

    constructor(props: ExtensionDetailOverview.Props) {
        super(props);
        this.markdownIt = new MarkdownIt('commonmark');
        this.state = {};
    }

    componentDidMount() {
        this.init();
    }

    protected async init() {
        if (this.props.extension.readmeUrl) {
            try {
                const readme = await this.props.service.getExtensionReadme(this.props.extension.readmeUrl);
                this.setState({ readme });
            } catch (err) {
                handleError(err);
            }
        } else {
            this.setState({ readme: '## No README available' });
        }
    }

    render() {
        if (!this.state.readme) {
            return '';
        }
        const { classes, extension } = this.props;
        let zonedDate;
        if (extension.timestamp) {
            const date = new Date(extension.timestamp);
            const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
            zonedDate = utcToZonedTime(date, timeZone);
        }
        return <React.Fragment>
            <Box display='flex' >
                <Box className={classes.markdown} flex={5}>
                    {this.renderMarkdown(this.state.readme)}
                </Box>
                <Box flex={3} display='flex' justifyContent='flex-end'>
                    <Box width='80%'>
                        {this.renderButtonList('category', 'Categories', extension.categories)}
                        <Box mt={2}>
                            {this.renderButtonList('search', 'Tags', extension.tags)}
                        </Box>
                        <Box mt={2}>
                            <Typography variant='h6'>Resources</Typography>
                            {this.renderResourceLink('Homepage', extension.homepage)}
                            {this.renderResourceLink('Repository', extension.repository)}
                            {this.renderResourceLink('Bugs', extension.bugs)}
                            {this.renderResourceLink('Q\'n\'A', extension.qna)}
                            {this.renderResourceLink('Download', extension.downloadUrl)}
                        </Box>
                        <Box mt={2}>
                            <Typography variant='h6'>More Info</Typography>
                            {this.renderInfo('Publisher', extension.publisher)}
                            {extension.version ? this.renderInfo('Version', extension.version) : ''}
                            {zonedDate ? this.renderInfo('Date', zonedDate.toLocaleString()) : ''}
                        </Box>
                    </Box>
                </Box>
            </Box>
        </React.Fragment>;
    }

    protected handleFilterButtonClicked = (kind: 'category' | 'search', buttonLabel: string) => {
        this.props.history.push(createURL([ExtensionListRoutes.EXTENSION_LIST_LINK], [{ key: kind, value: buttonLabel }]));
    }

    protected renderButtonList(kind: 'category' | 'search', title: string, arr?: string[]) {
        return arr ?
            <Box>
                <Typography variant='h6'>{title}</Typography>
                {
                    arr.map((buttonLabel: string) =>
                        <Button
                            className={this.props.classes.categoryButton}
                            size='small'
                            key={buttonLabel}
                            variant='outlined'
                            onClick={() => this.handleFilterButtonClicked(kind, buttonLabel)}
                        >
                            {buttonLabel}
                        </Button>)
                }
            </Box> : '';
    }

    protected renderResourceLink(label: string, href?: string) {
        return href ? <Box><Link href={href} target='_blank' variant='body2' color='secondary'>{label}</Link></Box> : '';
    }

    protected renderInfo(key: string, value: string) {
        return <Box display='flex'>
            <Box flex='1'>
                <Typography variant='body2'>{key}</Typography>
            </Box>
            <Box flex='1'>
                <Typography variant='body2'>{value}</Typography>
            </Box>
        </Box>;
    }

    protected renderMarkdown(md: string) {
        const renderedMd = this.markdownIt.render(md);
        return <span dangerouslySetInnerHTML={{ __html: renderedMd }} />;
    }
}

export namespace ExtensionDetailOverview {
    export interface Props extends WithStyles<typeof overviewStyles>, RouteComponentProps {
        extension: Extension,
        service: ExtensionRegistryService
    }
    export interface State {
        readme?: string
    }
}

export const ExtensionDetailOverview = withStyles(overviewStyles)(ExtensionDetailOverviewComponent);
