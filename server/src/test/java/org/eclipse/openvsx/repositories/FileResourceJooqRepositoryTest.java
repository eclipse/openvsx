/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.eclipse.openvsx.AbstractPostgresContainerTest;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.util.TargetPlatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.STORAGE_LOCAL;

/**
 * The API tests mock the repository, so these are what catch the {@code size} column dropping out of the
 * projection, which would turn every {@code downloadSizes} value into "unknown".
 */
@SpringBootTest(classes = JooqOnlyTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class FileResourceJooqRepositoryTest extends AbstractPostgresContainerTest {

    @Autowired
    DSLContext dsl;

    @Autowired
    EntityManager em;

    private FileResourceJooqRepository repo;
    private ExtensionVersion extVersion;

    @BeforeEach
    void setUp() {
        repo = new FileResourceJooqRepository(dsl);

        var owner = new UserData();
        owner.setLoginName("file-resource-repo-test-" + UUID.randomUUID());
        em.persist(owner);

        var namespace = new Namespace();
        namespace.setName("ns-" + UUID.randomUUID());
        em.persist(namespace);

        var extension = new Extension();
        extension.setName("ext");
        extension.setNamespace(namespace);
        extension.setActive(true);
        extension.setDownloadable(true);
        extension.setPublishedDate(LocalDateTime.now());
        extension.setLastUpdatedDate(LocalDateTime.now());
        em.persist(extension);

        extVersion = new ExtensionVersion();
        extVersion.setExtension(extension);
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setActive(true);
        extVersion.setPublishedBy(owner);
        em.persist(extVersion);
    }

    @Test
    void findByTypeReturnsTheStoredSize() {
        persistDownload(1_234_567L);

        var found = repo.findByType(List.of(extVersion), List.of(DOWNLOAD));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSize()).isEqualTo(1_234_567L);
    }

    @Test
    void findAllReturnsTheStoredSize() {
        persistDownload(1_234_567L);

        var found = repo.findAll(List.of(extVersion.getId()), List.of(DOWNLOAD));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getSize()).isEqualTo(1_234_567L);
    }

    // A resource stored before the column existed and not yet backfilled has no size, which has to come
    // back as null rather than as zero.
    @Test
    void anUnknownSizeComesBackNull() {
        persistDownload(null);

        assertThat(repo.findByType(List.of(extVersion), List.of(DOWNLOAD)))
                .singleElement()
                .extracting(FileResource::getSize)
                .isNull();
        assertThat(repo.findAll(List.of(extVersion.getId()), List.of(DOWNLOAD)))
                .singleElement()
                .extracting(FileResource::getSize)
                .isNull();
    }

    private void persistDownload(Long size) {
        var download = new FileResource();
        download.setExtension(extVersion);
        download.setType(DOWNLOAD);
        download.setStorageType(STORAGE_LOCAL);
        download.setName("ext-1.0.0.vsix");
        download.setSize(size);
        em.persist(download);

        // The jOOQ queries run over the transaction's raw JDBC connection, so pending inserts have to be
        // flushed before they can see them.
        em.flush();
    }
}
