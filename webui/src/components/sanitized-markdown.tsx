/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import React, { FunctionComponent, useEffect } from 'react';
import * as MarkdownIt from 'markdown-it';
import * as MarkdownItAnchor from 'markdown-it-anchor';
import * as DOMPurify from 'dompurify';
import { Theme, styled } from '@mui/material/styles';
import linkIcon from './link-icon';
import { useLocation } from 'react-router-dom';

const Markdown = styled('div')(({ theme }: { theme: Theme }) => ({
    '& a': {
        textDecoration: 'none',
        color: theme.palette.secondary.main,
        '&:hover': {
            textDecoration: 'underline'
        }
    },
    '& a.header-anchor': {
        fill: theme.palette.textHint,
        opacity: 0.4,
        marginLeft: theme.spacing(0.5),
        '&:hover': {
            opacity: 1
        }
    },
    '& kbd': {
        borderRadius: theme.spacing(3),
        backgroundColor: theme.palette.neutral.light,
        margin: theme.spacing(2),
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
}));

export const SanitizedMarkdown: FunctionComponent<SanitizedMarkdownProps> = ({ content, sanitize, linkify }) => {

    const markdownIt = new MarkdownIt({
        html: true,
        linkify: true,
        typographer: true
    });
    if (linkify === undefined || linkify) {
        const anchor = MarkdownItAnchor.default;
        markdownIt.use(anchor, {
            permalink: anchor.permalink.linkInsideHeader({
                symbol: linkIcon({ x: 0, y: 0, width: 24, height: 10 })
            })
        });
    }

    const { hash } = useLocation();

    useEffect(() => {
        if (hash && hash.length > 1) {
            const heading = document.getElementById(hash.substring(1));
            if (heading) {
                heading.scrollIntoView();
            }
        }
    }, []);

    const renderedMd = markdownIt.render(content);
    const sanitized = sanitize === undefined || sanitize
        ? DOMPurify.sanitize(renderedMd) : renderedMd;

    return <Markdown dangerouslySetInnerHTML={{ __html: sanitized }} />;
};

export interface SanitizedMarkdownProps {
    content: string;
    sanitize?: boolean;
    linkify?: boolean;
}