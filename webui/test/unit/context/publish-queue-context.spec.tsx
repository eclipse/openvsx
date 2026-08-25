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

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, waitFor } from '@testing-library/react';
import { renderHookWithProviders } from '../support/test-providers';
import { testUser } from '../support/trusted-publishing';
import { usePublishQueue } from '../../../src/context/publish-queue-context';
import { ExtensionRegistryService } from '../../../src/extension-registry-service';
import { Extension } from '../../../src/extension-registry-types';

const vsix = (name = 'bar.vsix') => new File(['package'], name, { type: 'application/vsix' });

const published = (overrides: Partial<Extension> = {}): Extension =>
    ({
        name: 'bar',
        namespace: 'foo',
        displayName: 'Bar',
        version: '1.0.0',
        // The async publish pipeline fills this in; a package with an icon needs no further polling.
        files: { icon: 'https://registry.test/icon.png' },
        ...overrides
    }) as Extension;

function renderQueue(service: Partial<ExtensionRegistryService>, loggedIn = true) {
    const handleError = vi.fn();
    return {
        handleError,
        ...renderHookWithProviders(() => usePublishQueue(), {
            mainContext: {
                service: service as ExtensionRegistryService,
                user: loggedIn ? testUser : undefined,
                handleError
            }
        })
    };
}

describe('publish queue', () => {
    it('uploads every dropped package straight away, with no confirmation step', async () => {
        const publishExtension = vi.fn().mockResolvedValue(published());
        const { result } = renderQueue({ publishExtension });

        act(() => result.current.publish([vsix('one.vsix'), vsix('two.vsix')]));

        expect(publishExtension).toHaveBeenCalledTimes(2);
        await waitFor(() => expect(result.current.items.map(item => item.status)).toEqual(['published', 'published']));
    });

    it('ignores files that are not .vsix packages', () => {
        const publishExtension = vi.fn().mockResolvedValue(published());
        const { result } = renderQueue({ publishExtension });

        act(() => result.current.publish([new File([''], 'notes.txt')]));

        expect(publishExtension).not.toHaveBeenCalled();
        expect(result.current.items).toHaveLength(0);
    });

    it('publishes nothing without a logged-in user', () => {
        const publishExtension = vi.fn().mockResolvedValue(published());
        const { result } = renderQueue({ publishExtension }, false);

        act(() => result.current.publish([vsix()]));

        expect(publishExtension).not.toHaveBeenCalled();
    });

    // A rejected publish reaches us as a thrown error: sendRequest throws the parsed body.
    it('creates the namespace of a first-time publisher and publishes again', async () => {
        const publishExtension = vi
            .fn()
            .mockRejectedValueOnce({
                error: "Unknown publisher: foo\nUse the 'create-namespace' command to create a namespace."
            })
            .mockResolvedValueOnce([published()]);
        const createNamespace = vi.fn().mockResolvedValue({ success: 'ok' });
        const { result } = renderQueue({ publishExtension, createNamespace });

        act(() => result.current.publish([vsix()]));

        await waitFor(() => expect(result.current.items[0].status).toBe('published'));
        expect(createNamespace).toHaveBeenCalledWith(expect.anything(), 'foo');
        expect(publishExtension).toHaveBeenCalledTimes(2);
    });

    it('creates the namespace when the error comes back as a value instead of a throw', async () => {
        const publishExtension = vi
            .fn()
            .mockResolvedValueOnce({ error: 'Unknown publisher: foo\nUse the CLI to create it' })
            .mockResolvedValueOnce([published()]);
        const createNamespace = vi.fn().mockResolvedValue({ success: 'ok' });
        const { result } = renderQueue({ publishExtension, createNamespace });

        act(() => result.current.publish([vsix()]));

        await waitFor(() => expect(result.current.items[0].status).toBe('published'));
        expect(createNamespace).toHaveBeenCalledWith(expect.anything(), 'foo');
    });

    it('reads the namespace even when the message carries no second line', async () => {
        const publishExtension = vi
            .fn()
            .mockRejectedValueOnce({ error: 'Unknown publisher: foo' })
            .mockResolvedValueOnce([published()]);
        const createNamespace = vi.fn().mockResolvedValue({ success: 'ok' });
        const { result } = renderQueue({ publishExtension, createNamespace });

        act(() => result.current.publish([vsix()]));

        await waitFor(() => expect(createNamespace).toHaveBeenCalledWith(expect.anything(), 'foo'));
    });

    it('surfaces the registry error when publishing fails for another reason', async () => {
        const publishExtension = vi.fn().mockRejectedValue({ error: 'Extension too large' });
        const createNamespace = vi.fn();
        const { result, handleError } = renderQueue({ publishExtension, createNamespace });

        act(() => result.current.publish([vsix()]));

        await waitFor(() => expect(result.current.items[0].status).toBe('failed'));
        expect(result.current.items[0].error).toBe('Extension too large');
        expect(createNamespace).not.toHaveBeenCalled();
        // Loud as well as recorded: the app's error dialog reports it like any other failed request.
        expect(handleError).toHaveBeenCalledWith(expect.objectContaining({ error: 'Extension too large' }));
    });

    it('lists the newest upload first, so a fresh one always lands in the same place', () => {
        const publishExtension = vi.fn().mockReturnValue(new Promise(() => {}));
        const { result } = renderQueue({ publishExtension });

        act(() => result.current.publish([vsix('first.vsix')]));
        act(() => result.current.publish([vsix('second.vsix')]));

        expect(result.current.items.map(item => item.fileName)).toEqual(['second.vsix', 'first.vsix']);
    });

    it('drops finished entries on clear, leaving work in flight alone', async () => {
        const publishExtension = vi
            .fn()
            .mockResolvedValueOnce([published()])
            .mockReturnValueOnce(new Promise(() => {}));
        const { result } = renderQueue({ publishExtension });

        act(() => result.current.publish([vsix('done.vsix'), vsix('busy.vsix')]));
        await waitFor(() => expect(result.current.items.some(item => item.status === 'published')).toBe(true));

        act(() => result.current.clearFinished());

        expect(result.current.items.map(item => item.fileName)).toEqual(['busy.vsix']);
    });
});

