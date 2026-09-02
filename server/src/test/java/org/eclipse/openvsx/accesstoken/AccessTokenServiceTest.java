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
package org.eclipse.openvsx.accesstoken;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import jakarta.persistence.EntityManager;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Streamable;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.eclipse.openvsx.entities.TrustedPublisher;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.mail.MailService;
import org.eclipse.openvsx.repositories.RepositoryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

    @Mock
    AccessTokenConfig config;

    @Mock
    EntityManager entityManager;

    @Mock
    RepositoryService repositories;

    @Mock
    MailService mail;

    @Mock
    DSLContext dsl;

    @InjectMocks
    AccessTokenService accessTokenService;

    @BeforeEach
    void setUp() {
        // lenient: generateTokenValue does not hash anything, so it needs neither of these
        lenient().when(config.getTokenHashAlgorithm()).thenReturn("SHA-256");
        lenient().when(config.getTokenHashSalt()).thenReturn("salt");
    }

    private PersonalAccessToken activeUnrestrictedToken() {
        var token = new PersonalAccessToken();
        token.setActive(true);
        token.setType(PersonalAccessTokenType.LLT);
        token.setVersion(1);
        return token;
    }

    @Test
    void authenticatesWithTheTokensUser() {
        var user = new UserData();
        var token = activeUnrestrictedToken();
        token.setUser(user);
        when(repositories.findPersonalAccessToken(anyString())).thenReturn(token);

        var tau = accessTokenService.useAccessToken("tok", new AccessTokenAction.Verify());

        assertThat(tau).isNotNull();
        assertThat(tau.userData()).isSameAs(user);
        assertThat(tau.type()).isEqualTo(PersonalAccessTokenType.LLT);
    }

    // Regression: personal_access_token.user_data has no NOT NULL constraint at the schema level.
    // A row with no user attached used to come back as a "successfully authenticated" (non-null)
    // AccessTokenAuthentication wrapping a null userData(), which every caller dereferences
    // unguarded (e.g. LocalRegistryService#createNamespace/deleteExtension/verifyToken), turning a
    // legacy/corrupt token row into an unhandled NPE instead of the intended auth failure.
    @Test
    void rejectsATokenWithNoUser() {
        var token = activeUnrestrictedToken();
        token.setUser(null);
        when(repositories.findPersonalAccessToken(anyString())).thenReturn(token);

        var tau = accessTokenService.useAccessToken("tok", new AccessTokenAction.Verify());

        assertThat(tau).isNull();
    }

    // The upgrade job is enqueued from every pod's own ApplicationStartedEvent, so the advisory lock is
    // what keeps one rolling update from having each of them scan and rewrite the same rows.
    @Test
    void skipsTheTokenUpgradeWhenAnotherInstanceHoldsTheLock() {
        var service = Mockito.spy(accessTokenService);
        Mockito.doReturn(false).when(service).tryAcquireUpgradeLock();

        assertThat(service.upgradeTokens()).isZero();

        verifyNoInteractions(repositories);
    }

    @Test
    void upgradesTokensWhenItWinsTheLock() {
        var service = Mockito.spy(accessTokenService);
        Mockito.doReturn(true).when(service).tryAcquireUpgradeLock();
        var legacy = new PersonalAccessToken();
        legacy.setVersion(0);
        legacy.setValue("raw");
        when(repositories.findAllPersonalAccessTokensByVersion(0)).thenReturn(Streamable.of(legacy));

        assertThat(service.upgradeTokens()).isEqualTo(1);

        assertThat(legacy.getVersion()).isEqualTo(1);
        assertThat(legacy.getValue()).isNotEqualTo("raw");
    }

    // The old implementation regenerated until repositories.hasPersonalAccessToken(value) came back
    // false, comparing a raw value against a column that stores salted hashes - it could never match for
    // a current token, so it only cost a query per token created. Uniqueness is UNIQUE (value)'s job, and
    // only it can work across pods anyway.
    @Test
    void generatesATokenValueWithoutAskingTheDatabase() {
        when(config.getPrefix()).thenReturn("ovsx");

        var value = accessTokenService.generateTokenValue(PersonalAccessTokenType.LLT);

        // prefix, then the marker saying what kind of token this is, then 256 bits base64url encoded
        assertThat(value).startsWith("ovsxat_");
        assertThat(value.substring("ovsxat_".length())).hasSize(43).doesNotContain("=", "+", "/");
        verifyNoInteractions(repositories);
    }

    @Test
    void marksEachKindOfTokenDistinctly() {
        when(config.getPrefix()).thenReturn("ovsx");

        assertThat(accessTokenService.generateTokenValue(PersonalAccessTokenType.TPT)).startsWith("ovsxtp_");
        assertThat(accessTokenService.generateTokenValue(PersonalAccessTokenType.LLT)).startsWith("ovsxat_");
    }

    @Test
    void generatesADifferentValueEveryTime() {
        when(config.getPrefix()).thenReturn("");

        var values = java.util.stream.Stream.generate(
                () -> accessTokenService.generateTokenValue(PersonalAccessTokenType.LLT)).limit(100).toList();

        assertThat(values).doesNotHaveDuplicates();
    }

    // How long a publishing token lives is the trusted publishing configuration's to decide, so this
    // service applies whatever it is handed instead of reading a setting of its own.
    @Test
    void appliesTheExpirationItIsGivenToATrustedPublishingToken() {
        var user = new UserData();
        var namespace = new Namespace();
        namespace.setName("foo");
        var extension = new Extension();
        extension.setName("bar");
        extension.setNamespace(namespace);
        var trustedPublisher = new TrustedPublisher();
        trustedPublisher.setExtension(extension);
        trustedPublisher.setCreatedBy(user);
        trustedPublisher.setRegistration(Map.of());
        when(config.getPrefix()).thenReturn("ovsx");

        var before = LocalDateTime.now(ZoneId.of("UTC"));
        var json = accessTokenService
                .createTrustedPublishingAccessToken(
                        trustedPublisher,
                        "Trusted publishing (github)",
                        Duration.ofMinutes(7),
                        Map.of("repository_id", "74"));
        var after = LocalDateTime.now(ZoneId.of("UTC"));

        var persisted = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(entityManager).persist(persisted.capture());
        assertThat(persisted.getValue().getType()).isEqualTo(PersonalAccessTokenType.TPT);
        assertThat(persisted.getValue().getExpiresTimestamp())
                .isBetween(before.plusMinutes(7), after.plusMinutes(7));
        // the token is scoped to the registration's extension, whatever its lifetime
        assertThat(persisted.getValue().getScopeExtension()).isSameAs(extension);
        // carried to the publish that uses this token, which copies them onto the version
        assertThat(persisted.getValue().getClaims()).containsEntry("repository_id", "74");
        assertThat(json.getValue()).isNotNull();
    }

    private PersonalAccessToken trustedPublishingToken(UserData user) {
        var namespace = new Namespace();
        namespace.setName("foo");
        var extension = new Extension();
        extension.setName("bar");
        extension.setNamespace(namespace);
        var trustedPublisher = new TrustedPublisher();
        trustedPublisher.setExtension(extension);
        trustedPublisher.setCreatedBy(user);
        trustedPublisher.setRegistration(Map.of());

        var token = new PersonalAccessToken();
        token.setActive(true);
        token.setType(PersonalAccessTokenType.TPT);
        token.setVersion(1);
        token.setUser(user);
        token.setTrustedPublisher(trustedPublisher);
        token.setScopeExtension(extension);
        return token;
    }

    // A release commonly publishes one version per target platform, and each is its own publish request.
    // The CLI exchanges the CI identity once and shares the token across them (cli/src/trusted-publishing.ts
    // caches it per extension), so consuming it on first use failed every target but one - and since the
    // CLI publishes them concurrently, which ones failed was down to timing.
    @Test
    void keepsATrustedPublishingTokenUsableForEveryTargetPlatformOfARelease() {
        var user = new UserData();
        var token = trustedPublishingToken(user);
        when(repositories.findPersonalAccessToken(anyString())).thenReturn(token);

        for (var i = 0; i < 3; i++) {
            var tau = accessTokenService
                    .useAccessToken("tok", new AccessTokenAction.PublishVersion("foo", "bar"));

            assertThat(tau).isNotNull();
            assertThat(tau.type()).isEqualTo(PersonalAccessTokenType.TPT);
        }

        verify(entityManager, never()).remove(any());
    }

    // The deprecated one-time type keeps its meaning: the split of one-time from ephemeral was to let a
    // trusted publishing token outlive a single request, not to make every machine token reusable.
    @Test
    void stillConsumesAOneTimeTokenOnFirstUse() {
        var token = activeUnrestrictedToken();
        token.setType(PersonalAccessTokenType.OTT);
        token.setUser(new UserData());
        when(repositories.findPersonalAccessToken(anyString())).thenReturn(token);

        var tau = accessTokenService.useAccessToken("tok", new AccessTokenAction.PublishVersion("foo", "bar"));

        assertThat(tau).isNotNull();
        verify(entityManager).remove(token);
    }

    // Reusable until it expires, but not past it - and the row goes rather than lingering deactivated,
    // because it carries the OIDC claims of the workflow that obtained it.
    @Test
    void deletesAnExpiredTrustedPublishingTokenRatherThanDeactivatingIt() {
        var token = trustedPublishingToken(new UserData());
        token.setExpiresTimestamp(LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(1));
        when(repositories.findPersonalAccessToken(anyString())).thenReturn(token);

        var tau = accessTokenService.useAccessToken("tok", new AccessTokenAction.PublishVersion("foo", "bar"));

        assertThat(tau).isNull();
        verify(entityManager).remove(token);
        assertThat(token.isActive()).isTrue();
    }

    // Nothing ever shows a trusted publishing token to its owner, so there is no page for a revoke link
    // to sit on. Being reusable does not make it user-managed.
    @Test
    void offersNoRevocationLinkForATrustedPublishingToken() {
        var user = new UserData();
        var trustedPublisher = trustedPublishingToken(user).getTrustedPublisher();
        when(config.getPrefix()).thenReturn("ovsx");

        var json = accessTokenService.createTrustedPublishingAccessToken(
                trustedPublisher,
                "Trusted publishing (github)",
                Duration.ofMinutes(5),
                Map.of());

        assertThat(json.getDeleteTokenUrl()).isNull();
    }
}
