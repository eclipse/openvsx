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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.eclipse.openvsx.entities.FileResource;
import org.junit.jupiter.api.Test;

class ExtensionProcessorTest {

    @Test
    void testTodoTree() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("util/todo-tree.zip");
            var processor = new ExtensionProcessor(stream);
        ) {
            assertThat(processor.getNamespace()).isEqualTo("Gruntfuggly");
            assertThat(processor.getExtensionName()).isEqualTo("todo-tree");

            var metadata = processor.getMetadata();
            assertThat(metadata.getVersion()).isEqualTo("0.0.213");
            assertThat(metadata.getDisplayName()).isEqualTo("Todo Tree");
            assertThat(metadata.getDescription()).isEqualTo("Show TODO, FIXME, etc. comment tags in a tree view");
            assertThat(metadata.getEngines()).isEqualTo(Arrays.asList("vscode@^1.46.0"));
            assertThat(metadata.getCategories()).isEqualTo(Arrays.asList("Other"));
            assertThat(metadata.getTags()).isEqualTo(Arrays.asList("multi-root ready", "task", "tasklist", "todo"));
            assertThat(metadata.getLicense()).isEqualTo("MIT");
            assertThat(metadata.getRepository()).isEqualTo("https://github.com/Gruntfuggly/todo-tree");

            checkResource(processor, FileResource.README, "README.md");
            checkResource(processor, FileResource.ICON, "todo-tree.png");
            checkResource(processor, FileResource.LICENSE, "License.txt");
        }
    }

    @Test
    void testChangelog() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("util/changelog.zip");
            var processor = new ExtensionProcessor(stream);
        ) {
            checkResource(processor, FileResource.CHANGELOG, "CHANGELOG.md");
        }
    }

    @Test
    void testCapitalizedCaseForResources() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("util/with-capitalized-case.zip");
            var processor = new ExtensionProcessor(stream);
        ) {
            checkResource(processor, FileResource.CHANGELOG, "Changelog.md");
            checkResource(processor, FileResource.README, "Readme.md");
            checkResource(processor, FileResource.LICENSE, "License.txt");
        }
    }

    @Test
    void testMinorCaseForResources() throws Exception {
        try (
            var stream = getClass().getResourceAsStream("util/with-minor-case.zip");
            var processor = new ExtensionProcessor(stream);
        ) {
            checkResource(processor, FileResource.CHANGELOG, "changelog.md");
            checkResource(processor, FileResource.README, "readme.md");
            checkResource(processor, FileResource.LICENSE, "license.txt");
        }
    }

    private void checkResource(ExtensionProcessor processor, String type, String expectedName) {
        var metadata = processor.getMetadata();
        var resources = processor.getResources(metadata);
        var fileOfType = resources.stream()
                .filter(res -> type.equals(res.getType()))
                .findAny();
        assertThat(fileOfType).isPresent();
        assertThat(fileOfType.get().getName()).isEqualTo(expectedName);
    }
}