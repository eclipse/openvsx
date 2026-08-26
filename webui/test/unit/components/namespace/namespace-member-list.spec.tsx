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
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../support/test-providers';
import { testNamespace } from '../../support/user-settings';
import { NamespaceMemberList } from '../../../../src/components/namespace/namespace-member-list';
import {
    NamespaceDetailConfig,
    NamespaceDetailConfigContext
} from '../../../../src/components/namespace/namespace-detail-config';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';
import { MembershipRole, NamespaceMembership, UserData } from '../../../../src/extension-registry-types';

const viewer = { loginName: 'gnugomez', provider: 'github', fullName: 'Jordi' } as UserData;
const other = { loginName: 'someone', provider: 'github', fullName: 'Someone Else' } as UserData;

const membership = (user: UserData, role: MembershipRole = 'contributor'): NamespaceMembership =>
    ({ user, role, namespace: 'foo' }) as NamespaceMembership;

function renderMembers(members: NamespaceMembership[], config: NamespaceDetailConfig = {}) {
    const getNamespaceMembers = vi.fn().mockResolvedValue({ namespaceMemberships: members });
    const setNamespaceMember = vi.fn().mockResolvedValue({ success: 'ok' });
    const service = { getNamespaceMembers, setNamespaceMember } as unknown as ExtensionRegistryService;
    renderWithProviders(
        <NamespaceDetailConfigContext.Provider value={config}>
            <NamespaceMemberList namespace={testNamespace({ roleUrl: '/role' })} setLoadingState={vi.fn()} />
        </NamespaceDetailConfigContext.Provider>,
        { mainContext: { service, user: viewer } }
    );
    return { getNamespaceMembers, setNamespaceMember };
}

describe('NamespaceMemberList', () => {
    it('lists the namespace members', async () => {
        renderMembers([membership(other)]);

        expect(await screen.findByText('Someone Else')).toBeInTheDocument();
        expect(screen.getByText('someone')).toBeInTheDocument();
    });

    it('says so when the namespace has no members yet', async () => {
        const { getNamespaceMembers } = renderMembers([]);

        await waitFor(() => expect(getNamespaceMembers).toHaveBeenCalled());
        expect(await screen.findByText('There are no members assigned yet.')).toBeInTheDocument();
    });

    it('pins the viewer to Owner when the surface fixes self, hiding their controls', async () => {
        renderMembers([membership(viewer, 'owner'), membership(other)], { fixSelf: true });

        expect(await screen.findByText('Owner')).toBeInTheDocument();
        // Only the other member keeps a remove action.
        expect(screen.queryByLabelText('Remove gnugomez')).not.toBeInTheDocument();
        expect(screen.getByLabelText('Remove someone')).toBeInTheDocument();
    });

    it('leaves the viewer manageable when the surface does not fix self', async () => {
        renderMembers([membership(viewer, 'owner')]);

        expect(await screen.findByLabelText('Remove gnugomez')).toBeInTheDocument();
    });

    it('removes a member through the role endpoint and reloads the list', async () => {
        const { getNamespaceMembers, setNamespaceMember } = renderMembers([membership(other)]);

        await userEvent.click(await screen.findByLabelText('Remove someone'));

        await waitFor(() =>
            expect(setNamespaceMember).toHaveBeenCalledWith(expect.anything(), '/role', other, 'remove')
        );
        await waitFor(() => expect(getNamespaceMembers).toHaveBeenCalledTimes(2));
    });
});
