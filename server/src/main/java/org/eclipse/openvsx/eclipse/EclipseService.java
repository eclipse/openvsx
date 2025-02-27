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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class EclipseService {

    private static final String VAR_PERSON_ID = "personId";

    public static final DateTimeFormatter CUSTOM_DATE_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(DateTimeFormatter.ISO_LOCAL_TIME)
            .toFormatter();
    
    private static final TypeReference<List<String>> TYPE_LIST_STRING = new TypeReference<>() {};
    private static final TypeReference<List<EclipseProfile>> TYPE_LIST_PROFILE = new TypeReference<>() {};
    private static final TypeReference<List<PublisherAgreementResponse>> TYPE_LIST_AGREEMENT = new TypeReference<>() {};

    protected final Logger logger = LoggerFactory.getLogger(EclipseService.class);

    private final TokenService tokens;
    private final ExtensionService extensions;
    private final EntityManager entityManager;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ovsx.eclipse.base-url:}")
    String eclipseApiUrl;

    @Value("${ovsx.eclipse.publisher-agreement.version:}")
    String publisherAgreementVersion;

    public EclipseService(
            TokenService tokens,
            ExtensionService extensions,
            EntityManager entityManager,
            RestTemplate restTemplate
    ) {
        this.tokens = tokens;
        this.extensions = extensions;
        this.entityManager = entityManager;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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
        var personId = user.getEclipsePersonId();
        if (personId == null) {
            throw new ErrorResultException("You must log in with an Eclipse Foundation account and sign a Publisher Agreement before publishing any extension.");
        }
        var profile = getPublicProfile(personId);
        if (profile.getPublisherAgreements() == null || profile.getPublisherAgreements().getOpenVsx() == null
                || profile.getPublisherAgreements().getOpenVsx().getVersion() == null) {
            throw new ErrorResultException("You must sign a Publisher Agreement with the Eclipse Foundation before publishing any extension.");
        }
        if (!publisherAgreementVersion.equals(profile.getPublisherAgreements().getOpenVsx().getVersion())) {
            throw new ErrorResultException("Your Publisher Agreement with the Eclipse Foundation is outdated (version "
                    + profile.getPublisherAgreements().getOpenVsx().getVersion() + "). The current version is "
                    + publisherAgreementVersion + ".");
        }
    }

    /**
     * Get the publicly available user profile.
     */
    public EclipseProfile getPublicProfile(String personId) {
        checkApiUrl();
        var urlTemplate = eclipseApiUrl + "account/profile/{personId}";
        var uriVariables = Map.of(VAR_PERSON_ID, personId);
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
     * Update the given user data with a profile obtained from Eclipse API.
     */
    @Transactional
    public void updateUserData(UserData user, EclipseProfile profile) {
        user = entityManager.merge(user);
        user.setEclipsePersonId(profile.getName());
    }

    /**
     * Enrich the given JSON user data with Eclipse-specific information.
     */
    public void enrichUserJson(UserJson json, UserData user) {
        if (!isActive()) {
            return;
        }

        var publisherAgreement = new UserJson.PublisherAgreement();
        publisherAgreement.setStatus("none");
        json.setPublisherAgreement(publisherAgreement);

        var personId = user.getEclipsePersonId();
        if (personId == null) {
            return;
        }

        var usableToken = true;
        try {
            // Add information on the publisher agreement
            var agreement = getPublisherAgreement(user);
            if(agreement != null && agreement.isActive() && agreement.version() != null) {
                var status = publisherAgreementVersion.equals(agreement.version()) ? "signed" : "outdated";
                publisherAgreement.setStatus(status);
            }
            if (agreement != null && agreement.timestamp() != null) {
                publisherAgreement.setTimestamp(TimeUtil.toUTCString(agreement.timestamp()));
            }
        } catch (ErrorResultException e) {
            if(e.getStatus() == HttpStatus.FORBIDDEN) {
                usableToken = false;
            } else {
                logger.info("Failed to enrich UserJson", e);
            }
        }

        // Report user as logged in only if there is a usable token:
        // we need the token to access the Eclipse REST API
        if(usableToken) {
            var eclipseLogin = new UserJson();
            eclipseLogin.setProvider("eclipse");
            eclipseLogin.setLoginName(personId);
            if (json.getAdditionalLogins() == null)
                json.setAdditionalLogins(Lists.newArrayList(eclipseLogin));
            else
                json.getAdditionalLogins().add(eclipseLogin);
        }
    }

    public void adminEnrichUserJson(UserJson json, UserData user) {
        if (!isActive()) {
            return;
        }

        var publisherAgreement = new UserJson.PublisherAgreement();
        var personId = user.getEclipsePersonId();
        if (personId == null) {
            publisherAgreement.setStatus("none");
            return;
        }

        try {
            var profile = getPublicProfile(personId);
            if (profile.getPublisherAgreements() == null || profile.getPublisherAgreements().getOpenVsx() == null || StringUtils.isEmpty(profile.getPublisherAgreements().getOpenVsx().getVersion()))
                publisherAgreement.setStatus("none");
            else if (publisherAgreementVersion.equals(profile.getPublisherAgreements().getOpenVsx().getVersion()))
                publisherAgreement.setStatus("signed");
            else
                publisherAgreement.setStatus("outdated");

            json.setPublisherAgreement(publisherAgreement);
        } catch (ErrorResultException e) {
            logger.error("Failed to get public profile", e);
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
        if(json == null) {
            return new EclipseProfile();
        }

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
    public PublisherAgreement getPublisherAgreement(UserData user) {
        var eclipseToken = checkEclipseToken(user);
        var personId = user.getEclipsePersonId();
        if (StringUtils.isEmpty(personId)) {
            return null;
        }
        checkApiUrl();
        var urlTemplate = eclipseApiUrl + "openvsx/publisher_agreement/{personId}";
        var uriVariables = Map.of(VAR_PERSON_ID, personId);
        var headers = new HttpHeaders();
        headers.setBearerAuth(eclipseToken.accessToken());
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var request = new HttpEntity<>(headers);

        try {
            var json = restTemplate.exchange(urlTemplate, HttpMethod.GET, request, String.class, uriVariables);
            return parseAgreementResponse(json);
        } catch (RestClientException exc) {
            HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
            if (exc instanceof HttpStatusCodeException) {
                status = ((HttpStatusCodeException) exc).getStatusCode();
                // The endpoint yields 404 if the specified user has not signed a publisher agreement
                if (status == HttpStatus.NOT_FOUND)
                    return null;
            }

            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("Get request failed with URL: " + url, exc);
            throw new ErrorResultException("Request for retrieving publisher agreement failed: " + exc.getMessage(),
                    status);
        }
    }

    private static final Pattern STATUS_400_MESSAGE = Pattern.compile("400 Bad Request: \\[\\[\"(?<message>[^\"]+)\"\\]\\]");

    /**
     * Sign the publisher agreement on behalf of the given user.
     */
    public PublisherAgreement signPublisherAgreement(UserData user) {
        checkApiUrl();
        var eclipseToken = checkEclipseToken(user);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(eclipseToken.accessToken());
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var data = new SignAgreementParam(publisherAgreementVersion, user.getLoginName());
        var request = new HttpEntity<>(data, headers);
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "openvsx", "publisher_agreement");

        try {
            var json = restTemplate.postForEntity(requestUrl, request, String.class);

            // The request was successful: reactivate all previously published extensions
            extensions.reactivateExtensions(user);
            
            // Parse the response and store the publisher agreement metadata
            return parseAgreementResponse(json);
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

    private PublisherAgreement parseAgreementResponse(ResponseEntity<String> response) {
        var json = response.getBody();
        if(json == null) {
            return null;
        }

        try {
            PublisherAgreementResponse agreementResponse;
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
                agreementResponse = profileList.get(0);
            } else {
                agreementResponse = objectMapper.readValue(json, PublisherAgreementResponse.class);
            }

            var timestamp = parseDate(agreementResponse.effectiveDate);
            return new PublisherAgreement(
                    TimeUtil.getCurrentUTC().isAfter(timestamp),
                    agreementResponse.documentID,
                    agreementResponse.version,
                    timestamp
            );
        } catch (JsonProcessingException exc) {
            logger.error("Failed to parse JSON response (" + response.getStatusCode() + "):\n" + json, exc);
            throw new ErrorResultException("Parsing publisher agreement response failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private LocalDateTime parseDate(String dateString) {
        try {
            return LocalDateTime.parse(dateString, CUSTOM_DATE_TIME);
        } catch (DateTimeParseException exc) {
            logger.error("Failed to parse timestamp.", exc);
            return null;
        }
    }

    /**
     * Revoke the given user's publisher agreement. If an admin user is given,
     * the admin's access token is used for the Eclipse API request, otherwise
     * the access token of the target user is used.
     */
    public void revokePublisherAgreement(UserData user, UserData admin) {
        checkApiUrl();
        checkEclipseData(user);

        var eclipseToken = admin == null ? checkEclipseToken(user) : checkEclipseToken(admin);
        var headers = new HttpHeaders();
        headers.setBearerAuth(eclipseToken.accessToken());
        var request = new HttpEntity<>(headers);
        var urlTemplate = eclipseApiUrl + "openvsx/publisher_agreement/{personId}";
        var uriVariables = Map.of(VAR_PERSON_ID, user.getEclipsePersonId());

        try {
            var requestCallback = restTemplate.httpEntityCallback(request);
            restTemplate.execute(urlTemplate, HttpMethod.DELETE, requestCallback, null, uriVariables);
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
        var eclipseToken = tokens.getActiveEclipseToken(user);
        if (eclipseToken == null || StringUtils.isEmpty(eclipseToken.accessToken())) {
            throw new ErrorResultException("Authorization by Eclipse required.", HttpStatus.FORBIDDEN);
        }
        return eclipseToken;
    }

    private void checkEclipseData(UserData user) {
        if (StringUtils.isEmpty(user.getEclipsePersonId())) {
            throw new ErrorResultException("Eclipse person ID is unavailable for user: "
                    + user.getProvider() + "/" + user.getLoginName());
        }
    }
}