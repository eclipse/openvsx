/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.cache;

import static org.eclipse.openvsx.cache.CacheService.CACHE_EXTENSION_JSON;
import static org.eclipse.openvsx.entities.FileResource.DOWNLOAD;
import static org.eclipse.openvsx.entities.FileResource.STORAGE_LOCAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.LocalRegistryService;
import org.eclipse.openvsx.UserService;
import org.eclipse.openvsx.admin.AdminService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionReview;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.AuthUserFactory;
import org.eclipse.openvsx.security.IdPrincipal;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CacheServiceTest {

    @Autowired
    CacheManager cache;

    @Autowired
    UserService users;

    @Autowired
    AdminService admins;

    @Autowired
    ExtensionService extensions;

    @Autowired
    LocalRegistryService registry;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    AuthUserFactory authUserFactory;

    @Test
    @Transactional
    void testGetExtension() throws IOException {
        setRequest();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            var json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
            var cachedJson = cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class);
            assertEquals(json, cachedJson);
        }
    }

    @Test
    @Transactional
    void testUpdateExistingUser() throws IOException {
        setRequest();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());

            var authority = "github";
            var authorities = List.of((GrantedAuthority) () -> authority);

            var loginName = "amvanbaren";
            var fullName = "Aart van Baren";
            var htmlUrl = "https://amvanbaren.github.io";
            var avatarUrl = "https://amvanbaren.github.io/avatar.png";
            var attributes = new HashMap<String, Object>();
            attributes.put("login", loginName);
            attributes.put("name", fullName);
            attributes.put("email", "amvanbaren@hotmail.com");
            attributes.put("html_url", htmlUrl);
            attributes.put("avatar_url", avatarUrl);

            var user = extVersion.getPublishedWith().getUser();
            var oauthUser = new DefaultOAuth2User(authorities, attributes, "name");
            var authUser = authUserFactory.createAuthUser(authority, oauthUser);
            users.updateExistingUser(user, authUser);
            assertNull(cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class));

            var json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
            assertEquals(loginName, json.getPublishedBy().getLoginName());
            assertEquals(fullName, json.getPublishedBy().getFullName());
            assertEquals(htmlUrl, json.getPublishedBy().getHomepage());
            assertEquals(authority, json.getPublishedBy().getProvider());
            assertEquals(avatarUrl, json.getPublishedBy().getAvatarUrl());

            var cachedJson = cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class);
            assertEquals(json, cachedJson);
        }
    }

    @Test
    @Transactional
    void testPostReview() throws IOException {
        setRequest();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            var json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
            assertEquals(Long.valueOf(0), json.getReviewCount());
            assertNull(json.getAverageRating());

            var poster = new UserData();
            poster.setLoginName("user1");
            entityManager.persist(poster);
            setLoggedInUser(poster);

            var review = new ReviewJson();
            review.setRating(3);
            review.setComment("Somewhat ok");
            review.setTimestamp("2000-01-01T10:00Z");

            registry.postReview(review, namespace.getName(), extension.getName());
            assertNull(cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class));

            json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
            assertEquals(Long.valueOf(1), json.getReviewCount());
            assertEquals(Double.valueOf(3), json.getAverageRating());

            var cachedJson = cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class);
            assertEquals(json, cachedJson);
        }
    }

    @Test
    @Transactional
    void testDeleteReview() throws IOException {
        setRequest();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            var poster = new UserData();
            poster.setLoginName("user1");
            entityManager.persist(poster);
            setLoggedInUser(poster);

            var review = new ReviewJson();
            review.setRating(3);
            review.setComment("Somewhat ok");
            review.setTimestamp("2000-01-01T10:00Z");

            registry.postReview(review, namespace.getName(), extension.getName());
            var json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
            assertEquals(Long.valueOf(1), json.getReviewCount());
            assertEquals(Double.valueOf(3), json.getAverageRating());

            registry.deleteReview(namespace.getName(), extension.getName());
            assertNull(cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class));

            json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
            assertEquals(Long.valueOf(0), json.getReviewCount());
            assertNull(json.getAverageRating());

            var cachedJson = cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class);
            assertEquals(json, cachedJson);
        }
    }

    @Test
    @Transactional
    void testDeleteExtension() throws IOException {
        setRequest();
        var admin = insertAdmin();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());

            admins.deleteExtension(namespace.getName(), extension.getName(), admin);
            assertNull(cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class));
        }
    }

    @Test
    @Transactional
    void testDeleteExtensionVersion() throws IOException {
        setRequest();
        var admin = insertAdmin();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            var newVersion = "0.2.0";
            var oldVersion = extVersion.getVersion();
            try (var newTempFile = insertNewVersion(extension, extVersion.getPublishedWith(), newVersion)) {

                var json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), newVersion);
                assertTrue(json.getAllVersions().containsKey(newVersion));
                assertTrue(json.getAllVersions().containsKey(oldVersion));

                admins.deleteExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), newVersion, admin);
                assertNull(cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class));

                json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
                assertFalse(json.getAllVersions().containsKey(newVersion));
                assertTrue(json.getAllVersions().containsKey(oldVersion));

                var cachedJson = cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class);
                assertEquals(json, cachedJson);
            }
        }
    }

    @Test
    @Transactional
    void testUpdateExtension() throws IOException {
        setRequest();
        try (var tempFile = insertExtensionVersion()) {
            var extVersion = tempFile.getResource().getExtension();
            var extension = extVersion.getExtension();
            var namespace = extension.getNamespace();
            var cacheKey = new ExtensionJsonCacheKeyGenerator().generate(namespace.getName(), extension.getName(),
                    extVersion.getTargetPlatform(), extVersion.getVersion());

            registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());

            var newVersion = "0.2.0";
            var oldVersion = extVersion.getVersion();
            try (var newTempFile = insertNewVersion(extension, extVersion.getPublishedWith(), newVersion)) {
                newTempFile.getResource().getExtension().setPreRelease(true);
                extensions.updateExtension(extension);
                assertNull(cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class));

                var json = registry.getExtension(namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), oldVersion);
                assertTrue(json.getAllVersions().containsKey(oldVersion));
                assertTrue(json.getAllVersions().containsKey(newVersion));
                assertTrue(json.getAllVersions().containsKey("latest"));
                assertTrue(json.getAllVersions().containsKey("pre-release"));

                var cachedJson = cache.getCache(CACHE_EXTENSION_JSON).get(cacheKey, ExtensionJson.class);
                assertEquals(json, cachedJson);
            }
        }
    }

    @Test
    @Transactional
    void testAverageReviewRating() throws IOException {
        var user = insertAdmin();
        try (var tempFile = insertExtensionVersion()) {
            // no reviews in database
            assertEquals(0L, repositories.getAverageReviewRating());

            var extVersion = tempFile.getResource().getExtension();
            var review = new ExtensionReview();
            review.setRating(3);
            review.setActive(true);
            review.setExtension(extVersion.getExtension());
            review.setTimestamp(LocalDateTime.now());
            review.setUser(user);
            entityManager.persist(review);

            // returns cached value
            assertEquals(0L, repositories.getAverageReviewRating());

            cache.getCache(CacheService.CACHE_AVERAGE_REVIEW_RATING).clear();

            // returns new value from database
            assertEquals(3L, repositories.getAverageReviewRating());
        }
    }

    private void setLoggedInUser(UserData user) {
        var principal = new IdPrincipal(user.getId(), user.getLoginName(), List.of((GrantedAuthority) () -> "github"));
        var authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setRequest() {
        // UrlUtil.getBaseUrl needs request
        var request = new MockHttpServletRequest();
        request.setScheme("https");
        request.setServerName("open-vsx.org");
        request.setServerPort(8080);
        request.setContextPath("/openvsx-server");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private UserData insertAdmin() {
        var admin = new UserData();
        admin.setLoginName("super_user");
        admin.setRole("admin");
        entityManager.persist(admin);

        return admin;
    }

    private TempFile insertNewVersion(Extension extension, PersonalAccessToken token, String newVersion) throws IOException {
        var extVersion = new ExtensionVersion();
        extVersion.setPreview(false);
        extVersion.setActive(true);
        extVersion.setVersion(newVersion);
        extVersion.setTargetPlatform("universal");
        extVersion.setDisplayName("baz");
        extVersion.setDescription("foo.bar baz");
        extVersion.setTimestamp(TimeUtil.getCurrentUTC());
        extVersion.setCategories(Collections.emptyList());
        extVersion.setTags(Collections.emptyList());
        extVersion.setExtension(extension);
        extVersion.setPublishedWith(token);
        entityManager.persist(extVersion);

        // populate extension versions list
        entityManager.flush();
        entityManager.refresh(extension);

        var download = new FileResource();
        download.setExtension(extVersion);
        download.setName("foo.bar-" + newVersion + ".vsix");
        download.setType(DOWNLOAD);
        download.setStorageType(STORAGE_LOCAL);
        entityManager.persist(download);

        var tempFile = new TempFile("foo.bar-" + newVersion, ".vsix");
        tempFile.setResource(download);
        Files.writeString(tempFile.getPath(), "VSIX Package");
        return tempFile;
    }

    private TempFile insertExtensionVersion() throws IOException {
        var version = "0.1.0";
        var namespace = new Namespace();
        namespace.setName("foo");
        namespace.setPublicId("12823789-189273189-1721983");
        entityManager.persist(namespace);

        var extension = new Extension();
        extension.setActive(true);
        extension.setName("bar");
        extension.setDownloadCount(0);
        extension.setNamespace(namespace);
        entityManager.persist(extension);

        var user = new UserData();
        user.setLoginName("user");
        user.setFullName("User");
        user.setAvatarUrl("https://github.com/user/avatar");
        user.setProviderUrl("https://github.com");
        user.setProvider("github");
        entityManager.persist(user);

        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue("lkasdjfdklas-daskjfdaksl-kasdljfaksl");
        token.setActive(true);
        token.setDescription("test token");
        token.setCreatedTimestamp(LocalDateTime.now());
        token.setAccessedTimestamp(LocalDateTime.now());
        entityManager.persist(token);

        var extVersion = new ExtensionVersion();
        extVersion.setPreview(false);
        extVersion.setActive(true);
        extVersion.setVersion(version);
        extVersion.setTargetPlatform("universal");
        extVersion.setDisplayName("baz");
        extVersion.setDescription("foo.bar baz");
        extVersion.setTimestamp(TimeUtil.getCurrentUTC());
        extVersion.setCategories(Collections.emptyList());
        extVersion.setTags(Collections.emptyList());
        extVersion.setExtension(extension);
        extVersion.setPublishedWith(token);
        entityManager.persist(extVersion);

        // populate extension versions list
        entityManager.flush();
        entityManager.refresh(extension);

        var download = new FileResource();
        download.setExtension(extVersion);
        download.setName("foo.bar-" + version + ".vsix");
        download.setType(DOWNLOAD);
        download.setStorageType(STORAGE_LOCAL);
        entityManager.persist(download);

        var tempFile = new TempFile("foo.bar-" + version, ".vsix");
        tempFile.setResource(download);
        Files.writeString(tempFile.getPath(), "VSIX Package");
        return tempFile;
    }
}
