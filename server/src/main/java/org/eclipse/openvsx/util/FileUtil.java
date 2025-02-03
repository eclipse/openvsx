/** ******************************************************************************
 * Copyright (c) 2025 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

package org.eclipse.openvsx.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FileUtil {

    private static final Map<Path, Object> LOCKS;

    static {
        var MAX_SIZE = 100;
        LOCKS = Collections.synchronizedMap(new LinkedHashMap<>(MAX_SIZE) {
            protected boolean removeEldestEntry(Map.Entry eldest){
                return size() > MAX_SIZE;
            }
        });
    }

    private FileUtil(){}

    /***
     * Write to file synchronously, if it doesn't already exist.
     * @param path File path to write to
     * @param writer Writes to file
     */
    public static void writeSync(Path path, Consumer<Path> writer) {
        synchronized (LOCKS.computeIfAbsent(path, key -> new Object())) {
            if(!Files.exists(path)) {
                writer.accept(path);
            }
        }
    }
}
