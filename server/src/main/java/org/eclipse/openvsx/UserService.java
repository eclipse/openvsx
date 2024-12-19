/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import com.google.common.base.Joiner;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.entities.NamespaceMembership;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.AccessTokenJson;
import org.eclipse.openvsx.json.NamespaceDetailsJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.security.IdPrincipal;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.eclipse.openvsx.cache.CacheService.CACHE_NAMESPACE_DETAILS_JSON;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

@Component
public class UserService {

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final StorageUtilService storageUtil;
    private final CacheService cache;
    private final ExtensionValidator validator;
    private final ClientRegistrationRepository clientRegistrationRepository;

    public UserService(
            EntityManager entityManager,
            RepositoryService repositories,
            StorageUtilService storageUtil,
            CacheService cache,
            ExtensionValidator validator,
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.storageUtil = storageUtil;
        this.cache = cache;
        this.validator = validator;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public UserData findLoggedInUser() {
        if(!canLogin()) {
            return null;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof IdPrincipal) {
                var principal = (IdPrincipal) authentication.getPrincipal();
                return entityManager.find(UserData.class, principal.getId());
            }
        }
        return null;
    }

    @Transactional
    public UserData registerNewUser(OAuth2User oauth2User) {
        var user = new UserData();
        user.setProvider("github");
        user.setAuthId(oauth2User.getName());
        user.setLoginName(oauth2User.getAttribute("login"));
        user.setFullName(oauth2User.getAttribute("name"));
        user.setEmail(oauth2User.getAttribute("email"));
        user.setProviderUrl(oauth2User.getAttribute("html_url"));
        user.setAvatarUrl(oauth2User.getAttribute("avatar_url"));
        entityManager.persist(user);
        return user;
    }

