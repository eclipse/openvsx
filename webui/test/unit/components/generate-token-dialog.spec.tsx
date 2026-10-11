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
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GenerateTokenDialog } from '../../../src/components/generate-token-dialog';

describe('GenerateTokenDialog', () => {
    it('copies the generated token to the clipboard', async () => {
        const user = userEvent.setup();
        render(<GenerateTokenDialog open onClose={vi.fn()} onGenerate={() => Promise.resolve('secret-token')} />);

        await user.click(screen.getByRole('button', { name: 'Generate Token' }));
        await user.click(await screen.findByRole('button', { name: 'Copy' }));

        expect(await navigator.clipboard.readText()).toBe('secret-token');
        expect(await screen.findByRole('button', { name: 'Copied!' })).toBeInTheDocument();
    });
});
