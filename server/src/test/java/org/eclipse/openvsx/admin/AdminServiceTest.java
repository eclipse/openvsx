/********************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.admin;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.eclipse.EclipseService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.ExtensionVersionChange;
import org.eclipse.openvsx.entities.ExtensionVersionState;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.LogService;
import org.eclipse.openvsx.util.TargetPlatform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AdminService}.
 * <p>
 * Mainly {@link AdminService#purgeExtensionAndReferencingExtensions(UserData, String, String)}: the cascade
 * that purges an extension together with every extension that references it (packs bundling it, extensions
 * depending on it), walking the reverse-reference direction.
 */
@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final String NAMESPACE = "n";

    @Mock
    RepositoryService repositories;

    @Mock
    ExtensionService extensions;

    @Mock
    EclipseService eclipse;

    @Mock
    LogService logs;

    @InjectMocks
    AdminService adminService;

    private final UserData admin = new UserData();
    private long idSequence = 0;

    private Extension extension(String name) {
        var namespace = new Namespace();
        namespace.setName(NAMESPACE);
        var extension = new Extension();
        extension.setId(++idSequence);
        extension.setName(name);
        extension.setNamespace(namespace);
        return extension;
    }

    private ExtensionVersion version(Extension extension) {
        var extVersion = new ExtensionVersion();
        extVersion.setId(++idSequence);
        extVersion.setExtension(extension);
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        return extVersion;
    }

    private void mockNoReferences(Extension extension) {
        when(repositories.findBundledExtensionsReference(extension)).thenReturn(Streamable.empty());
        when(repositories.findDependenciesReference(extension)).thenReturn(Streamable.empty());
    }

    @Test
    void revokingPublisherContributionsRecordsWhenTheVersionsStoppedBeingVisible() {
        var user = new UserData();
        user.setLoginName("amy");
        var extVersion = version(extension("ext"));
        extVersion.setActive(true);

        when(repositories.findUserByLoginName("github", "amy")).thenReturn(user);
        when(repositories.findAccessTokens(user)).thenReturn(Streamable.empty());
        when(repositories.findVersionsByUser(user, true)).thenReturn(Streamable.of(extVersion));

        adminService.revokePublisherContributions("github", "amy", admin);

        assertThat(extVersion.isActive()).isFalse();
        assertThat(extVersion.isRemoved()).isFalse();
        // The version stops being publicly visible without being removed. Unless that transition is
        // appended to the log, the changes feed would keep reporting the version as active and
        // consumers following the feed would never learn that it disappeared.
        verify(repositories).recordExtensionVersionChange(
                eq(extVersion),
                eq(ExtensionVersionState.INACTIVE),
                any());
    }

    // Regression: revokePublisherAgreement used to be called unguarded, so any exception it threw
    // (Eclipse API down, unexpected runtime error, ...) rolled back the whole revoke, including the
    // token/extension/membership cleanup that has nothing to do with Eclipse.
    @Test
    void revokingPublisherContributionsStillDeactivatesEverythingElseWhenTheEclipseRevokeFails() {
        var user = new UserData();
        user.setLoginName("amy");
        user.setEclipsePersonId("12345");
        var extVersion = version(extension("ext"));
        extVersion.setActive(true);

        when(repositories.findUserByLoginName("github", "amy")).thenReturn(user);
        when(repositories.findAccessTokens(user)).thenReturn(Streamable.empty());
        when(repositories.findVersionsByUser(user, true)).thenReturn(Streamable.of(extVersion));
        when(eclipse.determinePublisherAgreementStatus(user)).thenReturn("signed");
        doThrow(new RuntimeException("Eclipse API is down")).when(eclipse).revokePublisherAgreement(user, admin);

        var result = adminService.revokePublisherContributions("github", "amy", admin);

        assertThat(extVersion.isActive()).isFalse();
        verify(repositories).recordExtensionVersionChange(
                eq(extVersion),
                eq(ExtensionVersionState.INACTIVE),
                any());
        // The failure is reported back rather than silently dropped or failing the whole call.
        assertThat(result.getError()).isNull();
        assertThat(result.getWarning()).contains("Eclipse API is down");
    }

    // Regression: the revoke used to be attempted whenever an Eclipse person ID was present,
    // even if the agreement status was 'none' or could not be determined at all - calling the
    // Eclipse API on a guess rather than a confirmed agreement.
    @Test
    void revokingPublisherContributionsDoesNotCallEclipseWhenTheAgreementStatusIsNotConfirmed() {
        var user = new UserData();
        user.setLoginName("amy");
        user.setEclipsePersonId("12345");

        when(repositories.findUserByLoginName("github", "amy")).thenReturn(user);
        when(repositories.findAccessTokens(user)).thenReturn(Streamable.empty());
        when(repositories.findVersionsByUser(user, true)).thenReturn(Streamable.empty());
        when(eclipse.determinePublisherAgreementStatus(user)).thenReturn(null);

        var result = adminService.revokePublisherContributions("github", "amy", admin);

        verify(eclipse, never()).revokePublisherAgreement(any(), any());
        assertThat(result.getError()).isNull();
        assertThat(result.getWarning()).isNull();
    }

    @Test
    void purgesReferencingExtensionAsWholeWhenAllVersionsReference() {
        var target = extension("target");
        var referencing = extension("referencing");
        // The referencing extension has two versions, both bundling the target.
        var refV1 = version(referencing);
        var refV2 = version(referencing);

        when(extensions.lockExtension(NAMESPACE, "target")).thenReturn(target);
        when(repositories.findBundledExtensionsReference(target)).thenReturn(Streamable.of(refV1, refV2));
        when(repositories.findDependenciesReference(target)).thenReturn(Streamable.empty());
        when(repositories.countVersions(NAMESPACE, "referencing")).thenReturn(2);
        mockNoReferences(referencing);

        adminService.purgeExtensionAndReferencingExtensions(admin, NAMESPACE, "target");

        // The referencing extension is purged as a whole (not version-by-version) so nothing is orphaned.
        verify(extensions).purgeExtension(admin, referencing, false);
        verify(extensions).purgeExtension(admin, target, false);
        verify(extensions, never()).purgeExtensionVersion(any(), any());
    }

    @Test
    void purgesOnlyReferencingVersionsWhenSomeVersionsReference() {
        var target = extension("target");
        var referencing = extension("referencing");
        // Only one of the referencing extension's three versions bundles the target.
        var refV1 = version(referencing);

        when(extensions.lockExtension(NAMESPACE, "target")).thenReturn(target);
        when(repositories.findBundledExtensionsReference(target)).thenReturn(Streamable.of(refV1));
        when(repositories.findDependenciesReference(target)).thenReturn(Streamable.empty());
        when(repositories.countVersions(NAMESPACE, "referencing")).thenReturn(3);

        adminService.purgeExtensionAndReferencingExtensions(admin, NAMESPACE, "target");

        // Only the referencing version is purged; the extension keeps its other versions.
        verify(extensions).purgeExtensionVersion(admin, refV1);
        verify(extensions, never()).purgeExtension(admin, referencing, false);
        verify(extensions).purgeExtension(admin, target, false);
    }

    @Test
    void handlesReferenceCyclesWithoutInfiniteRecursion() {
        // a and b bundle each other; both are single-version extensions.
        var a = extension("a");
        var b = extension("b");
        var aV = version(a);
        var bV = version(b);

        when(extensions.lockExtension(NAMESPACE, "a")).thenReturn(a);
        // versions bundling a -> b's version; versions bundling b -> a's version
        when(repositories.findBundledExtensionsReference(a)).thenReturn(Streamable.of(bV));
        when(repositories.findDependenciesReference(a)).thenReturn(Streamable.empty());
        when(repositories.findBundledExtensionsReference(b)).thenReturn(Streamable.of(aV));
        when(repositories.findDependenciesReference(b)).thenReturn(Streamable.empty());
        when(repositories.countVersions(NAMESPACE, "a")).thenReturn(1);
        when(repositories.countVersions(NAMESPACE, "b")).thenReturn(1);

        assertThatCode(() -> adminService.purgeExtensionAndReferencingExtensions(admin, NAMESPACE, "a"))
                .doesNotThrowAnyException();

        verify(extensions).purgeExtension(admin, a, false);
        verify(extensions).purgeExtension(admin, b, false);
        verify(extensions, never()).purgeExtensionVersion(any(), any());
    }

    @Test
    void purgesDeepReferenceChainWithoutDepthLimit() {
        // Chain of 8 single-version extensions where each references the previous one; the previous
        // depth limit (> 5) would have aborted this legitimate chain, the visited-set does not.
        var chain = new Extension[8];
        var chainVersions = new ExtensionVersion[8];
        for (var i = 0; i < chain.length; i++) {
            chain[i] = extension("e" + i);
            chainVersions[i] = version(chain[i]);
        }

        when(extensions.lockExtension(NAMESPACE, "e0")).thenReturn(chain[0]);
        for (var i = 0; i < chain.length; i++) {
            // chain[i+1] references chain[i]
            when(repositories.findBundledExtensionsReference(chain[i]))
                    .thenReturn(i + 1 < chain.length ? Streamable.of(chainVersions[i + 1]) : Streamable.empty());
            when(repositories.findDependenciesReference(chain[i])).thenReturn(Streamable.empty());
            if (i > 0) {
                when(repositories.countVersions(NAMESPACE, "e" + i)).thenReturn(1);
            }
        }

        assertThatCode(() -> adminService.purgeExtensionAndReferencingExtensions(admin, NAMESPACE, "e0"))
                .doesNotThrowAnyException();

        for (var extension : chain) {
            verify(extensions).purgeExtension(admin, extension, false);
        }
    }
}
