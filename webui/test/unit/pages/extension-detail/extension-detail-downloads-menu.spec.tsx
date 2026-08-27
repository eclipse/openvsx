/********************************************************************************
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
 ********************************************************************************/

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ExtensionDetailDownloadsMenu } from '../../../../src/pages/extension-detail/extension-detail-downloads-menu';

describe('ExtensionDetailDownloadsMenu', () => {
    // Regression for #2100: each download option used to be an inline <a> nested inside a
    // non-interactive MenuItem row, so the anchor only spanned the width of its own text -
    // clicking anywhere else in the row (which is what the menu visually offers) did nothing.
    // The menu item itself must now be the anchor, so its accessible role/href cover the whole row.
    it('renders each download option itself as the link, not a link nested inside it', async () => {
        const user = userEvent.setup();
        render(
            <ExtensionDetailDownloadsMenu
                downloads={{
                    universal: 'https://example.com/universal.vsix',
                    web: 'https://example.com/web.vsix'
                }}
            />
        );

        await user.click(screen.getByRole('button', { name: 'Download' }));

        const universalItem = screen.getByRole('menuitem', { name: 'Universal' });
        expect(universalItem.tagName).toBe('A');
        expect(universalItem).toHaveAttribute('href', 'https://example.com/universal.vsix');

        const webItem = screen.getByRole('menuitem', { name: 'Web' });
        expect(webItem.tagName).toBe('A');
        expect(webItem).toHaveAttribute('href', 'https://example.com/web.vsix');
    });

    it('closes the menu once a download option is clicked', async () => {
        const user = userEvent.setup();
        render(<ExtensionDetailDownloadsMenu downloads={{ universal: 'https://example.com/universal.vsix' }} />);

        await user.click(screen.getByRole('button', { name: 'Download' }));
        expect(screen.getByRole('menu')).toBeInTheDocument();

        await user.click(screen.getByRole('menuitem', { name: 'Universal' }));

        const { waitFor } = await import('@testing-library/react');
        await waitFor(() => expect(screen.queryByRole('menu')).not.toBeInTheDocument());
    });
});
