/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import org.springframework.stereotype.Component;

@Component
public class SearchEntryJsonCacheKeyGenerator {

    public Object generate(long extensionId, boolean includeAllVersions) {
        return "extensionId=" + extensionId + ",includeAllVersions=" + includeAllVersions;
    }
}