/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

import type { Settings } from '../../extension-registry-types';

/** A runtime setting shown as an on/off switch on the admin dashboard's Settings page. */
export interface AdminSettingsFlag {
    /**
     * The `Settings` property the switch reads and writes. For a contributed switch, a boolean the
     * deployment's server adds to `/admin/settings`.
     */
    key: string;
    title: string;
    description: string;
}

/**
 * A titled group of switches on the Settings page, contributed through
 * `PageSettings.elements.adminSettingsSections`. Additive only: a built-in switch cannot be
 * replaced, so a switch whose key is already in use is ignored.
 */
export interface AdminSettingsSection {
    /**
     * A built-in id - `registry`, `site`, `publishing` or `analytics` - appends the switches to that
     * section. Any other id adds a section after the built-in ones; sections sharing an id are
     * merged into one.
     */
    id: string;
    /** Ignored for a built-in id. A section of your own is titled by its id without one. */
    title?: string;
    description?: string;
    flags: AdminSettingsFlag[];
    /** Outlines the section while it holds unsaved edits, for settings worth a second look. Ignored for a built-in id. */
    warnOnChange?: boolean;
}

/** A section as the page renders it, with the contributed switches folded into the built-in ones. */
export interface ResolvedSettingsSection {
    id: string;
    title: string;
    description: string;
    flags: AdminSettingsFlag[];
    warnOnChange: boolean;
}

const flag = (key: keyof Settings, title: string, description: string): AdminSettingsFlag => ({
    key,
    title,
    description
});

/** The sections the page always shows, in this order. The banner and the size limit are rendered by the page itself. */
export const BUILT_IN_SECTIONS: readonly ResolvedSettingsSection[] = [
    {
        id: 'registry',
        title: 'Registry',
        description: 'How the registry service behaves for every caller.',
        flags: [
            flag(
                'readOnly',
                'Read-only mode',
                'Blocks write operations while keeping browsing, search, and downloads available.'
            )
        ],
        warnOnChange: true
    },
    // No change outline here: unlike read-only mode, a banner edit is not worth a warning.
    {
        id: 'site',
        title: 'Site settings',
        description: 'What visitors see on the web UI.',
        flags: [],
        warnOnChange: false
    },
    {
        id: 'publishing',
        title: 'Publishing',
        description: 'Limits applied when extensions are published.',
        flags: [],
        warnOnChange: false
    },
    {
        id: 'analytics',
        title: 'Analytics',
        description: 'What the registry reports about how extensions are used.',
        flags: [
            flag(
                'downloadAnalyticsEnabled',
                'Download analytics',
                'Shows weekly download graphs on extension pages and serves the download series endpoint. ' +
                    'Switch it on once the download history is backfilled, so visitors do not meet empty graphs.'
            )
        ],
        warnOnChange: false
    }
];

/** The built-in sections with the contributed ones folded in, in the order the page shows them. */
export const withContributedSections = (
    contributed: readonly AdminSettingsSection[] = []
): ResolvedSettingsSection[] => {
    const sections = BUILT_IN_SECTIONS.map(section => ({ ...section, flags: [...section.flags] }));
    const used = new Set(sections.flatMap(section => section.flags.map(({ key }) => key)));
    for (const extra of contributed) {
        const flags = extra.flags.filter(({ key }) => !used.has(key));
        flags.forEach(({ key }) => used.add(key));
        const target = sections.find(section => section.id === extra.id);
        if (target) {
            target.flags.push(...flags);
        } else {
            sections.push({
                id: extra.id,
                title: extra.title ?? extra.id,
                description: extra.description ?? '',
                flags,
                warnOnChange: extra.warnOnChange ?? false
            });
        }
    }
    return sections;
};
