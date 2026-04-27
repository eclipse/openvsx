/*
 * Copyright (c) 2026 Eclipse Foundation AISBL
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.openvsx.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A wrapper around {@link ZipFile} that normalizes backslashes to forward slashes in entry names,
 * matching the behaviour of yauzl (the ZIP parser used by VS Code).
 *
 * Entries are indexed by their normalized name at construction time. The original {@link ZipEntry}
 */
public class NormalizedZipFile implements AutoCloseable {

    private final ZipFile zipFile;
    /**
     * Maps each normalized entry name to its original {@link ZipEntry}, in archive order.
     * When two entries normalize to the same name the last one wins; {@link #hasDuplicateNormalizedEntries()}
     * signals that this occurred.
     */
    private final LinkedHashMap<String, ZipEntry> entriesByNormalizedName;
    private final boolean hasDuplicates;

    public NormalizedZipFile(File file) throws IOException {
        this.zipFile = new ZipFile(file);

        var map = new LinkedHashMap<String, ZipEntry>();
        boolean duplicates = false;

        var iter = zipFile.entries();
        while (iter.hasMoreElements()) {
            ZipEntry entry = iter.nextElement();
            if (map.put(normalize(entry.getName()), entry) != null) {
                duplicates = true;
            }
        }

        this.entriesByNormalizedName = map;
        this.hasDuplicates = duplicates;
    }

    public NormalizedZipFile(String path) throws IOException {
        this(new File(path));
    }

    /** Returns the entry whose normalized name equals {@code name}, or {@code null} if absent. */
    public ZipEntry getEntry(String name) {
        return entriesByNormalizedName.get(normalize(name));
    }

    /** Returns the entry whose normalized name matches {@code name} case-insensitively, or {@code null} if absent. */
    public ZipEntry getEntryIgnoreCase(String name) {
        String target = normalize(name);
        for (Map.Entry<String, ZipEntry> e : entriesByNormalizedName.entrySet()) {
            if (e.getKey().equalsIgnoreCase(target)) {
                return e.getValue();
            }
        }
        return null;
    }

    public Collection<ZipEntry> entries() {
        return entriesByNormalizedName.values();
    }

    public Stream<ZipEntry> stream() {
        return entriesByNormalizedName.values().stream();
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return zipFile.getInputStream(entry);
    }

    /**
     * Returns {@code true} if two or more entries share the same normalized name — a sign of a
     * potentially malicious ZIP.
     */
    public boolean hasDuplicateNormalizedEntries() {
        return hasDuplicates;
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }

    private static String normalize(String name) {
        return name.replace('\\', '/');
    }
}
