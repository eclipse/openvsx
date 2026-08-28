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

/**
 * Uploads one `.vsix` package. A package the registry turns down rejects with its reason,
 * whether the server said so with a status or with an error body.
 */
export const usePublishExtension = () => {
    const { service } = useContext(MainContext);
    return useMutation({ mutationFn: (extensionPackage: File) => service.publishExtension(extensionPackage) });
};
