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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

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
import org.eclipse.openvsx.security.TokenService;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.AzureDownloadCountService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.google.common.io.CharStreams;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(SpringExtension.class)
@MockBean({
    EntityManager.class, SearchUtilService.class, GoogleCloudStorageService.class, AzureBlobStorageService.class,
    VSCodeIdService.class, AzureDownloadCountService.class, CacheService.class,
    UserService.class, PublishExtensionVersionHandler.class,
    SimpleMeterRegistry.class
})
public class EclipseServiceTest {

    @MockBean
    RepositoryService repositories;

    @MockBean
    TokenService tokens;

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    EclipseService eclipse;

    @BeforeEach
    public void setup() {
        eclipse.publisherAgreementVersion = "1";
        eclipse.eclipseApiUrl = "https://test.openvsx.eclipse.org/";
    }

    @Test
    public void testGetPublicProfile() throws Exception {
        var urlTemplate = "https://test.openvsx.eclipse.org/account/profile/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
            .thenReturn(mockProfileResponse());

        var profile = eclipse.getPublicProfile("test");

        assertThat(profile).isNotNull();
        assertThat(profile.name).isEqualTo("test");
        assertThat(profile.githubHandle).isEqualTo("test");
        assertThat(profile.publisherAgreements).isNotNull();
        assertThat(profile.publisherAgreements.openVsx).isNotNull();
        assertThat(profile.publisherAgreements.openVsx.version).isEqualTo("1");
    }

    @Test
    public void testGetUserProfile() throws Exception {
        Mockito.when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
            .thenReturn(mockProfileResponse());

        var profile = eclipse.getUserProfile("12345");

        assertThat(profile).isNotNull();
        assertThat(profile.name).isEqualTo("test");
        assertThat(profile.githubHandle).isEqualTo("test");
        assertThat(profile.publisherAgreements).isNotNull();
        assertThat(profile.publisherAgreements.openVsx).isNotNull();
        assertThat(profile.publisherAgreements.openVsx.version).isEqualTo("1");
    }

    @Test
    public void testGetPublisherAgreement() throws Exception {
        var user = mockUser();
        var eclipseData = new EclipseData();
        user.setEclipseData(eclipseData);
        eclipseData.personId = "test";

        var urlTemplate = "https://test.openvsx.eclipse.org/openvsx/publisher_agreement/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
                .thenReturn(mockAgreementResponse());

        var agreement = eclipse.getPublisherAgreement(user);
        assertThat(agreement).isNotNull();
        assertThat(agreement.isActive).isTrue();
        assertThat(agreement.documentId).isEqualTo("abcd");
        assertThat(agreement.version).isEqualTo("1");
        assertThat(agreement.timestamp).isNotNull();
        assertThat(agreement.timestamp.toString()).isEqualTo("2020-10-09T05:10:32");
    }

