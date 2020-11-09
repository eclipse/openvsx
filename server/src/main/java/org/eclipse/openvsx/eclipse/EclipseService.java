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
import java.util.function.Consumer;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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

    protected final Logger logger = LoggerFactory.getLogger(EclipseService.class);

    @Autowired
    TokenService tokens;

    @Autowired
    TransactionTemplate transactions;

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
            if (Strings.isNullOrEmpty(publisherAgreementTimeZone)) {
                return local;
            }
            return TimeUtil.convertToUTC(local, publisherAgreementTimeZone);
        } catch (DateTimeParseException exc) {
            logger.error("Failed to parse timestamp.", exc);
            return null;
        }
    };

    public boolean isActive() {
        return !Strings.isNullOrEmpty(publisherAgreementVersion);
    }

    public void checkPublisherAgreement(UserData user) {
        if (!isActive()) {
            return;
        }
        // Users without authentication provider have been created directly in the DB,
        // so we skip the agreement check in this case.
        if (user.getProvider() == null) {
            return;
        }
        var agreement = getPublisherAgreement(user);
        if (agreement == null || agreement.version == null) {
            throw new ErrorResultException("You must sign a Publisher Agreement with the Eclipse Foundation before publishing any extension.");
        }
        if (!agreement.isActive) {
            throw new ErrorResultException("Your Publisher Agreement with the Eclipse Foundation is currently inactive.");
        }
        if (!publisherAgreementVersion.equals(agreement.version)) {
            throw new ErrorResultException("Your Publisher Agreement with the Eclipse Foundation is outdated (version "
                    + agreement.version + "). The current version is " + publisherAgreementVersion + ".");
        }
    }

    @Transactional
    public void updateUserData(UserData user, EclipseProfile profile) {
        EclipseData eclipseData;
        if (user.getEclipseData() == null) {
            eclipseData = new EclipseData();
        } else {
            // We need to clone and reset the data to ensure that Hibernate will persist the updated state
            eclipseData = user.getEclipseData().clone();
        }
        eclipseData.personId = profile.name;
        user.setEclipseData(eclipseData);
        entityManager.merge(user);
    }

    /**
     * Enrich the given JSON user data with Eclipse-specific information.
     */
    public void enrichUserJson(UserJson json, UserData user) {
        if (!isActive()) {
            return;
        }
        var eclipseData = user.getEclipseData();
        if (eclipseData == null) {
            json.publisherAgreement = "none";
            return;
        }

        var agreement = eclipseData.publisherAgreement;
        if (agreement == null || !agreement.isActive || agreement.version == null)
            json.publisherAgreement = "none";
        else if (publisherAgreementVersion.equals(agreement.version))
            json.publisherAgreement = "signed";
        else
            json.publisherAgreement = "outdated";
        if (agreement != null && agreement.timestamp != null)
            json.publisherAgreementTimestamp = TimeUtil.toUTCString(agreement.timestamp);

        // Report user as logged in only if there is a usabe token:
        // we need the token to access the Eclipse REST API
        if (eclipseData.personId != null && user.getEclipseToken() != null && user.getEclipseToken().refreshToken != null) {
            var eclipseLogin = new UserJson();
            eclipseLogin.provider = "eclipse";
            eclipseLogin.loginName = eclipseData.personId;
            if (json.additionalLogins == null)
                json.additionalLogins = Lists.newArrayList(eclipseLogin);
            else
                json.additionalLogins.add(eclipseLogin);
        }
    }

    public EclipseProfile getUserProfile(String accessToken) {
        checkApiUrl();
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "profile");
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
            if (json.startsWith("[")) {
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

    public EclipseData.PublisherAgreement getPublisherAgreement(UserData user) {
        var eclipseToken = checkEclipseToken(user);
        return getPublisherAgreement(user, eclipseToken.accessToken);
    }

    public EclipseData.PublisherAgreement getPublisherAgreement(UserData user, String accessToken) {
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || Strings.isNullOrEmpty(eclipseData.personId)) {
            return null;
        }
        checkApiUrl();
        var headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "publisher_agreement", eclipseData.personId);
        var request = new RequestEntity<>(headers, HttpMethod.GET, URI.create(requestUrl));

        try {
            var json = restTemplate.exchange(request, String.class);
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
            logger.error("Get request failed with URL: " + requestUrl, exc);
            throw new ErrorResultException("Request for retrieving publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public EclipseData.PublisherAgreement signPublisherAgreement(UserData user) {
        checkApiUrl();
        var eclipseToken = checkEclipseToken(user);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(eclipseToken.accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var data = new SignAgreementParam(publisherAgreementVersion);
        var request = new HttpEntity<>(data, headers);
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "publisher_agreement");

        try {
            var json = restTemplate.postForEntity(requestUrl, request, String.class);
            var response = parseAgreementResponse(json);

            return updateEclipseData(user, ed -> {
                ed.publisherAgreement = response.createEntityData(parseDate);
                return ed.publisherAgreement;
            }, ed -> {
                ed.personId = response.personID;
            });
        } catch (RestClientException exc) {
            logger.error("Post request failed with URL: " + requestUrl, exc);
            throw new ErrorResultException("Request for signing publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    private PublisherAgreementResponse parseAgreementResponse(ResponseEntity<String> response) {
        var json = response.getBody();
        try {
            if (json.startsWith("[")) {
                var error = objectMapper.readValue(json, TYPE_LIST_STRING);
                logger.error("Publisher agreement request failed:\n" + json);
                throw new ErrorResultException("Request to the Eclipse Foundation server failed: " + error,
                        HttpStatus.INTERNAL_SERVER_ERROR);
            } else {
                return objectMapper.readValue(json, PublisherAgreementResponse.class);
            }
        } catch (JsonProcessingException exc) {
            logger.error("Failed to parse JSON response (" + response.getStatusCode() + "):\n" + json, exc);
            throw new ErrorResultException("Parsing publisher agreement response failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
		}
    }

    public void revokePublisherAgreement(UserData user) {
        checkApiUrl();
        var eclipseToken = checkEclipseToken(user);
        var headers = new HttpHeaders();
        headers.setBearerAuth(eclipseToken.accessToken);
        var request = new HttpEntity<Void>(headers);
        var eclipseData = checkEclipseData(user);
        var requestUrl = UrlUtil.createApiUrl(eclipseApiUrl, "publisher_agreement", eclipseData.personId);

        try {
            var requestCallback = restTemplate.httpEntityCallback(request);
            restTemplate.execute(requestUrl, HttpMethod.DELETE, requestCallback, null);

            if (eclipseData.publisherAgreement != null) {
                updateEclipseData(user, ed -> {
                    ed.publisherAgreement.isActive = false;
                    return null;
                }, NOP_INIT);
            }
        } catch (RestClientException exc) {
            logger.error("Delete request failed with URL: " + requestUrl, exc);
            throw new ErrorResultException("Request for revoking publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkApiUrl() {
        if (Strings.isNullOrEmpty(eclipseApiUrl)) {
            throw new ErrorResultException("Missing URL for Eclipse API.");
        }
    }

    private AuthToken checkEclipseToken(UserData user) {
        var eclipseToken = tokens.getActiveToken(user, "eclipse");
        if (eclipseToken == null || Strings.isNullOrEmpty(eclipseToken.accessToken)) {
            throw new ErrorResultException("Authorization by Eclipse required.", HttpStatus.FORBIDDEN);
        }
        return eclipseToken;
    }

    private EclipseData checkEclipseData(UserData user) {
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || Strings.isNullOrEmpty(eclipseData.personId)) {
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