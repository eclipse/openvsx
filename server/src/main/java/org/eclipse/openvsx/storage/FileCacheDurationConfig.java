/********************************************************************************
 * Copyright (c) 2022 Marshall Walker and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.openvsx.storage;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileCacheDurationConfig {

    @Value("#{T(java.time.Duration).parse('${ovsx.storage.file-cache-duration:P7D}')}")
    private Duration cacheDuration;

    public Duration getCacheDuration() {
        return cacheDuration;
    }
}
