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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
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
        when(config.getPrefix()).thenReturn("ovsx_");

        var value = accessTokenService.generateTokenValue(PersonalAccessTokenType.LLT);

        // prefix, then the marker saying what kind of token this is, then 256 bits base64url encoded
        assertThat(value).startsWith("ovsx_at_");
        assertThat(value.substring("ovsx_at_".length())).hasSize(43).doesNotContain("=", "+", "/");
        verifyNoInteractions(repositories);
    }

    @Test
    void marksEachKindOfTokenDistinctly() {
        when(config.getPrefix()).thenReturn("ovsx_");

        assertThat(accessTokenService.generateTokenValue(PersonalAccessTokenType.TPT)).startsWith("ovsx_tp_");
        assertThat(accessTokenService.generateTokenValue(PersonalAccessTokenType.LLT)).startsWith("ovsx_at_");
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
        when(config.getPrefix()).thenReturn("ovsx_");

        var before = LocalDateTime.now(ZoneId.of("UTC"));
        var json = accessTokenService
                .createTrustedPublishingAccessToken(
                        trustedPublisher,
                        "Trusted publishing (github)",
                        Duration.ofMinutes(7));
        var after = LocalDateTime.now(ZoneId.of("UTC"));

        var persisted = ArgumentCaptor.forClass(PersonalAccessToken.class);
        verify(entityManager).persist(persisted.capture());
        assertThat(persisted.getValue().getType()).isEqualTo(PersonalAccessTokenType.TPT);
        assertThat(persisted.getValue().getExpiresTimestamp())
                .isBetween(before.plusMinutes(7), after.plusMinutes(7));
        // the token is scoped to the registration's extension, whatever its lifetime
        assertThat(persisted.getValue().getScopeExtension()).isSameAs(extension);
        assertThat(json.getValue()).isNotNull();
    }
}
