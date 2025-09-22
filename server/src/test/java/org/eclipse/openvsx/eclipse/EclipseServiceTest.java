/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.eclipse;

import com.google.common.io.CharStreams;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.ExtensionValidator;
import org.eclipse.openvsx.MockTransactionTemplate;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.adapter.VSCodeIdService;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.cache.LatestExtensionVersionCacheKeyGenerator;
import org.eclipse.openvsx.entities.*;
import org.eclipse.openvsx.publish.PublishExtensionVersionHandler;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.storage.*;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.util.Streamable;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(SpringExtension.class)
@MockitoBean(types = {
    EntityManager.class, SearchUtilService.class, GoogleCloudStorageService.class, AzureBlobStorageService.class,
    AwsStorageService.class, VSCodeIdService.class, AzureDownloadCountService.class, CacheService.class,
    UserService.class, PublishExtensionVersionHandler.class, SimpleMeterRegistry.class, FileCacheDurationConfig.class
})
class EclipseServiceTest {

    @MockitoBean
    RepositoryService repositories;

    @MockitoBean
    TokenService tokens;

    @MockitoBean
    RestTemplate restTemplate;

    @Autowired
    EclipseService eclipse;

    @BeforeEach
    void setup() {
        eclipse.publisherAgreementAllowedVersions = List.of("1", "1.0", "1.1");
        eclipse.publisherAgreementVersion = "1.1";
        eclipse.eclipseApiUrl = "https://test.openvsx.eclipse.org/";
    }

    @Test
    void testGetPublicProfile() throws Exception {
        var urlTemplate = "https://test.openvsx.eclipse.org/account/profile/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
                .thenReturn(mockProfileResponse());

        var profile = eclipse.getPublicProfile("test");

        assertThat(profile).isNotNull();
        assertThat(profile.getName()).isEqualTo("test");
        assertThat(profile.getGithubHandle()).isEqualTo("test");
        assertThat(profile.getPublisherAgreements()).isNotNull();
        assertThat(profile.getPublisherAgreements().getOpenVsx()).isNotNull();
        assertThat(profile.getPublisherAgreements().getOpenVsx().getVersion()).isEqualTo("1.1");
    }

    @Test
    void testGetUserProfile() throws Exception {
        Mockito.when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
            .thenReturn(mockProfileResponse());

        var profile = eclipse.getUserProfile("12345");

        assertThat(profile).isNotNull();

        assertThat(profile.getName()).isEqualTo("test");
        assertThat(profile.getGithubHandle()).isEqualTo("test");
        assertThat(profile.getPublisherAgreements()).isNotNull();
        assertThat(profile.getPublisherAgreements().getOpenVsx()).isNotNull();
        assertThat(profile.getPublisherAgreements().getOpenVsx().getVersion()).isEqualTo("1.1");
    }

    @Test
    void testGetPublisherAgreement() throws Exception {
        var user = mockUser();
        user.setEclipsePersonId("test");

        var urlTemplate = "https://test.openvsx.eclipse.org/openvsx/publisher_agreement/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
                .thenReturn(mockAgreementResponse());

        var agreement = eclipse.getPublisherAgreement(user);
        assertThat(agreement).isNotNull();
        assertThat(agreement.isActive()).isTrue();
        assertThat(agreement.documentId()).isEqualTo("abcd");
        assertThat(agreement.version()).isEqualTo("1.1");
        assertThat(agreement.timestamp()).isEqualTo(LocalDateTime.of(2020, 10, 9, 5, 10, 32));
    }

    @Test
    void testCheckPublisherAgreementOutdated() throws Exception {
        var user = mockUser();
        user.setEclipsePersonId("test");

        var urlTemplate = "https://test.openvsx.eclipse.org/account/profile/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
                .thenReturn(mockOutdatedProfileResponse());

        try {
            eclipse.checkPublisherAgreement(user);
            fail("Expected an ErrorResultException");
        } catch(ErrorResultException exc) {
            assertThat(exc.getMessage()).isEqualTo("Your Publisher Agreement with the Eclipse Foundation is outdated (version 0.1). The current version is 1.1.");
        }
    }

    @Test
    void testCheckPublisherAgreementAllowed() throws Exception {
        var user = mockUser();
        user.setEclipsePersonId("test");

        var urlTemplate = "https://test.openvsx.eclipse.org/account/profile/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
                .thenReturn(mockAllowedProfileResponse());

        eclipse.checkPublisherAgreement(user);
    }

    @Test
    void testCheckPublisherAgreement() throws Exception {
        var user = mockUser();
        user.setEclipsePersonId("test");

        var urlTemplate = "https://test.openvsx.eclipse.org/account/profile/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
                .thenReturn(mockProfileResponse());

        eclipse.checkPublisherAgreement(user);
    }

    @Test
    void testGetPublisherAgreementNotFound() throws Exception {
        var user = mockUser();
        user.setEclipsePersonId("test");

        var urlTemplate = "https://test.openvsx.eclipse.org/openvsx/publisher_agreement/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        var agreement = eclipse.getPublisherAgreement(user);
        assertThat(agreement).isNull();
    }

    @Test
    void testGetPublisherAgreementNotAuthenticated() throws Exception {
        var user = mockUser();

        var agreement = eclipse.getPublisherAgreement(user);

        assertThat(agreement).isNull();
    }

