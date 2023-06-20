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

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.EclipseData;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.security.TokenService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Component
public class EclipseService {

    public static final DateTimeFormatter CUSTOM_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();
    
    private static final TypeReference<List<String>> TYPE_LIST_STRING = new TypeReference<List<String>>() {};
    private static final TypeReference<List<EclipseProfile>> TYPE_LIST_PROFILE = new TypeReference<List<EclipseProfile>>() {};
    private static final TypeReference<List<PublisherAgreementResponse>> TYPE_LIST_AGREEMENT = new TypeReference<List<PublisherAgreementResponse>>() {};

    protected final Logger logger = LoggerFactory.getLogger(EclipseService.class);

    @Autowired
    TokenService tokens;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    ExtensionService extensions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.eclipse.base-url:}")
    String eclipseApiUrl;

    @Value("${ovsx.eclipse.publisher-agreement.version:}")
    String publisherAgreementVersion;

    @Value("${ovsx.eclipse.publisher-agreement.timezone:}")
    String publisherAgreementTimeZone;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public EclipseService() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final Function<String, LocalDateTime> parseDate = dateString -> {
        try {
            var local = LocalDateTime.parse(dateString, CUSTOM_DATE_TIME);
            if (StringUtils.isEmpty(publisherAgreementTimeZone)) {
                return local;
            }
            return TimeUtil.convertToUTC(local, publisherAgreementTimeZone);
        } catch (DateTimeParseException exc) {
            logger.error("Failed to parse timestamp.", exc);
            return null;
        }
    };

    public boolean isActive() {
        return !StringUtils.isEmpty(publisherAgreementVersion);
    }

    /**
     * Check whether the given user has an active publisher agreement.
     * @throws ErrorResultException if the user has no active agreement
     */
    public void checkPublisherAgreement(UserData user) {
        if (!isActive()) {
            return;
        }
        // Users without authentication provider have been created directly in the DB,
        // so we skip the agreement check in this case.
        if (user.getProvider() == null) {
            return;
        }
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || eclipseData.personId == null) {
            throw new ErrorResultException("You must log in with an Eclipse Foundation account and sign a Publisher Agreement before publishing any extension.");
        }
        var profile = getPublicProfile(eclipseData.personId);
        if (profile.publisherAgreements == null || profile.publisherAgreements.openVsx == null
                || profile.publisherAgreements.openVsx.version == null) {
            throw new ErrorResultException("You must sign a Publisher Agreement with the Eclipse Foundation before publishing any extension.");
        }
        if (!publisherAgreementVersion.equals(profile.publisherAgreements.openVsx.version)) {
            throw new ErrorResultException("Your Publisher Agreement with the Eclipse Foundation is outdated (version "
                    + profile.publisherAgreements.openVsx.version + "). The current version is "
                    + publisherAgreementVersion + ".");
        }
    }

    /**
     * Update the given user data with a profile obtained from Eclipse API.
     */
    @Transactional
    public EclipseData updateUserData(UserData user, EclipseProfile profile) {
        EclipseData eclipseData;
        if (user.getEclipseData() == null) {
            eclipseData = new EclipseData();
        } else {
            // We need to clone and reset the data to ensure that Hibernate will persist the updated state
            eclipseData = user.getEclipseData().clone();
        }

        if (StringUtils.isEmpty(eclipseData.personId)) {
            eclipseData.personId = profile.name;
        }
        if (profile.publisherAgreements != null) {
            if (profile.publisherAgreements.openVsx == null) {
                if (eclipseData.publisherAgreement != null) {
                    eclipseData.publisherAgreement.isActive = false;
                }
            } else if (!StringUtils.isEmpty(profile.publisherAgreements.openVsx.version)) {
                if (eclipseData.publisherAgreement == null) {
                    eclipseData.publisherAgreement = new EclipseData.PublisherAgreement();
                }
                eclipseData.publisherAgreement.isActive = true;
                eclipseData.publisherAgreement.version = profile.publisherAgreements.openVsx.version;
            }
        }

        user.setEclipseData(eclipseData);
        entityManager.merge(user);
        return eclipseData;
    }

    /**
     * Enrich the given JSON user data with Eclipse-specific information.
     */
    public void enrichUserJson(UserJson json, UserData user) {
        if (!isActive()) {
            return;
        }

        json.publisherAgreement = new UserJson.PublisherAgreement();
        var eclipseData = user.getEclipseData();
        if (eclipseData == null) {
            json.publisherAgreement.status = "none";
            return;
        }

        // Update the internal data from the Eclipse profile
        if (eclipseData.personId != null) {
            try {
                var profile = getPublicProfile(eclipseData.personId);
                eclipseData = transactions.execute(status -> updateUserData(user, profile));
            } catch (ErrorResultException | TransactionException exc) {
                // Continue with the information that is currently in the DB
            }
        }

        // Add information on the publisher agreement status
        var agreement = eclipseData.publisherAgreement;
        if (agreement == null || !agreement.isActive || agreement.version == null)
            json.publisherAgreement.status = "none";
        else if (publisherAgreementVersion.equals(agreement.version))
            json.publisherAgreement.status = "signed";
        else
            json.publisherAgreement.status = "outdated";
        if (agreement != null && agreement.timestamp != null)
            json.publisherAgreement.timestamp = TimeUtil.toUTCString(agreement.timestamp);

        // Report user as logged in only if there is a usabe token:
        // we need the token to access the Eclipse REST API
        if (eclipseData.personId != null && tokens.isUsable(user.getEclipseToken())) {
            var eclipseLogin = new UserJson();
            eclipseLogin.provider = "eclipse";
            eclipseLogin.loginName = eclipseData.personId;
            if (json.additionalLogins == null)
                json.additionalLogins = Lists.newArrayList(eclipseLogin);
            else
                json.additionalLogins.add(eclipseLogin);
        }
    }

    /**
     * Get the publicly available user profile.
     */
    public EclipseProfile getPublicProfile(String personId) {
        checkApiUrl();
        var urlTemplate = eclipseApiUrl + "account/profile/{personId}";
        var uriVariables = Map.of("personId", personId);
        var headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var request = new HttpEntity<Void>(headers);

        try {
            var response = restTemplate.exchange(urlTemplate, HttpMethod.GET, request, String.class, uriVariables);
            return parseEclipseProfile(response);
        } catch (RestClientException exc) {
            if (exc instanceof HttpStatusCodeException) {
                var status = ((HttpStatusCodeException) exc).getStatusCode();
                if (status == HttpStatus.NOT_FOUND)
                    throw new ErrorResultException("No Eclipse profile data available for user: " + personId);
            }

            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("Get request failed with URL: " + url, exc);
            throw new ErrorResultException("Request for retrieving user profile failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the user profile available through an access token.
     */
    public EclipseProfile getUserProfile(String accessToken) {
        checkApiUrl();
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "openvsx", "profile");
        var request = new RequestEntity<>(headers, HttpMethod.GET, URI.create(requestUrl));

        try {
            var response = restTemplate.exchange(request, String.class);
            return parseEclipseProfile(response);
        } catch (RestClientException exc) {
            logger.error("Get request failed with URL: " + requestUrl, exc);
            throw new ErrorResultException("Request for retrieving user profile failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private EclipseProfile parseEclipseProfile(ResponseEntity<String> response) {
        var json = response.getBody();
        try {
            if (json.startsWith("[\"")) {
                var error = objectMapper.readValue(json, TYPE_LIST_STRING);
                logger.error("Profile request failed:\n" + json);
                throw new ErrorResultException("Request to the Eclipse Foundation server failed: " + error,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            } else if (json.startsWith("[")) {
                var profileList = objectMapper.readValue(json, TYPE_LIST_PROFILE);
                if (profileList.isEmpty()) {
                    throw new ErrorResultException("No Eclipse user profile available.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return profileList.get(0);
            } else {
                return objectMapper.readValue(json, EclipseProfile.class);
            }
        } catch (JsonProcessingException exc) {
            logger.error("Failed to parse JSON response (" + response.getStatusCode() + "):\n" + json, exc);
            throw new ErrorResultException("Parsing Eclipse user profile failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the publisher agreement of the given user with the user's current access token.
     */
    public EclipseData.PublisherAgreement getPublisherAgreement(UserData user) {
        var eclipseToken = checkEclipseToken(user);
        return getPublisherAgreement(user, eclipseToken.accessToken);
    }

    /**
     * Get the publisher agreement of the given user with the given access token.
     */
    public EclipseData.PublisherAgreement getPublisherAgreement(UserData user, String accessToken) {
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || StringUtils.isEmpty(eclipseData.personId)) {
            return null;
        }
        checkApiUrl();
        var urlTemplate = eclipseApiUrl + "openvsx/publisher_agreement/{personId}";
        var uriVariables = Map.of("personId", eclipseData.personId);
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var request = new HttpEntity<>(headers);

        try {
            var json = restTemplate.exchange(urlTemplate, HttpMethod.GET, request, String.class, uriVariables);
            var response = parseAgreementResponse(json);

            return updateEclipseData(user, ed -> {
                ed.publisherAgreement = response.createEntityData(parseDate);
                return ed.publisherAgreement;
            }, ed -> {
                ed.personId = response.personID;
            });
        } catch (RestClientException exc) {
            if (exc instanceof HttpStatusCodeException) {
                var status = ((HttpStatusCodeException) exc).getStatusCode();
                // The endpoint yields 404 if the specified user has not signed a publisher agreement
                if (status == HttpStatus.NOT_FOUND)
                    return null;
            }

            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("Get request failed with URL: " + url, exc);
            throw new ErrorResultException("Request for retrieving publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static final Pattern STATUS_400_MESSAGE = Pattern.compile("400 Bad Request: \\[\\[\"(?<message>[^\"]+)\"\\]\\]");

    /**
     * Sign the publisher agreement on behalf of the given user.
     */
    public EclipseData.PublisherAgreement signPublisherAgreement(UserData user) {
        checkApiUrl();
        var eclipseToken = checkEclipseToken(user);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(eclipseToken.accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var data = new SignAgreementParam(publisherAgreementVersion, user.getLoginName());
        var request = new HttpEntity<>(data, headers);
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "openvsx", "publisher_agreement");

        try {
            var json = restTemplate.postForEntity(requestUrl, request, String.class);

            // The request was successful: reactivate all previously published extensions
            extensions.reactivateExtensions(user);
            
            // Parse the response and store the publisher agreement metadata
            var response = parseAgreementResponse(json);
            var result = updateEclipseData(user, ed -> {
                ed.publisherAgreement = response.createEntityData(parseDate);
                return ed.publisherAgreement;
            }, ed -> {
                ed.personId = response.personID;
            });
            return result;

        } catch (RestClientException exc) {
            String message = exc.getMessage();
            var statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
            if (exc instanceof HttpStatusCodeException) {
                var excStatus = ((HttpStatusCodeException) exc).getStatusCode();
                // The endpoint yields 409 if the specified user has already signed a publisher agreement
                if (excStatus == HttpStatus.CONFLICT) {
                    message = "A publisher agreement is already present for user " + user.getLoginName() + ".";
                    statusCode = HttpStatus.BAD_REQUEST;
                } else if (excStatus == HttpStatus.BAD_REQUEST) {
                    var matcher = STATUS_400_MESSAGE.matcher(exc.getMessage());
                    if (matcher.matches()) {
                        message = matcher.group("message");
                    }
                }
            }
            if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR) {
                message = "Request for signing publisher agreement failed: " + message;
            }

            String payload;
            try {
                payload = objectMapper.writeValueAsString(data);
            } catch (JsonProcessingException exc2) {
                payload = "<" + exc2.getMessage() + ">";
            }
            logger.error("Post request failed with URL: " + requestUrl + " Payload: " + payload, exc);
            throw new ErrorResultException(message, statusCode);
        }
    }

    private PublisherAgreementResponse parseAgreementResponse(ResponseEntity<String> response) {
        var json = response.getBody();
        try {
            if (json.startsWith("[\"")) {
                var error = objectMapper.readValue(json, TYPE_LIST_STRING);
                logger.error("Publisher agreement request failed:\n" + json);
                throw new ErrorResultException("Request to the Eclipse Foundation server failed: " + error,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            } else if (json.startsWith("[")) {
                var profileList = objectMapper.readValue(json, TYPE_LIST_AGREEMENT);
                if (profileList.isEmpty()) {
                    throw new ErrorResultException("No publisher agreement available.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return profileList.get(0);
            } else {
                return objectMapper.readValue(json, PublisherAgreementResponse.class);
            }
        } catch (JsonProcessingException exc) {
            logger.error("Failed to parse JSON response (" + response.getStatusCode() + "):\n" + json, exc);
            throw new ErrorResultException("Parsing publisher agreement response failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Revoke the given user's publisher agreement. If an admin user is given,
     * the admin's access token is used for the Eclipse API request, otherwise
     * the access token of the target user is used.
     */
    public void revokePublisherAgreement(UserData user, UserData admin) {
        checkApiUrl();
        AuthToken eclipseToken;
        if (admin == null)
            eclipseToken = checkEclipseToken(user);
        else
            eclipseToken = checkEclipseToken(admin);
        var headers = new HttpHeaders();
        headers.setBearerAuth(eclipseToken.accessToken);
        var request = new HttpEntity<>(headers);
        var eclipseData = checkEclipseData(user);
        var urlTemplate = eclipseApiUrl + "openvsx/publisher_agreement/{personId}";
        var uriVariables = Map.of("personId", eclipseData.personId);

        try {
            var requestCallback = restTemplate.httpEntityCallback(request);
            restTemplate.execute(urlTemplate, HttpMethod.DELETE, requestCallback, null, uriVariables);

            if (eclipseData.publisherAgreement != null) {
                updateEclipseData(user, ed -> {
                    ed.publisherAgreement.isActive = false;
                    return null;
                }, NOP_INIT);
            }
        } catch (RestClientException exc) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("Delete request failed with URL: " + url, exc);
            throw new ErrorResultException("Request for revoking publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkApiUrl() {
        if (StringUtils.isEmpty(eclipseApiUrl)) {
            throw new ErrorResultException("Missing URL for Eclipse API.");
        }
    }

    private AuthToken checkEclipseToken(UserData user) {
        var eclipseToken = tokens.getActiveToken(user, "eclipse");
        if (eclipseToken == null || StringUtils.isEmpty(eclipseToken.accessToken)) {
            throw new ErrorResultException("Authorization by Eclipse required.", HttpStatus.FORBIDDEN);
        }
        return eclipseToken;
    }

    private EclipseData checkEclipseData(UserData user) {
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || StringUtils.isEmpty(eclipseData.personId)) {
            throw new ErrorResultException("Eclipse person ID is unavailable for user: "
                    + user.getProvider() + "/" + user.getLoginName());
        }
        return eclipseData;
    }

    private static final Consumer<EclipseData> NOP_INIT = ed -> {};

    /**
     * Update the Eclipse data of the given user and commit the change to the database.
     */
    protected <T> T updateEclipseData(UserData user, Function<EclipseData, T> update, Consumer<EclipseData> initialize) {
        return transactions.execute(status -> {
            EclipseData eclipseData;
            if (user.getEclipseData() == null) {
                eclipseData = new EclipseData();
                initialize.accept(eclipseData);
            } else {
                // We need to clone and reset the data to ensure that Hibernate will persist the updated state
                eclipseData = user.getEclipseData().clone();
            }
            var result = update.apply(eclipseData);
            user.setEclipseData(eclipseData);
            entityManager.merge(user);
            return result;
        });
    }

}