    @Transactional
    public UserData updateExistingUser(UserData user, OAuth2User oauth2User) {
        if ("github".equals(user.getProvider())) {
            var updated = false;
            String loginName = oauth2User.getAttribute("login");
            if (loginName != null && !loginName.equals(user.getLoginName())) {
                user.setLoginName(loginName);
                updated = true;
            }
            String fullName = oauth2User.getAttribute("name");
            if (fullName != null && !fullName.equals(user.getFullName())) {
                user.setFullName(fullName);
                updated = true;
            }
            String email = oauth2User.getAttribute("email");
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }
            String providerUrl = oauth2User.getAttribute("html_url");
            if (providerUrl != null && !providerUrl.equals(user.getProviderUrl())) {
                user.setProviderUrl(providerUrl);
                updated = true;
            }
            String avatarUrl = oauth2User.getAttribute("avatar_url");
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                updated = true;
            }
            if (updated) {
                cache.evictExtensionJsons(user);
            }
        }
        return user;
    }

    @Transactional
    public PersonalAccessToken useAccessToken(String tokenValue) {
        var token = repositories.findAccessToken(tokenValue);
        if (token == null || !token.isActive()) {
            return null;
        }
        token.setAccessedTimestamp(TimeUtil.getCurrentUTC());
        return token;
    }

    public String generateTokenValue() {
        String value;
        do {
            value = UUID.randomUUID().toString();
        } while (repositories.hasAccessToken(value));
        return value;
    }

    public boolean hasPublishPermission(UserData user, Namespace namespace) {
        if (UserData.ROLE_PRIVILEGED.equals(user.getRole())) {
            // Privileged users can publish to every namespace.
            return true;
        }

        return repositories.canPublishInNamespace(user, namespace);
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson setNamespaceMember(UserData requestingUser, String namespaceName, String provider, String userLogin, String role) {
        var namespace = repositories.findNamespace(namespaceName);
        if (!repositories.isNamespaceOwner(requestingUser, namespace)) {
            throw new ErrorResultException("You must be an owner of this namespace.");
        }
        var targetUser = repositories.findUserByLoginName(provider, userLogin);
        if (targetUser == null) {
            throw new ErrorResultException("User not found: " + provider + "/" + userLogin);
        }

        if (role.equals("remove")) {
            return removeNamespaceMember(namespace, targetUser);
        } else {
            return addNamespaceMember(namespace, targetUser, role);
        }
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson removeNamespaceMember(Namespace namespace, UserData user) throws ErrorResultException {
        var membership = repositories.findMembership(user, namespace);
        if (membership == null) {
            throw new ErrorResultException("User " + user.getLoginName() + " is not a member of " + namespace.getName() + ".");
        }
        entityManager.remove(membership);
        return ResultJson.success("Removed " + user.getLoginName() + " from namespace " + namespace.getName() + ".");
    }

    @Transactional(rollbackOn = ErrorResultException.class)
    public ResultJson addNamespaceMember(Namespace namespace, UserData user, String role) {
        if (!(role.equals(NamespaceMembership.ROLE_OWNER)
                || role.equals(NamespaceMembership.ROLE_CONTRIBUTOR))) {
            throw new ErrorResultException("Invalid role: " + role);
        }
        var membership = repositories.findMembership(user, namespace);
        if (membership != null) {
            if (role.equals(membership.getRole())) {
                throw new ErrorResultException("User " + user.getLoginName() + " already has the role " + role + ".");
            }
            membership.setRole(role);
            return ResultJson.success("Changed role of " + user.getLoginName() + " in " + namespace.getName() + " to " + role + ".");
        }
        membership = new NamespaceMembership();
        membership.setNamespace(namespace);
        membership.setUser(user);
        membership.setRole(role);
        entityManager.persist(membership);
        return ResultJson.success("Added " + user.getLoginName() + " as " + role + " of " + namespace.getName() + ".");
    }

    @Transactional(rollbackOn = { ErrorResultException.class, NotFoundException.class })
    @CacheEvict(value = { CACHE_NAMESPACE_DETAILS_JSON }, key="#details.name")
    public ResultJson updateNamespaceDetails(NamespaceDetailsJson details) {
        var namespace = repositories.findNamespace(details.getName());
        if (namespace == null) {
            throw new NotFoundException();
        }

        var issues = validator.validateNamespaceDetails(details);
        if (!issues.isEmpty()) {
            var message = issues.size() == 1
                    ? issues.get(0).toString()
                    : "Multiple issues were found in the extension metadata:\n" + Joiner.on("\n").join(issues);

            throw new ErrorResultException(message);
        }

        if(!Objects.equals(details.getDisplayName(), namespace.getDisplayName())) {
            namespace.setDisplayName(details.getDisplayName());
        }
        if(!Objects.equals(details.getDescription(), namespace.getDescription())) {
            namespace.setDescription(details.getDescription());
        }
        if(!Objects.equals(details.getWebsite(), namespace.getWebsite())) {
            namespace.setWebsite(details.getWebsite());
        }
        if(!Objects.equals(details.getSupportLink(), namespace.getSupportLink())) {
            namespace.setSupportLink(details.getSupportLink());
        }
        if(!Objects.equals(details.getSocialLinks(), namespace.getSocialLinks())) {
            namespace.setSocialLinks(details.getSocialLinks());
        }
        if(StringUtils.isEmpty(details.getLogo()) && StringUtils.isNotEmpty(namespace.getLogoName())) {
            storageUtil.removeNamespaceLogo(namespace);
            namespace.clearLogoBytes();
            namespace.setLogoName(null);
            namespace.setLogoStorageType(null);
        }

        return ResultJson.success("Updated details for namespace " + details.getName());
    }

    @Transactional
    @CacheEvict(value = { CACHE_NAMESPACE_DETAILS_JSON }, key="#namespaceName")
    public ResultJson updateNamespaceDetailsLogo(String namespaceName, MultipartFile file) {
        var namespace = repositories.findNamespace(namespaceName);
        if (namespace == null) {
            throw new NotFoundException();
        }

        var oldNamespace = SerializationUtils.clone(namespace);
        try (
                var logoFile = new TempFile("namespace-logo", ".png");
                var out = Files.newOutputStream(logoFile.getPath())
        ) {
            var tika = new Tika();
            var detectedType = tika.detect(file.getInputStream(), file.getOriginalFilename());
            var logoType = MimeTypes.getDefaultMimeTypes().getRegisteredMimeType(detectedType);
            var expectedLogoTypes = List.of(MediaType.image("png"), MediaType.image("jpg"));
            if(logoType == null || !expectedLogoTypes.contains(logoType.getType())) {
                throw new ErrorResultException("Namespace logo should be a png or jpg file");
            }

            namespace.setLogoName(NamingUtil.toLogoName(namespace, logoType));
            file.getInputStream().transferTo(out);
            logoFile.setNamespace(namespace);
            storageUtil.uploadNamespaceLogo(logoFile);
            if(StringUtils.isNotEmpty(oldNamespace.getLogoName())) {
                storageUtil.removeNamespaceLogo(oldNamespace);
            }
        } catch (IOException | MimeTypeException e) {
            throw new RuntimeException(e);
        }

        return ResultJson.success("Updated logo for namespace " + namespace.getName());
    }

    @Transactional
    public AccessTokenJson createAccessToken(UserData user, String description) {
        var token = new PersonalAccessToken();
        token.setUser(user);
        token.setValue(generateTokenValue());
        token.setActive(true);
        token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
        token.setDescription(description);
        entityManager.persist(token);
        var json = token.toAccessTokenJson();
        // Include the token value after creation so the user can copy it
        json.setValue(token.getValue());
        json.setDeleteTokenUrl(createApiUrl(UrlUtil.getBaseUrl(), "user", "token", "delete", Long.toString(token.getId())));

        return json;
    }

    @Transactional
    public ResultJson deleteAccessToken(UserData user, long id) {
        var token = repositories.findAccessToken(id);
        if (token == null || !token.isActive()) {
            throw new NotFoundException();
        }

        user = entityManager.merge(user);
        if(!token.getUser().equals(user)) {
            throw new NotFoundException();
        }

        token.setActive(false);
        return ResultJson.success("Deleted access token for user " + user.getLoginName() + ".");
    }

    public boolean canLogin() {
        return clientRegistrationRepository != null && clientRegistrationRepository.findByRegistrationId("github") != null;
    }
}