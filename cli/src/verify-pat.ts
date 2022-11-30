/********************************************************************************
 * Copyright (c) 2022 Anibal Solon and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import { Registry, RegistryOptions } from './registry';
import { readManifest, addEnvOptions } from './util';

/**
 * Validates that a Personal Access Token can publish to a namespace.
 */
export async function verifyPat(options: VerifyPatOptions): Promise<void> {
    addEnvOptions(options);
    if (!options.pat) {
        throw new Error("A personal access token must be given with the option '--pat'.");
    }

    if (!options.namespace) {
      let error;
      try {
        options.namespace = (await readManifest()).publisher;
      } catch (e) {
        error = e;
      }

      if (!options.namespace) {
        throw new Error(
          `Unable to read the namespace's name. Please supply it as an argument or run ovsx from the extension folder.` +
          (error ? `\n\n${error}` : '')
        );
      }
    }

    const registry = new Registry(options);
    const result = await registry.verifyPat(options.namespace, options.pat);
    if (result.error) {
        throw new Error(result.error);
    }
    console.log(`\ud83d\ude80  PAT valid to publish at ${options.namespace}`);
}

export interface VerifyPatOptions extends RegistryOptions {
    /**
     * Name of the namespace.
     */
    namespace?: string
}
