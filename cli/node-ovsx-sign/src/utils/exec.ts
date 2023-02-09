/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

import * as cp from "child_process";

export const exec = async (
  command: string,
  options: { cwd?: string; quiet?: boolean; }
): Promise<{ stdout: string; stderr: string }> => {
  if (!options?.quiet) {
    console.log(`Running: ${command}`);
  }
  return new Promise((resolve, reject) => {
    const child = cp.exec(
      command,
      {
        cwd: options?.cwd,
        maxBuffer: 10 * 1024 * 1024, // 10MB
        env: {
          ...process.env,
        },
        shell: "/bin/bash",
      },
      (error, stdout, stderr) => {
        if (error) {
          return reject(error);
        }
        resolve({ stdout, stderr });
      }
    );
    if (!options?.quiet) {
      child.stdout.pipe(process.stdout);
    }
    child.stderr.pipe(process.stderr);
  });
};
