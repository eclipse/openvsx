/******************************************************************************
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
 *****************************************************************************/

import { useContext } from 'react';
import { useMutation } from '@tanstack/react-query';
import { MainContext } from '../../context';
import { Extension, ErrorResult, isError } from '../../extension-registry-types';

/** `isError` narrows to `ErrorResult`, which does not subtract from a `Readonly<…>` union. */
const isPublished = (result: Readonly<Extension | ErrorResult>): result is Readonly<Extension> => !isError(result);

/**
 * Uploads one `.vsix` package. The registry answers a package it turned down either by
 * rejecting with the parsed body or by returning an `ErrorResult`; both become a rejection
 * here, so the queue has one shape to catch.
 */
export const usePublishExtension = () => {
    const { service } = useContext(MainContext);
    return useMutation<Readonly<Extension>, Readonly<ErrorResult> | Error, File>({
        mutationFn: async (extensionPackage: File) => {
            const result = await service.publishExtension(extensionPackage);
            if (!isPublished(result)) {
                throw result;
            }
            return result;
        }
    });
};
