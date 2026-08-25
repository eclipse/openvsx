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
import { act, screen, waitFor } from '@testing-library/react';
import { useLocation } from 'react-router';
import { renderWithProviders } from '../../support/test-providers';
import { testUser } from '../../support/trusted-publishing';
import { GlobalPublishDrop } from '../../../../src/components/publish/global-publish-drop';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { Extension } from '../../../../src/extension-registry-types';

const vsix = (name = 'bar.vsix') => new File(['package'], name, { type: 'application/vsix' });

/** A drag event carrying files, as the browser delivers it. */
function fileDragEvent(type: string, files: File[] = []): Event {
    const event = new Event(type, { bubbles: true, cancelable: true });
    Object.defineProperty(event, 'dataTransfer', { value: { types: ['Files'], files } });
    return event;
}

const CurrentPath = () => <span data-testid='path'>{useLocation().pathname}</span>;

function renderDropTarget(loggedIn = true, route = '/') {
    const publishExtension = vi
        .fn()
        .mockResolvedValue({ name: 'bar', namespace: 'foo', displayName: 'Bar', files: {} } as unknown as Extension);
    renderWithProviders(
        <>
            <CurrentPath />
            <GlobalPublishDrop />
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

describe('GlobalPublishDrop', () => {
    it('invites a drop as soon as files are dragged over the app', () => {
        renderDropTarget();

        act(() => {
            window.dispatchEvent(fileDragEvent('dragenter'));
        });

        expect(screen.getByText('Drop to publish')).toBeInTheDocument();
    });

    it('publishes what is dropped and follows the packages to the publish page', async () => {
        const { publishExtension } = renderDropTarget(true, '/user-settings/extensions');

        act(() => {
            window.dispatchEvent(fileDragEvent('dragenter'));
            window.dispatchEvent(fileDragEvent('drop', [vsix()]));
        });

        await waitFor(() => expect(publishExtension).toHaveBeenCalledOnce());
        expect(screen.getByTestId('path')).toHaveTextContent('/publish');
        expect(screen.queryByText('Drop to publish')).not.toBeInTheDocument();
    });

    it('stays out of the way for a signed-out visitor', () => {
        const { publishExtension } = renderDropTarget(false);

        act(() => {
            window.dispatchEvent(fileDragEvent('dragenter'));
            window.dispatchEvent(fileDragEvent('drop', [vsix()]));
        });

        expect(screen.queryByText('Drop to publish')).not.toBeInTheDocument();
        expect(publishExtension).not.toHaveBeenCalled();
        expect(screen.getByTestId('path')).toHaveTextContent('/');
    });

    it('ignores drags that carry no files', () => {
        renderDropTarget();
        const textDrag = new Event('dragenter', { bubbles: true });
        Object.defineProperty(textDrag, 'dataTransfer', { value: { types: ['text/plain'], files: [] } });

        act(() => {
            window.dispatchEvent(textDrag);
        });

        expect(screen.queryByText('Drop to publish')).not.toBeInTheDocument();
    });
});