    @Test
    public void testGetPublisherAgreementNotFound() throws Exception {
        var user = mockUser();
        var eclipseData = new EclipseData();
        user.setEclipseData(eclipseData);
        eclipseData.personId = "test";

        var urlTemplate = "https://test.openvsx.eclipse.org/openvsx/publisher_agreement/{personId}";
        Mockito.when(restTemplate.exchange(eq(urlTemplate), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class), eq(Map.of("personId", "test"))))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        var agreement = eclipse.getPublisherAgreement(user);
        assertThat(agreement).isNull();
    }

    @Test
    public void testGetPublisherAgreementNotAuthenticated() throws Exception {
        var user = mockUser();

        var agreement = eclipse.getPublisherAgreement(user);

        assertThat(agreement).isNull();
    }

    @Test
    public void testSignPublisherAgreement() throws Exception {
        var user = mockUser();
        Mockito.when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
            .thenReturn(mockAgreementResponse());
        Mockito.when(repositories.findAccessTokens(user))
            .thenReturn(Streamable.empty());

        eclipse.signPublisherAgreement(user);

        assertThat(user.getEclipseData()).isNotNull();
        var ed = user.getEclipseData();
        assertThat(ed.personId).isEqualTo("test");
        assertThat(ed.publisherAgreement).isNotNull();
        assertThat(ed.publisherAgreement.isActive).isTrue();
        assertThat(ed.publisherAgreement.documentId).isEqualTo("abcd");
        assertThat(ed.publisherAgreement.version).isEqualTo("1");
        assertThat(ed.publisherAgreement.timestamp).isNotNull();
        assertThat(ed.publisherAgreement.timestamp.toString()).isEqualTo("2020-10-09T05:10:32");
    }

    @Test
    public void testSignPublisherAgreementReactivateExtension() throws Exception {
        var user = mockUser();
        Mockito.when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
            .thenReturn(mockAgreementResponse());
        var accessToken = new PersonalAccessToken();
        accessToken.setUser(user);
        accessToken.setActive(true);
        Mockito.when(repositories.findAccessTokens(user))
            .thenReturn(Streamable.of(accessToken));
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
        Mockito.when(repositories.findVersionsByAccessToken(accessToken, false))
            .thenReturn(Streamable.of(extVersion));

        eclipse.signPublisherAgreement(user);

        assertThat(user.getEclipseData()).isNotNull();
        assertThat(extVersion.isActive()).isTrue();
        assertThat(extension.isActive()).isTrue();
    }

    @Test
    public void testPublisherAgreementAlreadySigned() throws Exception {
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
    public void testRevokePublisherAgreement() throws Exception {
        var user = mockUser();
        var eclipseData = new EclipseData();
        user.setEclipseData(eclipseData);
        eclipseData.personId = "test";
        eclipseData.publisherAgreement = new EclipseData.PublisherAgreement();
        eclipseData.publisherAgreement.isActive = true;

        eclipse.revokePublisherAgreement(user, null);

        assertThat(user.getEclipseData().publisherAgreement.isActive).isFalse();
    }

    @Test
    public void testRevokePublisherAgreementByAdmin() throws Exception {
        var user = mockUser();
        var eclipseData = new EclipseData();
        user.setEclipseData(eclipseData);
        eclipseData.personId = "test";
        eclipseData.publisherAgreement = new EclipseData.PublisherAgreement();
        eclipseData.publisherAgreement.isActive = true;
        var admin = new UserData();
        admin.setLoginName("admin");
        admin.setEclipseToken(new AuthToken());
        admin.getEclipseToken().accessToken = "67890";
        Mockito.when(tokens.getActiveToken(admin, "eclipse"))
            .thenReturn(admin.getEclipseToken());

        eclipse.revokePublisherAgreement(user, admin);

        assertThat(user.getEclipseData().publisherAgreement.isActive).isFalse();
    }

    private UserData mockUser() {
        var user = new UserData();
        user.setLoginName("test");
        user.setEclipseToken(new AuthToken());
        user.getEclipseToken().accessToken = "12345";
        Mockito.when(tokens.getActiveToken(user, "eclipse"))
            .thenReturn(user.getEclipseToken());
        return user;
    }

    private ResponseEntity<String> mockProfileResponse() throws IOException {
        try (var stream = getClass().getResourceAsStream("profile-response.json")) {
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
        EclipseService eclipseService() {
            return new EclipseService();
        }

        @Bean
        ExtensionService extensionService() {
            return new ExtensionService();
        }

        @Bean
        ExtensionValidator extensionValidator() {
            return new ExtensionValidator();
        }

        @Bean
        StorageUtilService storageUtilService() {
            return new StorageUtilService();
        }

        @Bean
        VersionService versionService() {
            return new VersionService();
        }

        @Bean
        LatestExtensionVersionCacheKeyGenerator latestExtensionVersionCacheKeyGenerator() {
            return new LatestExtensionVersionCacheKeyGenerator();
        }
    }
    
}