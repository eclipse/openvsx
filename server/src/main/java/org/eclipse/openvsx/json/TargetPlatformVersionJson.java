/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import jakarta.validation.constraints.NotNull;

public class TargetPlatformVersionJson {
    /***
     * Name of the target platform
     */
    public String targetPlatform;

    /***
     * Selected version, or the latest version if none was specified
     */
    @NotNull
    public String version;
}