    @Test
    void testSignPublisherAgreement() throws Exception {
        var user = mockUser();
        Mockito.when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
            .thenReturn(mockAgreementResponse());
        Mockito.when(repositories.findVersionsByUser(user, false))
            .thenReturn(Streamable.empty());

        var agreement = eclipse.signPublisherAgreement(user);
        assertThat(agreement).isNotNull();
        assertThat(agreement.isActive()).isTrue();
        assertThat(agreement.documentId()).isEqualTo("abcd");
        assertThat(agreement.version()).isEqualTo("1.1");
        assertThat(agreement.timestamp()).isEqualTo(LocalDateTime.of(2020, 10, 9, 5, 10, 32));
    }

    @Test
    void testSignPublisherAgreementReactivateExtension() throws Exception {
        var user = mockUser();
        Mockito.when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
            .thenReturn(mockAgreementResponse());
        var namespace = new Namespace();
        namespace.setName("foo");
        var extension = new Extension();
        extension.setName("bar");
        extension.setNamespace(namespace);
        var extVersion = new ExtensionVersion();
        extVersion.setVersion("1.0.0");
        extVersion.setTargetPlatform(TargetPlatform.NAME_UNIVERSAL);
        extVersion.setExtension(extension);
        extension.getVersions().add(extVersion);
        Mockito.when(repositories.findVersionsByUser(user, false))
            .thenReturn(Streamable.of(extVersion));

        var agreement = eclipse.signPublisherAgreement(user);

        assertThat(agreement).isNotNull();
        assertThat(agreement.isActive()).isTrue();
        assertThat(agreement.documentId()).isEqualTo("abcd");
        assertThat(agreement.version()).isEqualTo("1.1");
        assertThat(agreement.timestamp()).isEqualTo(LocalDateTime.of(2020, 10, 9, 5, 10, 32));
        assertThat(extVersion.isActive()).isTrue();
        assertThat(extension.isActive()).isTrue();
    }

    @Test
    void testPublisherAgreementAlreadySigned() throws Exception {
        var user = mockUser();
        Mockito.when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));

        try {
            eclipse.signPublisherAgreement(user);
            fail("Expected an ErrorResultException");
        } catch (ErrorResultException exc) {
            assertThat(exc.getMessage()).isEqualTo("A publisher agreement is already present for user test.");
        }
    }

    @Test
    void testRevokePublisherAgreement() {
        var user = mockUser();
        user.setEclipsePersonId("test");

        eclipse.revokePublisherAgreement(user, null);
    }

    @Test
    void testRevokePublisherAgreementByAdmin() {
        var user = mockUser();
        user.setEclipsePersonId("test");

        var admin = new UserData();
        admin.setLoginName("admin");
        admin.setEclipseToken(new AuthToken("67890", null, null, null, null, null));
        Mockito.when(tokens.getActiveEclipseToken(admin))
            .thenReturn(admin.getEclipseToken());

        eclipse.revokePublisherAgreement(user, admin);
    }

    private UserData mockUser() {
        var user = new UserData();
        user.setLoginName("test");
        user.setProvider("github");
        user.setEclipseToken(new AuthToken("12345", null, null, null, null, null));
        Mockito.when(tokens.getActiveEclipseToken(user))
            .thenReturn(user.getEclipseToken());
        return user;
    }

    private ResponseEntity<String> mockProfileResponse() throws IOException {
        try (var stream = getClass().getResourceAsStream("profile-response.json")) {
            var json = CharStreams.toString(new InputStreamReader(stream));
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
    }

    private ResponseEntity<String> mockOutdatedProfileResponse() throws IOException {
        try (var stream = getClass().getResourceAsStream("profile-outdated-response.json")) {
            var json = CharStreams.toString(new InputStreamReader(stream));
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
    }

    private ResponseEntity<String> mockAllowedProfileResponse() throws IOException {
        try (var stream = getClass().getResourceAsStream("profile-allowed-response.json")) {
            var json = CharStreams.toString(new InputStreamReader(stream));
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
    }

    private ResponseEntity<String> mockAgreementResponse() throws IOException {
        try (var stream = getClass().getResourceAsStream("publisher-agreement-response.json")) {
            var json = CharStreams.toString(new InputStreamReader(stream));
            return new ResponseEntity<>(json, HttpStatus.OK);
        }
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        TransactionTemplate transactionTemplate() {
            return new MockTransactionTemplate();
        }

        @Bean
        EclipseService eclipseService(
                TokenService tokens,
                ExtensionService extensions,
                EntityManager entityManager,
                RestTemplate restTemplate
        ) {
            return new EclipseService(tokens, extensions, entityManager, restTemplate);
        }

        @Bean
        ExtensionService extensionService(
                RepositoryService repositories,
                SearchUtilService search,
                CacheService cache,
                PublishExtensionVersionHandler publishHandler
        ) {
            return new ExtensionService(repositories, search, cache, publishHandler);
        }

        @Bean
        ExtensionValidator extensionValidator() {
            return new ExtensionValidator();
        }

        @Bean
        StorageUtilService storageUtilService(
                RepositoryService repositories,
                GoogleCloudStorageService googleStorage,
                AzureBlobStorageService azureStorage,
                LocalStorageService localStorage,
                AwsStorageService awsStorage,
                AzureDownloadCountService azureDownloadCountService,
                SearchUtilService search,
                CacheService cache,
                EntityManager entityManager,
                FileCacheDurationConfig fileCacheDurationConfig
        ) {
            return new StorageUtilService(
                    repositories,
                    googleStorage,
                    azureStorage,
                    localStorage,
                    awsStorage,
                    azureDownloadCountService,
                    search,
                    cache,
                    entityManager,
                    fileCacheDurationConfig
            );
        }

        @Bean
        LocalStorageService localStorageService() {
            return new LocalStorageService();
        }

        @Bean
        LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKeyGenerator() {
            return new LatestExtensionVersionCacheKeyGenerator();
        }
    }
    
}