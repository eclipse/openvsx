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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

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
import org.springframework.security.oauth2.core.user.OAuth2User;
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

    protected final Logger logger = LoggerFactory.getLogger(EclipseService.class);

    @Autowired
    TokenService tokens;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.eclipse.publisher-agreement.version:}")
    String publisherAgreementVersion;

    @Value("${ovsx.eclipse.publisher-agreement.api:}")
    String publisherAgreementApiUrl;

    @Value("${ovsx.eclipse.publisher-agreement.timezone:}")
    String publisherAgreementTimeZone;

    private final Function<String, LocalDateTime> parseDate = dateString -> {
        var local = LocalDateTime.parse(dateString, CUSTOM_DATE_TIME);
        if (Strings.isNullOrEmpty(publisherAgreementTimeZone)) {
            return local;
        }
        return TimeUtil.convertToUTC(local, publisherAgreementTimeZone);
    };

    public void checkPublisherAgreement(UserData user) {
        if (Strings.isNullOrEmpty(publisherAgreementVersion)) {
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
    public void updateUserData(UserData user, OAuth2User oauth2User) {
        var eclipseData = user.getEclipseData();
        if (eclipseData == null) {
            eclipseData = new EclipseData();
            user.setEclipseData(eclipseData);
        }
        eclipseData.personId = oauth2User.getName();
        try {
            getPublisherAgreement(user);
        } catch (ErrorResultException exc) {
            // Ignore the failed request and assume the current DB state is valid
        }
        entityManager.merge(user);
    }

    /**
     * Enrich the given JSON user data with Eclipse-specific information.
     */
    public void addPublisherAgreementInfo(UserJson json, UserData user) {
        if (Strings.isNullOrEmpty(publisherAgreementVersion)) {
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

    public EclipseData.PublisherAgreement getPublisherAgreement(UserData user) {
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || Strings.isNullOrEmpty(eclipseData.personId)) {
            return null;
        }
        checkApiUrl();
        var eclipseToken = checkEclipseToken(user);
        var headers = new HttpHeaders();
        headers.setBearerAuth(eclipseToken.accessToken);
        var request = new HttpEntity<>(headers);
        var requestUrl = UrlUtil.createApiUrl(publisherAgreementApiUrl, eclipseData.personId);

        try {
            var response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, PublisherAgreementResponse.class);

            var agreement = response.getBody().createEntityData(parseDate);
            return transactions.execute(status -> {
                eclipseData.publisherAgreement = agreement;
                entityManager.merge(user);
                return agreement;
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

        try {
            var response = restTemplate.postForObject(publisherAgreementApiUrl, request, PublisherAgreementResponse.class);

            return transactions.execute(status -> {
                var eclipseData = user.getEclipseData();
                if (eclipseData == null) {
                    eclipseData = new EclipseData();
                    eclipseData.personId = response.personID;
                    user.setEclipseData(eclipseData);
                }
                eclipseData.publisherAgreement = response.createEntityData(parseDate);
                entityManager.merge(user);
                return eclipseData.publisherAgreement;
            });
        } catch (RestClientException exc) {
            logger.error("Post request failed with URL: " + publisherAgreementApiUrl, exc);
            throw new ErrorResultException("Request for signing publisher agreement failed: " + exc.getMessage(),
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
        var requestUrl = UrlUtil.createApiUrl(publisherAgreementApiUrl, eclipseData.personId);

        try {
            var requestCallback = restTemplate.httpEntityCallback(request);
            restTemplate.execute(requestUrl, HttpMethod.DELETE, requestCallback, null);

            if (eclipseData.publisherAgreement != null) {
                transactions.<Void>execute(status -> {
                    eclipseData.publisherAgreement.isActive = false;
                    entityManager.merge(user);
                    return null;
                });
            }
        } catch (RestClientException exc) {
            logger.error("Delete request failed with URL: " + requestUrl, exc);
            throw new ErrorResultException("Request for revoking publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void checkApiUrl() {
        if (Strings.isNullOrEmpty(publisherAgreementApiUrl)) {
            throw new ErrorResultException("Missing URL for Eclipse publisher agreement API.");
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

}