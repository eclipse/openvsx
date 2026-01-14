/********************************************************************************
 * Copyright (c) 2025 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.json.QueryResultJson;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.json.QueryRequest;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LocalRegistryService.query() method
 * Tests null preview value handling:
 * var preview = previewsByExtensionId.getOrDefault(ev.getExtension().getId(), false);
 * 
 * When the map contains null as the value, getOrDefault returns null (not the default false)
 */
public class QueryMethodNullPreviewTest {

    private LocalRegistryService localRegistryService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private RepositoryService repositories;

    @Mock
    private ExtensionService extensions;

    @Mock
    private VersionService versions;

    @Mock
    private UserService users;

    @Mock
    private SearchUtilService search;

    @Mock
    private ExtensionValidator validator;

    @Mock
    private StorageUtilService storageUtil;

    @Mock
    private EclipseService eclipse;

    @Mock
    private CacheService cache;

    @Mock
    private ExtensionVersionIntegrityService integrityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        localRegistryService = new LocalRegistryService(
                entityManager,
                repositories,
                extensions,
                versions,
                users,
                search,
                validator,
                storageUtil,
                eclipse,
                cache,
                integrityService
        );
    }

    /**
     * Test query method when preview value in map is explicitly null
     * This tests the critical case where:
     * var preview = previewsByExtensionId.getOrDefault(ev.getExtension().getId(), false);
     * 
     * Returns null instead of false, and verifies the code handles it gracefully
     */
    @Test
    void testQueryMethodWithNullPreviewValue() {
        // Setup test data
        var namespace = createNamespace(1L, "test-namespace");
        var extension = createExtension(2L, "test-extension", namespace);
        var extVersion = createExtensionVersion(3L, "1.0.0", extension);

        // Create query request
        var queryRequest = new QueryRequest(
                "test-namespace",
                "test-extension",
                null,
                null,
                null,
                null,
                false,
                TargetPlatform.NAME_UNIVERSAL,
                100,
                0
        );

        // Mock repository methods
        when(repositories.findActiveVersions(any(QueryRequest.class)))
                .thenReturn(new PageImpl<>(List.of(extVersion), Pageable.ofSize(100), 1));

        // Mock preview map with NULL value for the extension
        // This is the key test case - when getOrDefault returns null
        Map<Long, Boolean> previewMapWithNull = new HashMap<>();
        previewMapWithNull.put(extension.getId(), null);
        when(repositories.findLatestVersionsIsPreview(Set.of(extension.getId())))
                .thenReturn(previewMapWithNull);

        // Mock other required methods
        when(repositories.findLatestVersionForAllUrls(eq(extension), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(extVersion);

        when(repositories.findVersionStringsSorted(any(Extension.class), anyString(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        when(repositories.findFileResourcesByExtensionVersionIdAndType(any(), any()))
                .thenReturn(Collections.emptyList());

        when(repositories.findNamespaceMemberships(any()))
                .thenReturn(Collections.emptyList());

        when(versions.getLatest(any(List.class), anyBoolean(), anyBoolean()))
                .thenReturn(extVersion);

        // Execute the query - this should not throw an exception
        QueryResultJson result = localRegistryService.query(queryRequest);

        // Verify results
        assertNotNull(result, "Query result should not be null even with null preview value");
        assertEquals(1, result.getExtensions().size(), "Should have one extension");
        assertNotNull(result.getExtensions().get(0), "Extension JSON should not be null");
        
        // Verify the extension has the correct data
        var returnedExtension = result.getExtensions().get(0);
        assertEquals("test-extension", returnedExtension.getName(), "Extension name should be correct");
        assertEquals("test-namespace", returnedExtension.getNamespace(), "Extension namespace should be correct");
    }

    // Helper methods to create test entities
    private Namespace createNamespace(long id, String name) {
        var namespace = new Namespace();
        namespace.setId(id);
        namespace.setName(name);
        namespace.setPublicId(UUID.randomUUID().toString());
        return namespace;
    }

    private Extension createExtension(long id, String name, Namespace namespace) {
        var extension = new Extension();
        extension.setId(id);
        extension.setName(name);
        extension.setPublicId(UUID.randomUUID().toString());
        extension.setNamespace(namespace);
        extension.setActive(true);
        return extension;
    }

    private ExtensionVersion createExtensionVersion(long id, String version, Extension extension) {
        var extVersion = new ExtensionVersion();
        extVersion.setId(id);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);
        extVersion.setTimestamp(LocalDateTime.now());
        extVersion.setPreview(false);
        return extVersion;
    }
}
