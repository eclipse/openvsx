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
import { act, fireEvent, screen } from '@testing-library/react';
import { renderWithProviders } from '../../support/test-providers';
import { testUser } from '../../support/trusted-publishing';
import { usePublishDrop } from '../../../../src/components/publish/use-publish-drop';
import { ExtensionRegistryService } from '../../../../src/extension-registry-service';

/** A window-level drag event carrying files, as the browser delivers it. */
function windowDrag(type: string): Event {
    const event = new Event(type, { bubbles: true, cancelable: true });
    Object.defineProperty(event, 'dataTransfer', { value: { types: ['Files'], files: [] } });
    return event;
}

const DropTarget = () => {
    const { dragging, over, dropProps } = usePublishDrop();
    return (
        <div data-testid='target' data-dragging={dragging} data-over={over} {...dropProps}>
            target
        </div>
    );
};

function mountDropTarget() {
    renderWithProviders(<DropTarget />, {
        mainContext: {
            service: { publishExtension: vi.fn() } as unknown as ExtensionRegistryService,
            user: testUser
        }
    });
    return screen.getByTestId('target');
}

describe('usePublishDrop', () => {
    it('drops the over state when the drag leaves the window, not just the target', () => {
        const target = mountDropTarget();

        act(() => window.dispatchEvent(windowDrag('dragenter')));
        fireEvent.dragOver(target, { dataTransfer: { types: ['Files'], files: [] } });
        expect(target).toHaveAttribute('data-over', 'true');

        // Dragging back out of the browser window: the target's own dragleave never fires.
        act(() => window.dispatchEvent(windowDrag('dragleave')));

        expect(target).toHaveAttribute('data-dragging', 'false');
        expect(target).toHaveAttribute('data-over', 'false');
    });
});
