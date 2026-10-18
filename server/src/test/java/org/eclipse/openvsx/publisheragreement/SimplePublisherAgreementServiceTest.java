/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.publisheragreement;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import org.eclipse.openvsx.entities.PublisherAgreement;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class SimplePublisherAgreementServiceTest {

    private final List<PublisherAgreement> rows = new ArrayList<>();
    private final RepositoryService repositories = Mockito.mock(RepositoryService.class);
    private final UserData user = new UserData();

    SimplePublisherAgreementServiceTest() {
        user.setId(7);
        Mockito.when(repositories.findActivePublisherAgreements(anyLong())).thenAnswer(
                i -> rows.stream().filter(a -> a.getRevokedTimestamp() == null)
                        .sorted((x, y) -> Long.compare(y.getId(), x.getId())).toList());
        Mockito.when(repositories.findPublisherAgreementHistory(anyLong())).thenAnswer(
                i -> rows.stream().sorted((x, y) -> Long.compare(y.getId(), x.getId())).toList());
        Mockito.when(repositories.savePublisherAgreement(any())).thenAnswer(i -> {
            PublisherAgreement a = i.getArgument(0);
            if (!rows.contains(a)) {
                a.setId(rows.size() + 1);
                rows.add(a);
            }
            return a;
        });
    }

    private SimplePublisherAgreementService service(int version) {
        return new SimplePublisherAgreementService(repositories, version);
    }

    @Test
    void disabledMeansEveryoneMayPublishAndNothingToSign() {
        var service = service(0);
        var json = new UserJson();
        assertThat(service.hasPublisherAgreement(user)).isTrue();
        service.enrichUserJson(json, user);
        assertThat(json.getPublisherAgreement()).isNull();
        assertThatThrownBy(() -> service.signPublisherAgreement(user, json))
                .isInstanceOfSatisfying(
                        ErrorResultException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void unsignedUserMayNotPublish() {
        var service = service(1);
        var json = new UserJson();
        assertThat(service.hasPublisherAgreement(user)).isFalse();
        assertThatThrownBy(() -> service.checkPublisherAgreement(user)).isInstanceOf(ErrorResultException.class);
        service.enrichUserJson(json, user);
        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("none");
    }

    @Test
    void signingAllowsPublishingUntilTheVersionIsRaised() {
        var json = new UserJson();
        service(1).signPublisherAgreement(user, json);
        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("signed");
        assertThatCode(() -> service(1).checkPublisherAgreement(user)).doesNotThrowAnyException();

        var later = service(2);
        var outdated = new UserJson();
        later.enrichUserJson(outdated, user);
        assertThat(outdated.getPublisherAgreement().getStatus()).isEqualTo("outdated");
        assertThat(later.hasPublisherAgreement(user)).isFalse();

        later.signPublisherAgreement(user, new UserJson());
        assertThat(later.hasPublisherAgreement(user)).isTrue();
    }

    @Test
    void historyIsKeptAcrossResignAndRevoke() {
        service(1).signPublisherAgreement(user, new UserJson());
        service(1).signPublisherAgreement(user, new UserJson());
        assertThat(rows).hasSize(1);
        service(2).signPublisherAgreement(user, new UserJson());
        assertThat(rows).hasSize(2);

        var admin = new UserData();
        admin.setId(9);
        service(2).revokePublisherAgreement(user, admin);
        assertThat(service(2).hasPublisherAgreement(user)).isFalse();
        assertThat(service(2).publisherAgreementHistory(user))
                .extracting(e -> e.version(), e -> e.revokedTimestamp() != null)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2, true),
                        org.assertj.core.groups.Tuple.tuple(1, true));
        assertThat(rows).hasSize(2).allSatisfy(a -> assertThat(a.getRevokedById()).isEqualTo(9L));
    }
}
