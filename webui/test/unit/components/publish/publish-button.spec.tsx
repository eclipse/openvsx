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

import { describe, expect, it, vi } from 'vitest';
import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useLocation } from 'react-router';
import { renderWithProviders } from '../../support/test-providers';
import { testUser } from '../../support/trusted-publishing';
import { PublishButton } from '../../../../src/components/publish/publish-button';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { Extension } from '../../../../src/extension-registry-types';

const vsix = (name = 'bar.vsix') => new File(['package'], name, { type: 'application/vsix' });

const fileTransfer = (files: File[] = []) => ({ types: ['Files'], files });

/** A window-level drag event carrying files, as the browser delivers it. */
function windowDrag(type: string, types = ['Files']): Event {
    const event = new Event(type, { bubbles: true, cancelable: true });
    Object.defineProperty(event, 'dataTransfer', { value: { types, files: [] } });
    return event;
}

const CurrentPath = () => <span data-testid='path'>{useLocation().pathname}</span>;

function renderButton(loggedIn = true, route = '/') {
    const publishExtension = vi
        .fn()
        .mockResolvedValue({ name: 'bar', namespace: 'foo', displayName: 'Bar', files: {} } as unknown as Extension);
    renderWithProviders(
        <>
            <CurrentPath />
            <PublishButton />
        </>,
        {
            route,
            mainContext: {
                service: {
                    publishExtension,
                    getExtensions: vi.fn().mockResolvedValue([])
                } as unknown as ExtensionRegistryService,
                user: loggedIn ? testUser : undefined
            }
        }
    );
    return { publishExtension };
}

describe('PublishButton', () => {
    it('links to the publish page, and gets there on the shortcut it advertises', async () => {
        renderButton();

        expect(screen.getByText('Publish')).toHaveAttribute('href', '/publish');

        await userEvent.keyboard('p');

        expect(screen.getByTestId('path')).toHaveTextContent('/publish');
    });

    it('offers itself as a drop area as soon as files are dragged into the app', () => {
        renderButton();

        act(() => window.dispatchEvent(windowDrag('dragenter')));

        expect(screen.getByText('Drop to publish')).toBeInTheDocument();
    });

    it('publishes what is dropped on it and follows the packages to the publish page', async () => {
        const { publishExtension } = renderButton(true, '/user-settings/extensions');

        act(() => window.dispatchEvent(windowDrag('dragenter')));
        fireEvent.drop(screen.getByText('Drop to publish'), { dataTransfer: fileTransfer([vsix()]) });

        await waitFor(() => expect(publishExtension).toHaveBeenCalledOnce());
        expect(screen.getByTestId('path')).toHaveTextContent('/publish');
    });

    it('leaves a package dropped anywhere else alone, rather than publishing it', async () => {
        const { publishExtension } = renderButton(true, '/user-settings/extensions');

        act(() => {
            window.dispatchEvent(windowDrag('dragenter'));
            window.dispatchEvent(windowDrag('drop'));
        });

        expect(publishExtension).not.toHaveBeenCalled();
        // The drag is over, so the button goes back to being a button.
        expect(await screen.findByText('Publish')).toBeInTheDocument();
        expect(screen.getByTestId('path')).toHaveTextContent('/user-settings/extensions');
    });

    it('stays out of the way for a signed-out visitor', () => {
        const { publishExtension } = renderButton(false);

        act(() => window.dispatchEvent(windowDrag('dragenter')));
        fireEvent.drop(screen.getByText('Publish'), { dataTransfer: fileTransfer([vsix()]) });

        expect(screen.queryByText('Drop to publish')).not.toBeInTheDocument();
        expect(publishExtension).not.toHaveBeenCalled();
        expect(screen.getByTestId('path')).toHaveTextContent('/');
    });

    it('ignores drags that carry no files', () => {
        renderButton();

        act(() => window.dispatchEvent(windowDrag('dragenter', ['text/plain'])));

        expect(screen.queryByText('Drop to publish')).not.toBeInTheDocument();
    });
});
