/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

export interface VerifySignatureOptions {
    /**
     * Path to the .vsix package.
     */
    packagePath: string;
    /**
     * Path to the signature manifest file (the `.signature.manifest` entry of a registry's
     * signature archive).
     */
    manifestPath: string;
    /**
     * Path to the signature file (the `.signature.sig` entry of a registry's signature archive).
     */
    signaturePath: string;
    /**
     * Path to the registry's public key file, in PEM format.
     */
    publicKeyPath: string;
}
