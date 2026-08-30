/********************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.openvsx.accesstoken.AccessTokenService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionReferenceJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.publish.ExtensionVersionIntegrityService;
import org.eclipse.openvsx.publish.PublishingConfig;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.search.SimilarityCheckService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.trustedpublishing.TrustedPublishingConfig;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.VersionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalRegistryServiceTest {

    @Mock
    EntityManager entityManager;

    @Mock
    RepositoryService repositories;

    @Mock
    ExtensionService extensions;

    @Mock
    VersionService versions;

    @Mock
    UserService users;

    @Mock
    AccessTokenService tokens;

    @Mock
    SearchUtilService searchUtilService;

    @Mock
    ExtensionValidator validator;

    @Mock
    StorageUtilService storageUtilService;

    @Mock
    EclipseService eclipse;

    @Mock
    CacheService cacheService;

    @Mock
    ExtensionVersionIntegrityService integrityService;

    @Mock
    SimilarityCheckService similarityCheckService;

    private LocalRegistryService registryService;

    private TempFile tempFile;

    @BeforeEach
    void setUp() {
        registryService = new LocalRegistryService(
                entityManager,
                repositories,
                extensions,
                versions,
                users,
                tokens,
                searchUtilService,
                validator,
                storageUtilService,
                eclipse,
                cacheService,
                integrityService,
                similarityCheckService,
                new PublishingConfig(),
                new TrustedPublishingConfig(),
                Duration.ofSeconds(30));

        // A permissive default for a void method rather than a per-test expectation: the tests of
        // visibleUntil exercise a pure function and touch no mock at all.
        lenient().doNothing().when(eclipse).checkPublisherAgreement(any());
    }

    /**
     * Regression test for a lost-update-style race: {@code ExtensionService.publishVersion(...)} hands
     * the temp file off to the {@code @Async} publish pipeline once metadata validation succeeds - that
     * pipeline reads the file (storage upload, signing, checksum) on a background thread and deletes it
     * itself once done. If {@code publish()} also closed it in its own try-with-resources, it would
     * delete the file out from under that background thread almost immediately, since publishVersion
     * returns as soon as the async task is fired, well before that thread is guaranteed to have even
     * started reading - producing an intermittent NoSuchFileException on the temp .vsix path.
     */
    @Test
    void shouldNotDeleteTempFileOnceOwnershipIsHandedToAsyncPublish() throws IOException {
        tempFile = new TempFile("extension_", ".vsix");
        Files.write(tempFile.getPath(), createExtensionPackage("bar", "1.0.0"));

        var token = new PersonalAccessToken();
        token.setUser(new UserData());

        var namespace = new Namespace();
        namespace.setName("foo");
        var extension = new Extension();
        extension.setNamespace(namespace);
        extension.setName("bar");
        var extVersion = new ExtensionVersion();
        extVersion.setId(42L);
        extVersion.setExtension(extension);
        extVersion.setVersion("1.0.0");

        when(extensions.createExtensionFile(any())).thenReturn(tempFile);
        when(tokens.useAccessToken(eq("tok"), any())).thenReturn(token);
        when(extensions.publishVersion(any(ExtensionProcessor.class), eq(token))).thenReturn(extVersion);
        when(storageUtilService.getFileUrls(any(), any(), any(String[].class))).thenReturn(Map.of(42L, Map.of()));

        registryService.publish(new ByteArrayInputStream(new byte[0]), "tok");

        assertThat(Files.exists(tempFile.getPath()))
                .as(
                        "ownership of the temp file was handed to the async publish pipeline; "
                                + "the request thread must not delete it")
                .isTrue();
    }

    /**
     * The counterpart of the above: when publish() rejects before ever calling
     * extensions.publishVersion(...) (so ownership was never handed off), it must still clean up the
     * temp file itself - nothing else will.
     */
    @Test
    void shouldDeleteTempFileWhenRejectedBeforeHandoff() throws IOException {
        tempFile = new TempFile("extension_", ".vsix");
        Files.write(tempFile.getPath(), createExtensionPackage("bar", "1.0.0"));

        when(extensions.createExtensionFile(any())).thenReturn(tempFile);
        when(tokens.useAccessToken(eq("tok"), any())).thenReturn(null);

        assertThatThrownBy(() -> registryService.publish(new ByteArrayInputStream(new byte[0]), "tok"))
                .isInstanceOf(ErrorResultException.class);

        assertThat(Files.exists(tempFile.getPath()))
                .as(
                        "ownership was never handed off (publishVersion was never called), so publish() "
                                + "must clean up the temp file itself")
                .isFalse();
    }

    @Test
    void shouldRejectNamespaceWhenSimilarNameExists() {
        // Build request with a name that collides with an existing namespace.
        var json = new NamespaceJson();
        json.setName("new-space");
        var user = new UserData();

        when(validator.validateNamespace("new-space")).thenReturn(Optional.empty());
        when(repositories.findNamespaceName("new-space")).thenReturn(null);
        when(similarityCheckService.isEnabled()).thenReturn(true);
        when(similarityCheckService.findSimilarNamespacesForCreation("new-space", user))
                .thenReturn(List.of(buildNamespace("new-space-1")));

        assertThatThrownBy(() -> registryService.createNamespace(json, user))
                .isInstanceOf(ErrorResultException.class)
                .hasMessageContaining("too similar to existing namespace");

        verify(entityManager, never()).persist(any(Namespace.class));
    }

    @Test
    void shouldRejectExistingNamespaceBeforeSimilarityCheck() {
        // If the namespace already exists, we should fail fast and avoid extra work.
        var json = new NamespaceJson();
        json.setName("duplicate");
        var user = new UserData();

        when(validator.validateNamespace("duplicate")).thenReturn(Optional.empty());
        when(repositories.findNamespaceName("duplicate")).thenReturn("duplicate");

        assertThatThrownBy(() -> registryService.createNamespace(json, user))
                .isInstanceOf(ErrorResultException.class)
                .hasMessageContaining("Namespace already exists: duplicate");

        // No persistence and no similarity checks should occur when we bail out early.
        verify(entityManager, never()).persist(any(Namespace.class));
        verify(similarityCheckService, never()).findSimilarNamespacesForCreation(any(), any());
    }

    @Test
    void shouldCreateNamespaceAndAssignContributorRole() {
        // Happy path: namespace is new and not similar, so we persist both entities.
        var json = new NamespaceJson();
        json.setName("clean-ns");
        var user = new UserData();

        when(validator.validateNamespace("clean-ns")).thenReturn(Optional.empty());
        when(repositories.findNamespaceName("clean-ns")).thenReturn(null);
        when(similarityCheckService.isEnabled()).thenReturn(true);
        when(similarityCheckService.findSimilarNamespacesForCreation("clean-ns", user)).thenReturn(List.of());

        registryService.createNamespace(json, user);

        // Capture persisted entities to verify they are wired as expected.
        var namespaceCaptor = ArgumentCaptor.forClass(Namespace.class);
        var membershipCaptor = ArgumentCaptor.forClass(NamespaceMembership.class);

        verify(entityManager).persist(namespaceCaptor.capture());
        verify(entityManager).persist(membershipCaptor.capture());

        var persistedNamespace = namespaceCaptor.getValue();
        var persistedMembership = membershipCaptor.getValue();

        assertThat(persistedNamespace.getName()).isEqualTo("clean-ns");
        assertThat(persistedMembership.getNamespace()).isSameAs(persistedNamespace);
        assertThat(persistedMembership.getUser()).isSameAs(user);
        assertThat(persistedMembership.getRole()).isEqualTo(NamespaceMembership.ROLE_CONTRIBUTOR);
    }

    /**
     * Regression coverage for eclipse-openvsx/openvsx#224: extension packs (and dependency lists) may
     * legitimately reference extensions that are not, or not yet, published here - publishing them is
     * not blocked on it - so the API needs to say which references actually resolve rather than just
     * handing out a URL that may 404.
     */
    @Test
    void shouldFlagWhetherEachExtensionReferenceIsAvailable() {
        var published = new ExtensionReferenceJson();
        published.setNamespace("foo");
        published.setExtension("published-one");
        var unpublished = new ExtensionReferenceJson();
        unpublished.setNamespace("foo");
        unpublished.setExtension("not-here-yet");

        when(repositories.findActiveExtension("published-one", "foo")).thenReturn(new Extension());
        when(repositories.findActiveExtension("not-here-yet", "foo")).thenReturn(null);

        registryService.resolveExtensionReferences(List.of(published, unpublished), "https://open-vsx.org");

        assertThat(published.getUrl()).isEqualTo("https://open-vsx.org/api/foo/published-one");
        assertThat(published.isAvailable()).isTrue();
        assertThat(unpublished.getUrl()).isEqualTo("https://open-vsx.org/api/foo/not-here-yet");
        assertThat(unpublished.isAvailable()).isFalse();
    }

    @Test
    void shouldTolerateNullExtensionReferenceList() {
        // getDependencies()/getBundledExtensions() are null whenever the version records none - must
        // be a no-op rather than a NullPointerException.
        registryService.resolveExtensionReferences(null, "https://open-vsx.org");
    }

    /**
     * toExtensionVersionJson(ExtensionVersion, String, String) backs the v1 getExtension(...) API and
     * used to fill in only the URL of each bundled-extension/dependency reference, duplicating (and
     * missing half of) what resolveExtensionReferences(...) does for v2 - so a reference to an
     * extension that is not (yet) published always came back without the {@code available} flag.
     */
    @Test
    void shouldFlagAvailabilityOfReferencesInV1ExtensionJson() {
        var extVersion = mockExtensionVersionWithBundledExtension();
        var extension = extVersion.getExtension();
        when(repositories.findLatestVersionForAllUrls(extension, null, false, true)).thenReturn(null);
        when(repositories.findLatestVersionForAllUrls(extension, null, true, true)).thenReturn(null);
        when(repositories.findVersionStringsSorted(extension, null, true)).thenReturn(List.of());
        when(storageUtilService.getFileUrls(any(), any(), any(String[].class))).thenReturn(Map.of(1L, Map.of()));
        when(repositories.findActiveExtension("bar", "foo")).thenReturn(null);

        var json = registryService.toExtensionVersionJson(extVersion, null, true);

        assertThat(json.getBundledExtensions()).hasSize(1);
        assertThat(json.getBundledExtensions().getFirst().isAvailable()).isFalse();
    }

    /**
     * Same regression as above, for the other v1 overload of toExtensionVersionJson(...) (the one
     * backing the v1 query(...) API).
     */
    @Test
    void shouldFlagAvailabilityOfReferencesInV1QueryExtensionJson() {
        var extVersion = mockExtensionVersionWithBundledExtension();
        when(repositories.findActiveExtension("bar", "foo")).thenReturn(new Extension());

        var json = registryService
                .toExtensionVersionJson(extVersion, null, null, 0L, false, null, null, List.of(), Map.of());

        assertThat(json.getBundledExtensions()).hasSize(1);
        assertThat(json.getBundledExtensions().getFirst().isAvailable()).isTrue();
    }

    private ExtensionVersion mockExtensionVersionWithBundledExtension() {
        var namespace = new Namespace();
        namespace.setName("foo");
        var extension = new Extension();
        extension.setNamespace(namespace);
        extension.setName("baz");
        var extVersion = new ExtensionVersion();
        extVersion.setId(1L);
        extVersion.setExtension(extension);
        extVersion.setVersion("1.0.0");
        extVersion.setBundledExtensions(List.of("foo.bar"));
        return extVersion;
    }

    @Test
    void shouldHoldBackTheMostRecentChanges() {
        // A request that reaches the present is clamped to the lag, so an entry whose transaction may
        // still be committing is not reported and cannot be passed over.
        var now = LocalDateTime.parse("2026-01-14T09:30:11");

        assertThat(LocalRegistryService.visibleUntil(null, now, Duration.ofSeconds(30)))
                .isEqualTo(LocalDateTime.parse("2026-01-14T09:29:41"));
    }

    @Test
    void shouldHoldBackAnUntilInsideTheLag() {
        var now = LocalDateTime.parse("2026-01-14T09:30:11");
        var until = LocalDateTime.parse("2026-01-14T09:30:00");

        // Asking for entries closer to the present than the lag reports nothing beyond it rather than
        // exposing them early.
        assertThat(LocalRegistryService.visibleUntil(until, now, Duration.ofSeconds(30)))
                .isEqualTo(LocalDateTime.parse("2026-01-14T09:29:41"));
    }

    @Test
    void shouldNotHoldBackAHistoricalUntil() {
        var now = LocalDateTime.parse("2026-01-14T09:30:11");
        var until = LocalDateTime.parse("2026-01-01T00:00");

        // Those entries have long been committed, so the caller's bound is the restrictive one and is
        // left alone.
        assertThat(LocalRegistryService.visibleUntil(until, now, Duration.ofSeconds(30))).isEqualTo(until);
    }

    @Test
    void shouldReportEverythingWithoutALag() {
        // A deployment that turns the lag off gets the whole log, which is what a registry with no
        // concurrent writers can afford.
        var now = LocalDateTime.parse("2026-01-14T09:30:11");

        assertThat(LocalRegistryService.visibleUntil(null, now, Duration.ZERO)).isEqualTo(now);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile.getPath());
        }
    }

    /**
     * Builds a minimal valid .vsix package, matching the fixture RegistryAPITest uses for the same
     * purpose, so ExtensionProcessor can genuinely parse the namespace/extension name out of it.
     */
    private byte[] createExtensionPackage(String name, String version) throws IOException {
        var bytes = new ByteArrayOutputStream();
        var archive = new ZipOutputStream(bytes);
        archive.putNextEntry(new ZipEntry("extension.vsixmanifest"));
        var vsixmanifest = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<PackageManifest Version=\"2.0.0\" xmlns=\"http://schemas.microsoft.com/developer/vsx-schema/2011\">"
                + "<Metadata>"
                + "<Identity Language=\"en-US\" Id=\"" + name + "\" Version=\"" + version + "\" Publisher=\"foo\" />"
                + "<DisplayName>foo</DisplayName>"
                + "<Description xml:space=\"preserve\"></Description>"
                + "<Tags></Tags>"
                + "<Categories>Other</Categories>"
                + "<GalleryFlags>Public</GalleryFlags>"
                + "</Metadata>"
                + "<Installation>"
                + "<InstallationTarget Id=\"Microsoft.VisualStudio.Code\"/>"
                + "</Installation>"
                + "<Dependencies/>"
                + "<Assets>"
                + "<Asset Type=\"Microsoft.VisualStudio.Code.Manifest\" Path=\"extension/package.json\" "
                + "Addressable=\"true\" />"
                + "</Assets>"
                + "</PackageManifest>";
        archive.write(vsixmanifest.getBytes());
        archive.closeEntry();
        archive.putNextEntry(new ZipEntry("extension/package.json"));
        var packageJson = "{"
                + "\"publisher\": \"foo\","
                + "\"name\": \"" + name + "\","
                + "\"version\": \"" + version + "\","
                + "\"displayName\": \"foo\""
                + "}";
        archive.write(packageJson.getBytes());
        archive.closeEntry();
        archive.finish();
        return bytes.toByteArray();
    }

    private Namespace buildNamespace(String name) {
        var namespace = new Namespace();
        namespace.setName(name);
        return namespace;
    }

    private NamespaceMembership buildMembership(UserData user, String namespaceName) {
        var namespace = new Namespace();
        namespace.setName(namespaceName);
        var membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        return membership;
    }
}
