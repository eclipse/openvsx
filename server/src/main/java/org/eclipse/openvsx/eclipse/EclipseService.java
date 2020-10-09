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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.eclipse.openvsx.entities.EclipseData;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
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
    TransactionTemplate transactions;

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.eclipse.publisher-agreement.version:}")
    String publisherAgreementVersion;

    @Value("${ovsx.eclipse.publisher-agreement.api:}")
    String publisherAgreementApiUrl;

    @Value("${ovsx.eclipse.publisher-agreement.timezone:}")
    String publisherAgreementTimeZone;

    public void addPublisherAgreementInfo(UserJson json, UserData user) {
        if (Strings.isNullOrEmpty(publisherAgreementVersion)) {
            return;
        }
        var eclipseData = user.getEclipseData();
        if (eclipseData == null || eclipseData.publisherAgreement == null) {
            json.publisherAgreement = "none";
        } else {
            var agreement = eclipseData.publisherAgreement;
            if (!agreement.isActive || agreement.version == null)
                json.publisherAgreement = "none";
            else if (publisherAgreementVersion.equals(agreement.version))
                json.publisherAgreement = "signed";
            else
                json.publisherAgreement = "outdated";
            if (agreement.timestamp != null)
                json.publisherAgreementTimestamp = TimeUtil.toUTCString(agreement.timestamp);
            if (eclipseData.personId != null) {
                var eclipseLogin = new UserJson();
                eclipseLogin.provider = "eclipse";
                eclipseLogin.loginName = eclipseData.personId;
                json.additionalLogins = Lists.newArrayList(eclipseLogin);
            }
        }
    }

    public void signPublisherAgreement(UserData user) {
        if (Strings.isNullOrEmpty(publisherAgreementApiUrl)) {
            throw new ErrorResultException("Missing URL for signing the publisher agreement.");
        }
        var eclipseToken = user.getEclipseToken();
        if (eclipseToken == null) {
            throw new ErrorResultException("Authorization by Eclipse required.", HttpStatus.FORBIDDEN);
        }

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(eclipseToken.accessToken);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        var data = new SignAgreementParam(publisherAgreementVersion);
        var request = new HttpEntity<>(data, headers);

        try {
            var response = restTemplate.postForObject(publisherAgreementApiUrl, request, SignAgreementResponse.class);

            var ed = new EclipseData();
            ed.personId = response.personID;
            var pub = ed.new PublisherAgreement();
            pub.isActive = response.sysDocument != null && ("true".equalsIgnoreCase(response.sysDocument.isActive)
                    || "1".equals(response.sysDocument.isActive));
            pub.documentId = response.documentID;
            pub.version = response.version;
            pub.timestamp = parseDate(response.effectiveDate);
            ed.publisherAgreement = pub;
            transactions.execute(status -> {
                user.setEclipseData(ed);
                return null;
            });
        } catch (RestClientException exc) {
            logger.error("Post request failed with URL: " + publisherAgreementApiUrl, exc);
            throw new ErrorResultException("Request for signing publisher agreement failed: " + exc.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected LocalDateTime parseDate(String dateString) {
        var local = LocalDateTime.parse(dateString, CUSTOM_DATE_TIME);
        if (Strings.isNullOrEmpty(publisherAgreementTimeZone)) {
            return local;
        }
        return TimeUtil.convertToUTC(local, publisherAgreementTimeZone);
    }

}