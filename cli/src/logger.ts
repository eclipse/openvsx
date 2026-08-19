/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

/**
 * Sink for the progress messages this library writes.
 *
 * Pass an implementation via {@link RegistryOptions.log} to capture or silence the output when using
 * `ovsx` programmatically; the command line interface uses {@link consoleLogger}.
 */
export interface Logger {
    /**
     * Reports progress. Called without a message to separate blocks of output.
     */
    log(message?: string): void;
    /**
     * Reports a condition that does not stop the operation.
     */
    warn(message: string): void;
}

/**
 * The default logger, writing to the console.
 */
export const consoleLogger: Logger = {
    log: (message = '') => console.log(message),
    warn: message => console.warn(message)
};

/**
 * A logger that discards everything, for callers that only care about the result.
 */
export const silentLogger: Logger = {
    log: () => { },
    warn: () => { }
};
