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
import { screen } from '@testing-library/react';
import { TrustedPublishingIcon } from '../../../../src/pages/extension-detail/trusted-publishing-icon';
import { PageSettings } from '../../../../src/page-settings';
import { renderWithProviders } from '../../support/test-providers';

const TITLE = 'Published via trusted publishing';

const pageSettings = (trustedPublishing?: string) => ({ elements: {}, urls: { trustedPublishing } }) as PageSettings;

describe('TrustedPublishingIcon', () => {
    it('renders nothing for a version published with an access token', () => {
        renderWithProviders(<TrustedPublishingIcon color='#fff' />, {
            mainContext: { pageSettings: pageSettings('https://example.com/trusted-publishing') }
        });

        expect(screen.queryByLabelText(TITLE)).not.toBeInTheDocument();
    });

    it('links to the documentation when the instance configures a URL for it', () => {
        renderWithProviders(<TrustedPublishingIcon publishedWithTrustedPublishing color='#fff' />, {
            mainContext: { pageSettings: pageSettings('https://example.com/trusted-publishing') }
        });

        const link = screen.getByLabelText(TITLE);
        expect(link.tagName).toBe('A');
        expect(link).toHaveAttribute('href', 'https://example.com/trusted-publishing');
        expect(link).toHaveAttribute('target', '_blank');
    });

    it('still shows the icon when no documentation URL is configured', () => {
        renderWithProviders(<TrustedPublishingIcon publishedWithTrustedPublishing color='#fff' />, {
            mainContext: { pageSettings: pageSettings() }
        });

        const icon = screen.getByLabelText(TITLE);
        expect(icon).toBeInTheDocument();
        expect(icon.tagName).not.toBe('A');
    });
});
