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

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.eclipse.openvsx.AbstractPostgresContainerTest;
import org.eclipse.openvsx.entities.PublisherAgreement;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.PublisherAgreementEntryJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.TimeUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The service against the real table. Its unit tests mock the repository, which cannot see the unique
 * index on the unrevoked rows that keeps a double click from signing twice.
 */
@Tag("integration")
@SpringBootTest
@Transactional
class SimplePublisherAgreementServicePersistenceTest extends AbstractPostgresContainerTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    private SimplePublisherAgreementService service(String version) {
        return new SimplePublisherAgreementService(repositories, version);
    }

    private UserData persistUser(String name) {
        var user = new UserData();
        user.setLoginName(name + "-" + UUID.randomUUID());
        user.setProvider("github");
        entityManager.persist(user);
        return user;
    }

    /** The outdated signing stays unrevoked next to the new one, so one unrevoked row per user cannot hold. */
    @Test
    void signingANewMajorVersionKeepsTheOutdatedSigningInTheHistory() {
        var user = persistUser("signer");
        assertThat(service("1.0.0").signPublisherAgreement(user, new UserJson())).isTrue();
        entityManager.flush();

        var later = service("2.0.0");
        assertThat(later.signPublisherAgreement(user, new UserJson())).isTrue();
        entityManager.flush();

        assertThat(later.hasPublisherAgreement(user)).isTrue();
        assertThat(later.publisherAgreementHistory(user))
                .extracting(PublisherAgreementEntryJson::version)
                .containsExactly("2.0.0", "1.0.0");
    }

    /** What the index is for: a second signing of the same version, from a double click, is refused. */
    @Test
    void theSameVersionIsNotSignedTwice() {
        var user = persistUser("double-click");
        persistSigning(user, "2.0.0");
        entityManager.flush();

        assertThatThrownBy(() -> {
            persistSigning(user, "2.0.0");
            entityManager.flush();
        }).hasMessageContaining("publisher_agreement__active__idx");
    }

    @Test
    void signingAgainAfterARevocationWasLifted() {
        var user = persistUser("revoked");
        var admin = persistUser("admin");
        var service = service("2.0.0");
        service.signPublisherAgreement(user, new UserJson());
        entityManager.flush();

        service.revokePublisherAgreement(user, admin);
        entityManager.flush();
        assertThat(service.hasPublisherAgreement(user)).isFalse();

        service.reinstatePublisherAgreement(user);
        entityManager.flush();
        assertThat(service.signPublisherAgreement(user, new UserJson())).isTrue();
        entityManager.flush();

        assertThat(service.hasPublisherAgreement(user)).isTrue();
    }

    private void persistSigning(UserData user, String version) {
        var signed = new PublisherAgreement();
        signed.setUserDataId(user.getId());
        signed.setVersion(version);
        signed.setSignedTimestamp(TimeUtil.getCurrentUTC());
        entityManager.persist(signed);
    }
}
