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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.openvsx.entities.FileResource;
import org.junit.jupiter.api.Test;

class ExtensionProcessorTest {

    @Test
    void testTodoTree() throws Exception {
        var path = writeToTempFile("util/todo-tree.zip");
        try (var processor = new ExtensionProcessor(path)) {
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
        } finally {
            Files.delete(path);
        }
    }

    @Test
    void testChangelog() throws Exception {
        var path = writeToTempFile("util/changelog.zip");
        try (var processor = new ExtensionProcessor(path)) {
            checkResource(processor, FileResource.CHANGELOG, "CHANGELOG.md");
        } finally {
            Files.delete(path);
        }
    }

    @Test
    void testCapitalizedCaseForResources() throws Exception {
        var path = writeToTempFile("util/with-capitalized-case.zip");
        try (var processor = new ExtensionProcessor(path)) {
            checkResource(processor, FileResource.CHANGELOG, "Changelog.md");
            checkResource(processor, FileResource.README, "Readme.md");
            checkResource(processor, FileResource.LICENSE, "License.txt");
        } finally {
            Files.delete(path);
        }
    }

    @Test
    void testMinorCaseForResources() throws Exception {
        var path = writeToTempFile("util/with-minor-case.zip");
        try (var processor = new ExtensionProcessor(path)) {
            checkResource(processor, FileResource.CHANGELOG, "changelog.md");
            checkResource(processor, FileResource.README, "readme.md");
            checkResource(processor, FileResource.LICENSE, "license.txt");
        } finally {
            Files.delete(path);
        }
    }

    private Path writeToTempFile(String resource) throws IOException {
        var path = Files.createTempFile("test", ".zip");
        try(
                var in = getClass().getResourceAsStream(resource);
                var out = Files.newOutputStream(path);
        ) {
            in.transferTo(out);
        }

        return path;
    }

    private void checkResource(ExtensionProcessor processor, String type, String expectedName) {
        var metadata = processor.getMetadata();
        var resources = processor.getFileResources(metadata);
        var fileOfType = resources.stream()
                .filter(res -> type.equals(res.getType()))
                .findAny();
        assertThat(fileOfType).isPresent();
        assertThat(fileOfType.get().getName()).isEqualTo(expectedName);
    }
}