describe('publish queue — review polling', () => {
    beforeEach(() => vi.useFakeTimers());
    afterEach(() => vi.useRealTimers());

    it('keeps checking an extension left under review until the verdict lands', async () => {
        const publishExtension = vi.fn().mockResolvedValue(published({ reviewStatus: 'under_review' }));
        const getExtensions = vi
            .fn()
            // the first read hydrates the card, the rest are the review poll
            .mockResolvedValueOnce([published({ reviewStatus: 'under_review' })])
            .mockResolvedValueOnce([published({ reviewStatus: 'under_review' })])
            .mockResolvedValue([published({ reviewStatus: 'published' })]);
        const { result } = renderQueue({ publishExtension, getExtensions });

        await act(async () => {
            result.current.publish([vsix()]);
        });
        expect(result.current.items[0].status).toBe('reviewing');

        await act(async () => {
            await vi.advanceTimersByTimeAsync(5000);
        });
        expect(getExtensions).toHaveBeenCalledTimes(2);
        expect(result.current.items[0].status).toBe('reviewing');

        await act(async () => {
            await vi.advanceTimersByTimeAsync(5000);
        });
        expect(result.current.items[0].status).toBe('published');
    });

    it('keeps re-reading until the async pipeline has stored the icon', async () => {
        const withoutIcon = published({ files: {} as Extension['files'] });
        const publishExtension = vi.fn().mockResolvedValue(withoutIcon);
        const getExtensions = vi
            .fn()
            .mockResolvedValueOnce([withoutIcon])
            .mockResolvedValueOnce([withoutIcon])
            .mockResolvedValue([published()]);
        const { result } = renderQueue({ publishExtension, getExtensions });

        await act(async () => {
            result.current.publish([vsix()]);
        });
        expect(result.current.items[0].extension?.files.icon).toBeUndefined();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(5000);
        });
        expect(result.current.items[0].extension?.files.icon).toBeUndefined();

        await act(async () => {
            await vi.advanceTimersByTimeAsync(5000);
        });
        expect(result.current.items[0].extension?.files.icon).toBe('https://registry.test/icon.png');
    });

    it('gives up on a missing icon rather than polling for ever', async () => {
        const withoutIcon = published({ files: {} as Extension['files'] });
        const publishExtension = vi.fn().mockResolvedValue(withoutIcon);
        const getExtensions = vi.fn().mockResolvedValue([withoutIcon]);
        const { result } = renderQueue({ publishExtension, getExtensions });

        await act(async () => {
            result.current.publish([vsix()]);
        });
        await act(async () => {
            await vi.advanceTimersByTimeAsync(90_000);
        });

        expect(result.current.items[0].status).toBe('published');
        // 60s of 5s polls, and then it stops.
        expect(getExtensions.mock.calls.length).toBeLessThanOrEqual(14);
    });

    it('holds a package whose namespace is not verified, instead of waiting on a review', async () => {
        const conflicted = published({ reviewStatus: 'under_review', namespaceOwnershipConflict: true });
        const publishExtension = vi.fn().mockResolvedValue(conflicted);
        const getExtensions = vi.fn().mockResolvedValue([conflicted]);
        const { result } = renderQueue({ publishExtension, getExtensions });

        await act(async () => {
            result.current.publish([vsix()]);
        });
        expect(result.current.items[0].status).toBe('blocked');

        await act(async () => {
            await vi.advanceTimersByTimeAsync(60_000);
        });

        expect(result.current.items[0].status).toBe('blocked');
        // Only the read that hydrated the card: claiming the namespace is the user's move, and no
        // amount of polling will see it happen.
        expect(getExtensions).toHaveBeenCalledTimes(1);
    });

    it('reports a package the review rejects, with the reason', async () => {
        const publishExtension = vi.fn().mockResolvedValue(published({ reviewStatus: 'under_review' }));
        const getExtensions = vi
            .fn()
            .mockResolvedValueOnce([published({ reviewStatus: 'under_review' })])
            .mockResolvedValue([published({ reviewStatus: 'rejected', reviewMessage: 'Malicious code found' })]);
        const { result } = renderQueue({ publishExtension, getExtensions });

        await act(async () => {
            result.current.publish([vsix()]);
        });
        await act(async () => {
            await vi.advanceTimersByTimeAsync(5000);
        });

        expect(result.current.items[0].status).toBe('rejected');
        expect(result.current.items[0].error).toBe('Malicious code found');
    });
});
