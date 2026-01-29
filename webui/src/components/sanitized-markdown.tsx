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
import MarkdownIt from 'markdown-it';
import * as MarkdownItAnchor from 'markdown-it-anchor';
import { alert } from '@mdit/plugin-alert';
import DOMPurify from 'dompurify';
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
    },
    '& .markdown-alert': {
        padding: '8px 16px',
        marginBottom: 16,
        borderLeft: '4px solid',
        borderRadius: 4,
        '& .markdown-alert-title': {
            display: 'flex',
            alignItems: 'center',
            fontWeight: 600,
            marginBottom: 4,
            '& svg': {
                marginRight: 8,
                width: 16,
                height: 16
            }
        },
        '& .markdown-alert-title:before': {
            content: '" "',
            width: '16px',
            height: '16px',
            marginRight: '8px',
        },
        '& p': {
            margin: 0
        }
    },
    '& .markdown-alert-note': {
        borderColor: '#2f81f7',
        backgroundColor: 'rgba(47, 129, 247, 0.1)',
        '& .markdown-alert-title': {
            color: '#2f81f7'
        },
        '& .markdown-alert-title::before': {
            content: `url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='%232f81f7' d='M0 8a8 8 0 1 1 16 0A8 8 0 0 1 0 8Zm8-6.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13ZM6.5 7.75A.75.75 0 0 1 7.25 7h1a.75.75 0 0 1 .75.75v2.75h.25a.75.75 0 0 1 0 1.5h-2a.75.75 0 0 1 0-1.5h.25v-2h-.25a.75.75 0 0 1-.75-.75ZM8 6a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z'/%3E%3C/svg%3E")`
        }
    },
    '& .markdown-alert-tip': {
        borderColor: '#3fb950',
        backgroundColor: 'rgba(63, 185, 80, 0.1)',
        '& .markdown-alert-title': {
            color: '#3fb950'
        },
        '& .markdown-alert-title::before': {
            content: `url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='%233fb950' d='M8 1.5c-2.363 0-4 1.69-4 3.75 0 .984.424 1.625.984 2.304l.214.253c.223.264.47.556.673.848.284.411.537.896.621 1.49a.75.75 0 0 1-1.484.211c-.04-.282-.163-.547-.37-.847a8.456 8.456 0 0 0-.542-.68c-.084-.1-.173-.205-.268-.32C3.201 7.75 2.5 6.766 2.5 5.25 2.5 2.31 4.863 0 8 0s5.5 2.31 5.5 5.25c0 1.516-.701 2.5-1.328 3.259-.095.115-.184.22-.268.319-.207.245-.383.453-.541.681-.208.3-.33.565-.37.847a.751.751 0 0 1-1.485-.212c.084-.593.337-1.078.621-1.489.203-.292.45-.584.673-.848.075-.088.147-.173.213-.253.561-.679.985-1.32.985-2.304 0-2.06-1.637-3.75-4-3.75ZM5.75 12h4.5a.75.75 0 0 1 0 1.5h-4.5a.75.75 0 0 1 0-1.5ZM6 15.25a.75.75 0 0 1 .75-.75h2.5a.75.75 0 0 1 0 1.5h-2.5a.75.75 0 0 1-.75-.75Z'/%3E%3C/svg%3E")`
        }
    },
    '& .markdown-alert-important': {
        borderColor: '#a371f7',
        backgroundColor: 'rgba(163, 113, 247, 0.1)',
        '& .markdown-alert-title': {
            color: '#a371f7'
        },
        '& .markdown-alert-title::before': {
            content: `url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='%23a371f7' d='M0 1.75C0 .784.784 0 1.75 0h12.5C15.216 0 16 .784 16 1.75v9.5A1.75 1.75 0 0 1 14.25 13H8.06l-2.573 2.573A1.458 1.458 0 0 1 3 14.543V13H1.75A1.75 1.75 0 0 1 0 11.25Zm1.75-.25a.25.25 0 0 0-.25.25v9.5c0 .138.112.25.25.25h2a.75.75 0 0 1 .75.75v2.19l2.72-2.72a.749.749 0 0 1 .53-.22h6.5a.25.25 0 0 0 .25-.25v-9.5a.25.25 0 0 0-.25-.25Zm7 2.25v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 9a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z'/%3E%3C/svg%3E ")`
        }
    },
    '& .markdown-alert-warning': {
        borderColor: '#d29922',
        backgroundColor: 'rgba(210, 153, 34, 0.1)',
        '& .markdown-alert-title': {
            color: '#d29922'
        },
        '& .markdown-alert-title::before': {
            content: `url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='%23d29922' d='M6.457 1.047c.659-1.234 2.427-1.234 3.086 0l6.082 11.378A1.75 1.75 0 0 1 14.082 15H1.918a1.75 1.75 0 0 1-1.543-2.575Zm1.763.707a.25.25 0 0 0-.44 0L1.698 13.132a.25.25 0 0 0 .22.368h12.164a.25.25 0 0 0 .22-.368Zm.53 3.996v2.5a.75.75 0 0 1-1.5 0v-2.5a.75.75 0 0 1 1.5 0ZM9 11a1 1 0 1 1-2 0 1 1 0 0 1 2 0Z'/%3E%3C/svg%3E")`
        }
    },
    '& .markdown-alert-caution': {
        borderColor: '#f85149',
        backgroundColor: 'rgba(248, 81, 73, 0.1)',
        '& .markdown-alert-title': {
            color: '#f85149'
        },
        '& .markdown-alert-title::before': {
            content: `url("data:image/svg+xml;utf8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3E%3Cpath fill='%23f85149' d='M4.47.22A.749.749 0 0 1 5 0h6c.199 0 .389.079.53.22l4.25 4.25c.141.14.22.331.22.53v6a.749.749 0 0 1-.22.53l-4.25 4.25A.749.749 0 0 1 11 16H5a.749.749 0 0 1-.53-.22L.22 11.53A.749.749 0 0 1 0 11V5c0-.199.079-.389.22-.53Zm.84 1.28L1.5 5.31v5.38l3.81 3.81h5.38l3.81-3.81V5.31L10.69 1.5ZM8 4a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 8 4Zm0 8a1 1 0 1 1 0-2 1 1 0 0 1 0 2Z'/%3E%3C/svg%3E")`
        }
    }
}));

export const SanitizedMarkdown: FunctionComponent<SanitizedMarkdownProps> = ({ content, sanitize, linkify }) => {

    const markdownIt = new MarkdownIt({
        html: true,
        linkify: true,
        typographer: true
    });

    markdownIt.use(alert);

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