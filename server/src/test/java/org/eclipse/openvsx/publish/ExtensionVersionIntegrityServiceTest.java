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

import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.migration.GenerateKeyPairJobService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ArchiveUtil;
import org.eclipse.openvsx.util.TempFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@MockBean({ CacheService.class, RepositoryService.class })
public class ExtensionVersionIntegrityServiceTest {

    @MockBean
    EntityManager entityManager;

    @Autowired
    ExtensionVersionIntegrityService integrityService;

    @Autowired
    GenerateKeyPairJobService keyPairService;

    @Test
    public void testGenerateSignature() throws IOException {
        keyPairService.generateKeyPair();
        var keyPairCaptor = ArgumentCaptor.forClass(SignatureKeyPair.class);
        Mockito.verify(entityManager).persist(keyPairCaptor.capture());
        var keyPair = keyPairCaptor.getValue();

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

        var sigzipContent = new byte[0];
        try (
                var stream = getClass().getResource("ms-python.python-2024.7.11511013.vsix").openStream();
                var extensionFile = new TempFile("ms-python", ".vsix")
        ) {
            Files.write(extensionFile.getPath(), stream.readAllBytes());
            var signature = integrityService.generateSignature(download, extensionFile, keyPair);
            sigzipContent = signature.getContent();
        }

        try(var temp = new TempFile("ms-python", ".sigzip")) {
            Files.write(temp.getPath(), sigzipContent);
            try (
                    var sigzip = new ZipFile(temp.getPath().toFile());
                    var expectedSigZip = new ZipFile(getClass().getResource("ms-python.python-2024.7.11511013.sigzip").getPath())
            ) {
                expectedSigZip.stream()
                        .forEach(expectedEntry -> {
                            var entry = sigzip.getEntry(expectedEntry.getName());
                            assertNotNull(entry);
                            if(expectedEntry.getName().equals(".signature.manifest")) {
                                assertEquals(
                                        new String(ArchiveUtil.readEntry(expectedSigZip, expectedEntry, ObservationRegistry.NOOP)),
                                        new String(ArchiveUtil.readEntry(sigzip, entry, ObservationRegistry.NOOP))
                                );
                            }
                        });

                var entry = sigzip.getEntry(".signature.sig");
                assertNotNull(entry);
                assertTrue(ArchiveUtil.readEntry(sigzip, entry, ObservationRegistry.NOOP).length > 0);
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
