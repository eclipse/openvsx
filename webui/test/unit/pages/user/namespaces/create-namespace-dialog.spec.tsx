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
import { renderWithProviders } from '../../../support/test-providers';
import { testUser } from '../../../support/trusted-publishing';
import { CreateNamespaceDialog } from '../../../../../src/pages/user/namespaces/create-namespace-dialog';
import { ExtensionRegistryService } from '../../../../../src/extension-registry-service';

function openDialog(createNamespace: ReturnType<typeof vi.fn>) {
    const namespaceCreated = vi.fn();
    renderWithProviders(<CreateNamespaceDialog open onClose={vi.fn()} namespaceCreated={namespaceCreated} />, {
        mainContext: { service: { createNamespace } as unknown as ExtensionRegistryService, user: testUser }
    });
    return { namespaceCreated, name: screen.getByLabelText('Namespace Name') };
}

const pressEnter = () => fireEvent.keyDown(document, { code: 'Enter' });

/** The submit runs through a mutation, so the request only goes out once microtasks have run. */
const settle = () =>
    act(async () => {
        await Promise.resolve();
    });

describe('CreateNamespaceDialog', () => {
    it('ignores Enter while the name is one the button would refuse', async () => {
        const createNamespace = vi.fn().mockResolvedValue({ success: 'ok' });
        openDialog(createNamespace);

        pressEnter();
        await settle();

        expect(createNamespace).not.toHaveBeenCalled();
    });

    it('creates the namespace on Enter once there is a name', async () => {
        const createNamespace = vi.fn().mockResolvedValue({ success: 'ok' });
        const { namespaceCreated, name } = openDialog(createNamespace);

        fireEvent.change(name, { target: { value: 'acme' } });
        pressEnter();

        await waitFor(() => expect(namespaceCreated).toHaveBeenCalledWith('acme'));
        expect(createNamespace).toHaveBeenCalledWith('acme');
    });

    it('leaves a request in flight alone rather than sending a second one', async () => {
        const createNamespace = vi.fn().mockReturnValue(new Promise(() => {}));
        const { name } = openDialog(createNamespace);

        // A held key repeats within the tick, before anything the first press set off has rendered.
        fireEvent.change(name, { target: { value: 'acme' } });
        pressEnter();
        pressEnter();
        await settle();

        expect(createNamespace).toHaveBeenCalledOnce();
    });
});
