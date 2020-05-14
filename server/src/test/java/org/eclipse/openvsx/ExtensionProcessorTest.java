/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

public class ExtensionProcessorTest {

    @Test
    public void testTodoTree() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("util/todo-tree.zip");
            var processor = new ExtensionProcessor(stream);
        ) {
            assertEquals("Gruntfuggly", processor.getNamespace());
            assertEquals("todo-tree", processor.getExtensionName());

            var metadata = processor.getMetadata();
            assertEquals("0.0.160", metadata.getVersion());
            assertEquals("Todo Tree", metadata.getDisplayName());
            assertEquals("Show TODO, FIXME, etc. comment tags in a tree view", metadata.getDescription());
            assertEquals(Arrays.asList("vscode@^1.5.0"), metadata.getEngines());
            assertEquals(Arrays.asList("Other"), metadata.getCategories());
            assertEquals(Arrays.asList("todo", "task", "tasklist", "multi-root ready"), metadata.getTags());
            assertEquals("MIT", metadata.getLicense());
            assertEquals("https://github.com/Gruntfuggly/todo-tree", metadata.getRepository());
            processor.getResources(metadata);
            assertEquals("README.md", metadata.getReadmeFileName());
            assertEquals("todo-tree.png", metadata.getIconFileName());
            assertEquals("LICENSE.txt", metadata.getLicenseFileName());
        }
    }

}