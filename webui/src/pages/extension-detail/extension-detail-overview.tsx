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
import * as MarkdownIt from 'markdown-it';
import { Box, withStyles, Theme, createStyles, WithStyles, Typography, Button, Link } from "@material-ui/core";
import { RouteComponentProps, Link as RouteLink, withRouter } from "react-router-dom";
import { handleError, toLocalTime, addQuery } from "../../utils";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension } from "../../extension-registry-types";
import { ExtensionListRoutes } from "../extension-list/extension-list-container";
import { PageSettings } from "../../page-settings";

const overviewStyles = (theme: Theme) => createStyles({
    link: {
        textDecoration: 'none',
        color: theme.palette.text.primary,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
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
    },
    code: {
        fontFamily: 'monospace',
        fontSize: '1rem'
    }
});

class ExtensionDetailOverviewComponent extends React.Component<ExtensionDetailOverview.Props, ExtensionDetailOverview.State> {

    protected markdownIt: MarkdownIt;

    constructor(props: ExtensionDetailOverview.Props) {
        super(props);
        this.markdownIt = new MarkdownIt('commonmark');
        this.state = {};
    }

    componentDidMount(): void {
        this.updateReadme();
    }

    protected async updateReadme(): Promise<void> {
        if (this.props.extension.files.readme) {
            try {
                const readme = await this.props.service.getExtensionReadme(this.props.extension);
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
        const zonedDate = toLocalTime(extension.timestamp);
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
                            {this.renderResourceLink('Download', extension.files.download)}
                        </Box>
                        <Box mt={2}>
                            <Typography variant='h6'>More Info</Typography>
                            {this.renderInfo('Namespace',
                                <RouteLink
                                    to={addQuery(ExtensionListRoutes.MAIN, [{ key: 'search', value: extension.namespace}])}
                                    className={this.props.classes.link}>
                                    {extension.namespace}
                                </RouteLink>)}
                            {this.renderInfo('Write Access', this.renderAccessInfo(extension))}
                            {this.renderInfo('Unique Identifier',
                                <span className={this.props.classes.code}>{extension.namespace}.{extension.name}</span>)}
                            {extension.version ? this.renderInfo('Version', extension.version) : ''}
                            {zonedDate ? this.renderInfo('Date', zonedDate.toLocaleString()) : ''}
                        </Box>
                        <Box mt={2}>
                            {this.props.pageSettings.claimNamespaceHref ?
                                this.renderResourceLink('Claim Ownership', this.props.pageSettings.claimNamespaceHref(extension.namespace))
                                : ''}
                            {this.props.pageSettings.reportAbuseHref ?
                                this.renderResourceLink('Report Abuse', this.props.pageSettings.reportAbuseHref(extension))
                                : ''}
                        </Box>
                    </Box>
                </Box>
            </Box>
        </React.Fragment>;
    }

    protected handleFilterButtonClicked(kind: 'category' | 'search', buttonLabel: string): void {
        const route = addQuery(ExtensionListRoutes.MAIN, [{ key: kind, value: buttonLabel }]);
        this.props.history.push(route);
    }

    protected renderButtonList(kind: 'category' | 'search', title: string, arr?: string[]): React.ReactNode {
        if (!arr || arr.length === 0) {
            return '';
        }
        return <Box>
            <Typography variant='h6'>{title}</Typography>
            {
                arr.sort((a, b) => a.localeCompare(b)).map((buttonLabel: string) =>
                    <Button
                        className={this.props.classes.categoryButton}
                        size='small'
                        key={buttonLabel}
                        variant='outlined'
                        onClick={() => this.handleFilterButtonClicked(kind, buttonLabel)} >
                        {buttonLabel}
                    </Button>)
            }
        </Box>;
    }

    protected renderResourceLink(label: string, href?: string): React.ReactNode {
        if (!href || !(href.startsWith('http') || href.startsWith('mailto'))) {
            return '';
        }
        return <Box>
            <Link href={href} target='_blank' variant='body2' color='secondary'>{label}</Link>
        </Box>;
    }

    protected renderInfo(key: string, value: React.ReactNode) {
        return <Box display='flex'>
            <Box flex='1'>
                <Typography variant='body2'>{key}</Typography>
            </Box>
            <Box flex='1'>
                <Typography variant='body2'>{value}</Typography>
            </Box>
        </Box>;
    }

    protected renderAccessInfo(extension: Extension): React.ReactNode {
        const text = extension.namespaceAccess || 'unknown';
        if (this.props.pageSettings.namespaceAccessInfoURL) {
            return <Link
                href={this.props.pageSettings.namespaceAccessInfoURL}
                target='_blank'
                className={this.props.classes.link}>
                {text}
            </Link>;
        } else {
            return text;
        }
    }

    protected renderMarkdown(md: string): React.ReactNode {
        const renderedMd = this.markdownIt.render(md);
        return <span dangerouslySetInnerHTML={{ __html: renderedMd }} />;
    }
}

export namespace ExtensionDetailOverview {
    export interface Props extends WithStyles<typeof overviewStyles>, RouteComponentProps {
        extension: Extension;
        service: ExtensionRegistryService;
        pageSettings: PageSettings;
    }
    export interface State {
        readme?: string;
    }
}

export const ExtensionDetailOverview = withStyles(overviewStyles)(withRouter(ExtensionDetailOverviewComponent));
