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
import HomeIcon from '@material-ui/icons/Home';
import GitHubIcon from '@material-ui/icons/GitHub';
import BugReportIcon from '@material-ui/icons/BugReport';
import QuestionAnswerIcon from '@material-ui/icons/QuestionAnswer';
import { toLocalTime, addQuery, createRoute } from "../../utils";
import { Optional } from "../../custom-mui-components/optional";
import { DelayedLoadIndicator } from "../../custom-mui-components/delayed-load-indicator";
import { ExtensionRegistryService } from "../../extension-registry-service";
import { Extension, ExtensionReference } from "../../extension-registry-types";
import { ExtensionListRoutes } from "../extension-list/extension-list-container";
import { PageSettings } from "../../page-settings";
import { ExtensionDetailRoutes } from "./extension-detail";
import { ErrorResponse } from '../../server-request';

const overviewStyles = (theme: Theme) => createStyles({
    overview: {
        display: 'flex',
        [theme.breakpoints.down('lg')]: {
            flexDirection: 'column',
        }
    },
    resourcesWrapper: {
        margin: "4rem 0",
        width: "100%",
        [theme.breakpoints.up('lg')]: {
            margin: '0 0 0 4.8rem',
        }
    },
    link: {
        textDecoration: 'none',
        color: theme.palette.primary.contrastText,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    markdown: {
        '& a': {
            textDecoration: 'none',
            color: theme.palette.secondary.main,
            '&:hover': {
                textDecoration: 'underline'
            }
        },
        '& img': {
            maxWidth: '100%'
        },
        '& code': {
            whiteSpace: 'pre',
            backgroundColor: '#f2f2f2',
            padding: 2
        },
        '& h2': {
            borderBottom: '1px solid #eee'
        },
        '& pre': {
            background: '#f2f2f2',
            padding: '10px 5px',
            '& code': {
                padding: 0,
                background: 'inherit'
            }
        },
        '& table': {
            borderCollapse: 'collapse',
            borderSpacing: 0,
            '& tr, & td, & th': {
                border: '1px solid #ddd'
            },
            '& td, & th': {
                padding: '6px 8px'
            },
            '& th': {
                textAlign: 'start',
                background: '#eee'
            }
        }
    },
    resourceLink: {
        display: 'flex',
        alignItems: 'center',
        marginTop: theme.spacing(0.5)
    },
    categoryButton: {
        fontWeight: 'normal',
        textTransform: 'none',
        marginRight: theme.spacing(0.5),
        marginBottom: theme.spacing(0.5),
        padding: '1px 6px'
    },
    downloadButton: {
        marginTop: theme.spacing(2)
    },
    code: {
        fontFamily: 'monospace',
        fontSize: '.85rem',
        [theme.breakpoints.down('lg')]: {
            fontSize: '.7rem'
        }
    },
    moreInfo: {
        maxWidth: '30rem',
    }
});

class ExtensionDetailOverviewComponent extends React.Component<ExtensionDetailOverview.Props, ExtensionDetailOverview.State> {

    protected markdownIt: MarkdownIt;

    constructor(props: ExtensionDetailOverview.Props) {
        super(props);
        this.markdownIt = new MarkdownIt({
            html: true,
            linkify: true,
            typographer: true
        });
        this.state = { loading: true };
    }

    componentDidMount(): void {
        this.updateReadme();
    }

    protected async updateReadme(): Promise<void> {
        if (this.props.extension.files.readme) {
            try {
                const readme = await this.props.service.getExtensionReadme(this.props.extension);
                this.setState({ readme, loading: false });
            } catch (err) {
                this.props.setError(err);
                this.setState({ loading: false });
            }
        } else {
            this.setState({ readme: '## No README available', loading: false });
        }
    }

    render() {
        if (!this.state.readme) {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }
        const { classes, extension } = this.props;
        const zonedDate = toLocalTime(extension.timestamp);
        const ClaimNamespace = this.props.pageSettings.claimNamespace;
        const ReportAbuse = this.props.pageSettings.reportAbuse;
        return <React.Fragment>
            <Box className={this.props.classes.overview}>
                <Box className={classes.markdown} flex={5} overflow="auto">
                    {this.renderMarkdown(this.state.readme)}
                </Box>
                <Box flex={3} display='flex' justifyContent='flex-end' minWidth='290px'>
                    <Box className={this.props.classes.resourcesWrapper}>
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
                            <Button variant='contained' color='secondary'
                                href={extension.files.download}
                                className={this.props.classes.downloadButton} >
                                Download
                            </Button>
                        </Box>
                        <Optional enabled={extension.bundledExtensions !== undefined && extension.bundledExtensions.length > 0}>
                            <Box mt={2}>
                                <Typography variant='h6'>Bundled Extensions</Typography>
                                {extension.bundledExtensions!.map(ref => this.renderExtensionRef(ref))}
                            </Box>
                        </Optional>
                        <Optional enabled={extension.dependencies !== undefined && extension.dependencies.length > 0}>
                            <Box mt={2}>
                                <Typography variant='h6'>Dependencies</Typography>
                                {extension.dependencies!.map(ref => this.renderExtensionRef(ref))}
                            </Box>
                        </Optional>
                        <Box mt={2} className={this.props.classes.moreInfo}>
                            <Typography variant='h6'>More Info</Typography>
                            {extension.version ? this.renderInfo('Version', extension.version + (extension.preview ? ' (preview)' : '')) : ''}
                            {zonedDate ? this.renderInfo('Released on', zonedDate.toLocaleString()) : ''}
                            {this.renderInfo('Namespace',
                                <RouteLink
                                    to={addQuery(ExtensionListRoutes.MAIN, [{ key: 'search', value: extension.namespace }])}
                                    className={this.props.classes.link}>
                                    {extension.namespace}
                                </RouteLink>)}
                            {this.renderInfo('Access Type', this.renderAccessInfo(extension))}
                            {this.renderInfo('Unique Identifier',
                                <span className={this.props.classes.code}>{extension.namespace}.{extension.name}</span>)}
                        </Box>
                        <Box mt={2}>
                            {ClaimNamespace ? <ClaimNamespace extension={extension} className={this.props.classes.resourceLink} /> : ''}
                            {ReportAbuse ? <ReportAbuse extension={extension} className={this.props.classes.resourceLink} /> : ''}
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
            <Link href={href} target='_blank' variant='body2' color='secondary'
                className={this.props.classes.resourceLink} >
                <Optional enabled={label === 'Homepage'}>
                    <HomeIcon fontSize='small' />&nbsp;
                </Optional>
                <Optional enabled={label === 'Repository' && href.startsWith('https://github.com/')}>
                    <GitHubIcon fontSize='small' />&nbsp;
                </Optional>
                <Optional enabled={label === 'Bugs'}>
                    <BugReportIcon fontSize='small' />&nbsp;
                </Optional>
                <Optional enabled={label === 'Q\'n\'A'}>
                    <QuestionAnswerIcon fontSize='small' />&nbsp;
                </Optional>
                {label}
            </Link>
        </Box>;
    }

    protected renderExtensionRef(ref: ExtensionReference): React.ReactNode {
        return <Box>
            <RouteLink to={createRoute([ExtensionDetailRoutes.ROOT, ref.namespace, ref.extension])}
                className={this.props.classes.link} >
                {ref.namespace}.{ref.extension}
            </RouteLink>
        </Box>;
    }

    protected renderInfo(key: string, value: React.ReactNode): React.ReactNode {
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
        setError: (err: Error | Partial<ErrorResponse>) => void;
    }
    export interface State {
        readme?: string;
        loading: boolean;
    }
}

export const ExtensionDetailOverview = withStyles(overviewStyles)(withRouter(ExtensionDetailOverviewComponent));
