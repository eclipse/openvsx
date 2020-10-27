/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as React from 'react';
import * as MarkdownIt from 'markdown-it';
import * as MarkdownItAnchor from 'markdown-it-anchor';
import * as DOMPurify from 'dompurify';
import { Box, Divider, Typography, withStyles, Theme, createStyles, WithStyles } from '@material-ui/core';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { MainContext } from '../../context';
import { ButtonWithProgress } from '../../components/button-with-progress';
import { DelayedLoadIndicator } from '../../components/delayed-load-indicator';
import { Extension, isEqualUser, isError, UserData } from '../../extension-registry-types';
import linkIcon from '../../components/link-icon';

const changesStyles = (theme: Theme) => createStyles({
    changes: {
        display: 'flex',
        marginTop: theme.spacing(2),
        [theme.breakpoints.down('lg')]: {
            flexDirection: 'column-reverse',
        }
    },
    link: {
        textDecoration: 'none',
        color: theme.palette.primary.contrastText,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    header: {
        display: 'flex',
        justifyContent: 'space-between',
        ['@media(max-width: 360px)']: {
            flexDirection: 'column',
            '& > div:first-of-type': {
                marginBottom: '1rem'
            },
            '& button': {
                maxWidth: '12rem',
            }
        },
    },
    markdown: {
        '& a': {
            textDecoration: 'none',
            color: theme.palette.secondary.main,
            '&:hover': {
                textDecoration: 'underline'
            }
        },
        '& a.header-anchor': {
            fill: theme.palette.text.hint,
            opacity: 0.4,
            marginLeft: theme.spacing(0.5),
            '&:hover': {
                opacity: 1
            }
        },
        '& img': {
            maxWidth: '100%'
        },
        '& code': {
            whiteSpace: 'pre',
            backgroundColor: theme.palette.neutral.light,
            padding: 2
        },
        '& h2': {
            borderBottom: '1px solid #eee'
        },
        '& pre': {
            background: theme.palette.neutral.light,
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
                background: theme.palette.neutral.dark
            }
        }
    }
});

class ExtensionDetailChangesComponent extends React.Component<ExtensionDetailChanges.Props, ExtensionDetailChanges.State> {
    static contextType = MainContext;
    declare context: MainContext;

    protected markdownIt: MarkdownIt;

    constructor(props: ExtensionDetailChanges.Props) {
        super(props);
        const anchorPlugin: MarkdownIt.PluginWithOptions<MarkdownItAnchor.AnchorOptions> = (MarkdownItAnchor as any).default;
        this.markdownIt = new MarkdownIt({
            html: true,
            linkify: true,
            typographer: true
        }).use(anchorPlugin, {
            permalink: true,
            permalinkSymbol: linkIcon({ x: 0, y: 0, width: 24, height: 10 })
        });
        this.state = { loading: true, canSync: false, syncing: false };
    }

    protected handleSync = async () => {
        this.setState({ syncing: true });
        try {
            const result = await this.context.service.syncExtensionChangelog(this.props.extension);
            if (isError(result)) {
                throw result;
            }
            this.props.changelogDidUpdate();
        } catch (err) {
            this.context.handleError(err);
        }
    };

    componentDidMount(): void {
        this.updateChanges();
    }

    componentDidUpdate(prevProps: ExtensionDetailChanges.Props) {
        const prevExt = prevProps.extension;
        const newExt = this.props.extension;
        const prevUser = prevProps.user;
        const newUser = this.props.user;

        if (!isEqualUser(prevUser, newUser) ||
            prevExt.namespace !== newExt.namespace ||
            prevExt.name !== newExt.name ||
            prevExt.files.changelog !== newExt.files.changelog) {
            this.setState({ loading: true });
            this.updateChanges();
        }
    }

    protected async updateChanges(): Promise<void> {
        const ns = this.context.user?.namespaces;
        const isOwner = !!ns?.find(o => o.name === this.props.extension.namespace);

        if (this.props.extension.files.changelog) {
            try {
                const changelog = await this.context.service.getExtensionChangelog(this.props.extension);
                this.setState({ canSync: isOwner && !changelog, changelog, loading: false,  syncing: false }, () => this.scrollToHeading());
            } catch (err) {
                this.context.handleError(err);
                this.setState({ canSync: isOwner, loading: false, syncing: false });
            }
        } else {
            this.setState({ canSync: isOwner, changelog: "", loading: false, syncing: false });
        }
    }

    protected scrollToHeading() {
        const anchor = location.hash;
        if (anchor && anchor.length > 1) {
            const heading = document.getElementById(anchor.substring(1));
            if (heading) {
                heading.scrollIntoView();
            }
        }
    }

    render() {
        if (typeof this.state.changelog === 'undefined') {
            return <DelayedLoadIndicator loading={this.state.loading} />;
        }
        const { classes } = this.props;

        return !this.state.changelog ?
            <React.Fragment>
                <Box className={this.props.classes.header} my={2}>
                    <Box>
                        <Typography variant='h5'>
                            Changelog
                        </Typography>
                    </Box>
                    { this.renderButton() }
                </Box>
                <Divider />
                <Box mt={3}>
                    <Typography>No changelog available</Typography>
                </Box>
            </React.Fragment> :
            <React.Fragment>
                <Box className={this.props.classes.changes}>
                    <Box className={classes.markdown} flex={5} overflow='auto'>
                        {this.renderMarkdown(this.state.changelog)}
                    </Box>
                </Box>
            </React.Fragment>;
    }

    protected renderButton(): React.ReactNode {
        return this.state.canSync
            ? <ButtonWithProgress
                working={this.state.syncing}
                onClick={this.handleSync} >
                Refresh
              </ButtonWithProgress>
            : null;
    }

    protected renderMarkdown(md: string): React.ReactNode {
        const renderedMd = this.markdownIt.render(md);
        const sanitized = DOMPurify.sanitize(renderedMd);
        return <span dangerouslySetInnerHTML={{ __html: sanitized }} />;
    }
}

export namespace ExtensionDetailChanges {
    export interface Props extends WithStyles<typeof changesStyles>, RouteComponentProps {
        extension: Extension;
        user?: UserData;
        changelogDidUpdate: () => void;
    }
    export interface State {
        changelog?: string;
        loading: boolean;
        syncing: boolean;
        canSync: boolean;
    }
}

export const ExtensionDetailChanges = withStyles(changesStyles)(withRouter(ExtensionDetailChangesComponent));
