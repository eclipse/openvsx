/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import org.springframework.stereotype.Service;

import static java.util.Objects.requireNonNull;

/**
 * UUID service based on Tatu Saloranta (FasterXML)
 * <a href="https://github.com/cowtowncoder/java-uuid-generator">java-uuid-generator</a> library.
 * Implementation details:
 * <ul>
 *     <li>Random UUIDs are generated using a time-based epoch generator as UUIDv7.</li>
 *     <li>Name-based UUIDs are generated using a name-based generator with UTF-8 encoding as UUIDv3.</li>
 *     <li>TODO: cluster wide sync using Jedis</li>
 * </ul>
 */
@Service
public class UUIDService {

    /**
     * Generates random UUID.
     */
    public UUID generateRandomUUID() {
        return Generators.timeBasedEpochGenerator().generate();
    }

    /**
     * Generates v3 "name based" UUIDv3.
     */
    public UUID generateFromName(String name) {
        requireNonNull(name);
        return Generators.nameBasedGenerator().generate(name.getBytes(StandardCharsets.UTF_8));
    }
}
