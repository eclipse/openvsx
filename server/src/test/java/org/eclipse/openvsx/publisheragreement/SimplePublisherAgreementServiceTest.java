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
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

class SimplePublisherAgreementServiceTest {

    private final List<PublisherAgreement> rows = new ArrayList<>();
    private final RepositoryService repositories = Mockito.mock(RepositoryService.class);
    private final UserData user = new UserData();

    SimplePublisherAgreementServiceTest() {
        user.setId(7);
        user.setProvider("github");
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

    private SimplePublisherAgreementService service(String version) {
        return new SimplePublisherAgreementService(repositories, version);
    }

    private UserData admin() {
        var admin = new UserData();
        admin.setId(9);
        return admin;
    }

    private String status(SimplePublisherAgreementService service) {
        var json = new UserJson();
        service.enrichUserJson(json, user);
        return json.getPublisherAgreement().getStatus();
    }

    @Test
    void disabledMeansEveryoneMayPublishAndNothingToSign() {
        var service = service("");
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
    void invalidVersionIsRejected() {
        for (var version : new String[] { "v1.0.0", "1.", ".1", "1..0", "1.2.3.4", "x", "-1", "01", "1.00" }) {
            assertThatThrownBy(() -> service(version)).as(version).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void aVersionNeedsNotSpellOutAllThreeNumbers() {
        for (var version : new String[] { "1", "1.0", "1.1", "1.0.0", "1.2.3", "1.0.0-beta", " 2 " }) {
            assertThat(service(version).hasPublisherAgreement(user)).as(version).isFalse();
        }
    }

    @Test
    void signaturesAreComparedByMajorWhateverTheFormOfTheVersion() {
        assertThat(service("1").signPublisherAgreement(user, new UserJson())).isTrue();

        for (var current : new String[] { "1", "1.0", "1.1", "1.0.0", "1.4.2" }) {
            assertThat(service(current).hasPublisherAgreement(user)).as(current).isTrue();
        }
        for (var current : new String[] { "2", "2.0", "2.1.0" }) {
            assertThat(status(service(current))).as(current).isEqualTo("outdated");
        }

        assertThat(service("2.1").signPublisherAgreement(user, new UserJson())).isTrue();
        assertThat(service("1.5").hasPublisherAgreement(user)).isTrue();
        assertThat(service("3").hasPublisherAgreement(user)).isFalse();
    }

    @Test
    void unsignedUserMayNotPublish() {
        var service = service("1.0.0");
        assertThat(service.hasPublisherAgreement(user)).isFalse();
        assertThatThrownBy(() -> service.checkPublisherAgreement(user)).isInstanceOf(ErrorResultException.class);
        assertThat(status(service)).isEqualTo("none");
    }

    @Test
    void signatureStaysValidWithinTheMajorVersionOnly() {
        assertThat(service("1.0.0").signPublisherAgreement(user, new UserJson())).isTrue();
        assertThat(status(service("1.0.0"))).isEqualTo("signed");
        assertThatCode(() -> service("1.0.0").checkPublisherAgreement(user)).doesNotThrowAnyException();

        assertThat(service("1.4.2").hasPublisherAgreement(user)).isTrue();

        var later = service("2.0.0");
        assertThat(status(later)).isEqualTo("outdated");
        assertThat(later.hasPublisherAgreement(user)).isFalse();

        assertThat(later.signPublisherAgreement(user, new UserJson())).isTrue();
        assertThat(later.hasPublisherAgreement(user)).isTrue();
    }

    @Test
    void signingAgainWhenAlreadySignedChangesNothing() {
        assertThat(service("1.0.0").signPublisherAgreement(user, new UserJson())).isTrue();
        assertThat(service("1.0.0").signPublisherAgreement(user, new UserJson())).isFalse();
        assertThat(rows).hasSize(1);
    }

    @Test
    void revocationBlocksSigningAgain() {
        var service = service("2.0.0");
        service("1.0.0").signPublisherAgreement(user, new UserJson());
        service.signPublisherAgreement(user, new UserJson());
        service.revokePublisherAgreement(user, admin());

        assertThat(service.hasPublisherAgreement(user)).isFalse();
        assertThat(status(service)).isEqualTo("revoked");
        assertThatThrownBy(() -> service.signPublisherAgreement(user, new UserJson()))
                .isInstanceOfSatisfying(
                        ErrorResultException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(rows).hasSize(2).allSatisfy(a -> assertThat(a.getRevokedById()).isEqualTo(9L));
        assertThat(service.publisherAgreementHistory(user))
                .extracting(e -> e.version(), e -> e.revokedTimestamp() != null)
                .containsExactly(tuple("2.0.0", true), tuple("1.0.0", true));
    }

    @Test
    void revokingAUserWhoNeverSignedBlocksThem() {
        var service = service("1.0.0");
        service.revokePublisherAgreement(user, admin());
        service.revokePublisherAgreement(user, admin());

        assertThat(rows).hasSize(1);
        assertThat(status(service)).isEqualTo("revoked");
        assertThatThrownBy(() -> service.signPublisherAgreement(user, new UserJson()))
                .isInstanceOf(ErrorResultException.class);
    }

    @Test
    void reinstatingLetsTheUserSignAgain() {
        var service = service("1.0.0");
        service.signPublisherAgreement(user, new UserJson());
        service.revokePublisherAgreement(user, admin());
        service.reinstatePublisherAgreement(user);

        assertThat(status(service)).isEqualTo("none");
        assertThat(service.hasPublisherAgreement(user)).isFalse();
        assertThat(service.signPublisherAgreement(user, new UserJson())).isTrue();
        assertThat(service.hasPublisherAgreement(user)).isTrue();
        assertThat(service.publisherAgreementHistory(user)).hasSize(2);

        service.revokePublisherAgreement(user, admin());
        assertThat(status(service)).isEqualTo("revoked");
    }

    @Test
    void reinstatingWithoutARevocationDoesNothing() {
        var service = service("1.0.0");
        service.signPublisherAgreement(user, new UserJson());
        service.reinstatePublisherAgreement(user);
        assertThat(rows).singleElement().satisfies(a -> assertThat(a.getReinstatedTimestamp()).isNull());
    }

    /** Created directly in the database, like the development super_user: there is no login to sign with. */
    private UserData withoutLoginProvider() {
        var dbUser = new UserData();
        dbUser.setId(8);
        return dbUser;
    }

    @Test
    void aUserWithoutALoginProviderIsNotHeldToTheAgreement() {
        var service = service("1.0.0");
        var dbUser = withoutLoginProvider();

        assertThat(service.hasPublisherAgreement(dbUser)).isTrue();
        assertThatCode(() -> service.checkPublisherAgreement(dbUser)).doesNotThrowAnyException();
        assertThat(rows).isEmpty();
    }

    @Test
    void aRevokedUserWithoutALoginProviderStaysBlockedUntilReinstated() {
        var service = service("1.0.0");
        var dbUser = withoutLoginProvider();

        service.revokePublisherAgreement(dbUser, admin());
        assertThat(service.hasPublisherAgreement(dbUser)).isFalse();

        service.reinstatePublisherAgreement(dbUser);
        assertThat(service.hasPublisherAgreement(dbUser)).isTrue();
    }

    @Test
    void aPublisherIsCompliantWhileTheirAgreementIsCurrent() {
        assertThat(service("1.0.0").publisherCompliance(user)).isEqualTo(PublisherCompliance.NON_COMPLIANT);

        service("1.0.0").signPublisherAgreement(user, new UserJson());
        assertThat(service("1.4.0").publisherCompliance(user)).isEqualTo(PublisherCompliance.COMPLIANT);
        assertThat(service("2.0.0").publisherCompliance(user)).isEqualTo(PublisherCompliance.NON_COMPLIANT);

        service("1.0.0").revokePublisherAgreement(user, admin());
        assertThat(service("1.0.0").publisherCompliance(user)).isEqualTo(PublisherCompliance.NON_COMPLIANT);
    }

    @Test
    void nobodyIsOutOfComplianceWhileAgreementsAreDisabled() {
        assertThat(service("").publisherCompliance(user)).isEqualTo(PublisherCompliance.COMPLIANT);
    }

    @Test
    void aUserWithoutALoginProviderIsCompliantUnlessRevoked() {
        var service = service("1.0.0");
        var dbUser = withoutLoginProvider();
        assertThat(service.publisherCompliance(dbUser)).isEqualTo(PublisherCompliance.COMPLIANT);

        service.revokePublisherAgreement(dbUser, admin());
        assertThat(service.publisherCompliance(dbUser)).isEqualTo(PublisherCompliance.NON_COMPLIANT);
    }
}
