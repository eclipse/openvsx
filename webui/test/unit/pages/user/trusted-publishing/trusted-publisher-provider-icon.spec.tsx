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

import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TrustedPublisherProviderIcon } from '../../../../../src/pages/user/trusted-publishing/trusted-publisher-provider-icon';

describe('TrustedPublisherProviderIcon', () => {
    it('shows the GitHub icon for the github provider', () => {
        render(<TrustedPublisherProviderIcon providerId='github' />);
        expect(screen.getByTestId('GitHubIcon')).toBeInTheDocument();
    });

    it('shares the GitLab icon across the GitLab provider family', () => {
        render(<TrustedPublisherProviderIcon providerId='eclipse-gitlab' data-testid='provider-icon' />);
        expect(screen.getByTestId('provider-icon')).toBeInTheDocument();
        expect(screen.queryByTestId('GitHubIcon')).not.toBeInTheDocument();
        expect(screen.queryByTestId('RocketLaunchIcon')).not.toBeInTheDocument();
    });

    it('falls back to a generic icon for unknown providers', () => {
        render(<TrustedPublisherProviderIcon providerId='bitbucket' />);
        expect(screen.getByTestId('RocketLaunchIcon')).toBeInTheDocument();
    });
});
