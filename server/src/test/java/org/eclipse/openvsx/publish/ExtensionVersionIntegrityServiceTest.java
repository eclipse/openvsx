/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software OU and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.publish;

import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.migration.GenerateKeyPairJobService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.eclipse.openvsx.util.TempFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@MockitoBean(types = { CacheService.class, RepositoryService.class, EntityManager.class })
class ExtensionVersionIntegrityServiceTest {

    @Autowired
    ExtensionVersionIntegrityService integrityService;

    @Autowired
    GenerateKeyPairJobService keyPairService;

    @Test
    void testGenerateSignature() throws IOException {
        var keyPair = keyPairService.generateKeyPair();

        var namespace = new Namespace();
        namespace.setName("foo");

        var extension = new Extension();
        extension.setName("bar");
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform("universal");
        extVersion.setExtension(extension);

        var download = new FileResource();
        download.setExtension(extVersion);

        try (
                var stream = getClass().getResource("ms-python.python-2024.7.11511013.vsix").openStream();
                var extensionFile = new TempFile("ms-python", ".vsix");
                var out = Files.newOutputStream(extensionFile.getPath())
        ) {
            stream.transferTo(out);
            extensionFile.setResource(download);
            try(
                    var signatureFile = integrityService.generateSignature(extensionFile, keyPair);
                    var sigzip = new ZipFile(signatureFile.getPath().toFile());
                    var expectedSigZip = new ZipFile(getClass().getResource("ms-python.python-2024.7.11511013.sigzip").getPath())
            ) {
                var iterator = expectedSigZip.stream().iterator();
                while(iterator.hasNext()) {
                    var expectedEntry = iterator.next();
                    var entry = sigzip.getEntry(expectedEntry.getName());
                    assertNotNull(entry);
                    if(expectedEntry.getName().equals(".signature.manifest")) {
                        try (
                                var expectedFile = ArchiveUtil.readEntry(expectedSigZip, expectedEntry);
                                var actualFile = ArchiveUtil.readEntry(sigzip, entry)
                        ) {
                            assertEquals(Files.readString(expectedFile.getPath()),Files.readString(actualFile.getPath()));
                        }
                    }
                }

                var entry = sigzip.getEntry(".signature.sig");
                assertNotNull(entry);
                try(var entryFile = ArchiveUtil.readEntry(sigzip, entry)) {
                    assertTrue(Files.size(entryFile.getPath()) > 0);
                }
            }
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        ExtensionVersionIntegrityService extensionVersionIntegrityService(EntityManager entityManager, CacheService cacheService) {
            return new ExtensionVersionIntegrityService(entityManager, cacheService);
        }

        @Bean
        GenerateKeyPairJobService generateKeyPairJobService(EntityManager entityManager, RepositoryService repositoryService) {
            return new GenerateKeyPairJobService(entityManager, repositoryService);
        }
    }
}
