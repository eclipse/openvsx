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

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.mail.MailService;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.UUIDService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenServiceTest {

    @Mock
    AccessTokenConfig config;

    @Mock
    UUIDService uuidService;

    @Mock
    EntityManager entityManager;

    @Mock
    RepositoryService repositories;

    @Mock
    MailService mail;

    @InjectMocks
    AccessTokenService accessTokenService;

    @BeforeEach
    void setUp() {
        when(config.getTokenHashAlgorithm()).thenReturn("SHA-256");
        when(config.getTokenHashSalt()).thenReturn("salt");
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
}
