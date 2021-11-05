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
import { withStyles, Theme, createStyles, WithStyles } from '@material-ui/core';
import linkIcon from './link-icon';

const markdownStyles = (theme: Theme) => createStyles({
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
        '& kbd': {
            borderRadius: 3,
            backgroundColor: theme.palette.neutral.light,
            margin: 2,
            fontFamily: theme.typography.fontFamily,
            fontSize: theme.typography.fontSize - 1,
            padding: '2px 7px 2px 7px',
            fontWeight: 500,
            display: 'inline-block'
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
            overflow: 'auto',
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

export class SanitizedMarkdownComponent extends React.Component<SanitizedMarkdown.Props> {

    protected readonly markdownIt: MarkdownIt;

    constructor(props: SanitizedMarkdown.Props) {
        super(props);
        const anchorPlugin: MarkdownIt.PluginWithOptions<MarkdownItAnchor.AnchorOptions> = (MarkdownItAnchor as any).default;
        this.markdownIt = new MarkdownIt({
            html: true,
            linkify: true,
            typographer: true
        });
        if (props.linkify === undefined || props.linkify) {
            this.markdownIt.use(anchorPlugin, {
                permalink: true,
                permalinkSymbol: linkIcon({ x: 0, y: 0, width: 24, height: 10 })
            });
        }
    }

    componentDidMount(): void {
        this.scrollToHeading();
    }

    protected scrollToHeading(): void {
        const anchor = location.hash;
        if (anchor && anchor.length > 1) {
            const heading = document.getElementById(anchor.substring(1));
            if (heading) {
                heading.scrollIntoView();
            }
        }
    }

    render(): React.ReactNode {
        const renderedMd = this.markdownIt.render(this.props.content);
        const sanitized = this.props.sanitize === undefined || this.props.sanitize
            ? DOMPurify.sanitize(renderedMd) : renderedMd;
        return <span
            className={this.props.classes.markdown}
            dangerouslySetInnerHTML={{ __html: sanitized }} />;
    }

}

export namespace SanitizedMarkdown {
    export interface Props extends WithStyles<typeof markdownStyles> {
        content: string;
        sanitize?: boolean;
        linkify?: boolean;
    }
}

export const SanitizedMarkdown = withStyles(markdownStyles)(SanitizedMarkdownComponent);
