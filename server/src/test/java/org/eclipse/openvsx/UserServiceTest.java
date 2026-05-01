/** ******************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************* */
package org.eclipse.openvsx;

import static org.mockito.ArgumentMatchers.anyString;

import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.OAuth2AttributesConfig;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.persistence.EntityManager;

@ExtendWith(SpringExtension.class)
@MockitoBean(types = {
    EntityManager.class, StorageUtilService.class, CacheService.class, ExtensionValidator.class, OAuth2AttributesConfig.class
})
class UserServiceTest {

    @MockitoBean
    RepositoryService repositories;

    @Autowired
    UserService users;

    @Test
    void testUpsertUserMatchingAuthId() {
        var testAuthId = "test-login-name";
        Mockito.when(repositories.findUserByLoginName(anyString(), anyString())).thenReturn(mockUser(testAuthId));

        var updated = mockUser(testAuthId);
        Assertions.assertEquals(updated, users.upsertUser(updated), "Should succeed as there were no changes to the entity");
    }

    @Test
    void testUpsertUserNonMatchingAuthId() {
        var testAuthId = "test-login-name";
        Mockito.when(repositories.findUserByLoginName(anyString(), anyString())).thenReturn(mockUser(testAuthId));

        var updated = mockUser("some-other-id");
        var exception = Assertions.assertThrows(AuthenticationServiceException.class, () -> users.upsertUser(updated),
                "Should succeed as there were no changes to the entity");
        Assertions.assertTrue(exception.getMessage().startsWith("The GitHub ID setting in your profile"),
                "Exception should pertain to mismatch of GitHub ID");
    }

    private UserData mockUser(String authId) {
        var userData = new UserData();
        userData.setLoginName("test_user");
        userData.setFullName("Test User");
        userData.setAuthId(authId);
        userData.setProvider("sample");
        userData.setProviderUrl("http://example.com/test");
        return userData;
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        UserService userService(
                EntityManager entityManager,
                RepositoryService repositories,
                StorageUtilService storageUtil,
                CacheService cache,
                ExtensionValidator validator,
                @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository,
                OAuth2AttributesConfig attributesConfig
        ) {
            return new UserService(entityManager, repositories, storageUtil, cache, validator, clientRegistrationRepository, attributesConfig);
        }
    }
}
