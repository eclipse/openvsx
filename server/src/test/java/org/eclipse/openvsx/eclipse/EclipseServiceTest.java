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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.io.IOException;
import java.io.InputStreamReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;

import org.eclipse.openvsx.entities.AuthToken;
import org.eclipse.openvsx.entities.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
public class EclipseServiceTest {

    @MockBean
    TransactionTemplate transactions;

    @MockBean
    RestTemplate restTemplate;

    @Autowired
    EclipseService eclipse;

    @BeforeEach
    public void setup() {
        eclipse.publisherAgreementVersion = "1";
        eclipse.publisherAgreementApiUrl = "https://test.openvsx.eclipse.org/";
        eclipse.publisherAgreementTimeZone = "US/Eastern";
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSignPublisherAgreement() throws Exception {
        var user = new UserData();
        user.setLoginName("test");
        user.setEclipseToken(new AuthToken());
        user.getEclipseToken().accessToken = "12345";

        Mockito.when(restTemplate.postForObject(any(String.class), any(), eq(SignAgreementResponse.class)))
            .thenReturn(mockSignAgreementResponse());
        Mockito.when(transactions.execute(any(TransactionCallback.class)))
            .thenAnswer(invocation -> {
                var action = invocation.getArgument(0, TransactionCallback.class);
                return action.doInTransaction(null);
            });

        eclipse.signPublisherAgreement(user);

        assertThat(user.getEclipseData()).isNotNull();
        var ed = user.getEclipseData();
        assertThat(ed.personId).isEqualTo("test");
        assertThat(ed.publisherAgreement).isNotNull();
        assertThat(ed.publisherAgreement.isActive).isTrue();
        assertThat(ed.publisherAgreement.documentId).isEqualTo("abcd");
        assertThat(ed.publisherAgreement.version).isEqualTo("1");
        assertThat(ed.publisherAgreement.timestamp).isNotNull();
        assertThat(ed.publisherAgreement.timestamp.toString()).isEqualTo("2020-10-09T09:10:32");
    }

    private SignAgreementResponse mockSignAgreementResponse() throws IOException {
        try (
            var stream = getClass().getResourceAsStream("sign-publisher-agreement-response.json");
        ) {
            var json = CharStreams.toString(new InputStreamReader(stream));
            return new ObjectMapper().readValue(json, SignAgreementResponse.class);
        }
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        EclipseService eclipseService() {
            return new EclipseService();
        }
    }
    
}