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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.eclipse.openvsx.entities.PublisherAgreementEvent;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.publisheragreement.AgreementStatus.State;
import org.eclipse.openvsx.repositories.RepositoryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SimplePublisherAgreementProviderTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 10, 1, 10, 0);

    private final List<PublisherAgreementEvent> events = new ArrayList<>();
    private final RepositoryService repositories = mock(RepositoryService.class);
    private final UserData user = new UserData();

    SimplePublisherAgreementProviderTest() {
        user.setId(7);
        when(repositories.findLatestPublisherAgreementEvent(anyLong(), any())).thenAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            Collection<PublisherAgreementEvent.Type> types = invocation.getArgument(1);
            return events.stream()
                    .filter(e -> e.getUserDataId() == userId && types.contains(e.getType()))
                    .max(Comparator.comparingLong(PublisherAgreementEvent::getId))
                    .orElse(null);
        });
    }

    private SimplePublisherAgreementProvider provider(String version) {
        return new SimplePublisherAgreementProvider(repositories, version);
    }

    private void happened(PublisherAgreementEvent.Type type, String version) {
        var event = new PublisherAgreementEvent();
        event.setId(events.size() + 1);
        event.setUserDataId(user.getId());
        event.setType(type);
        event.setVersion(version);
        event.setTimestamp(AT);
        events.add(event);
    }

    @Test
    void aBlankVersionDisablesAgreements() {
        for (var version : new String[] { "", "  ", null }) {
            assertThat(provider(version).enabled()).as(String.valueOf(version)).isFalse();
        }
        assertThat(provider("1").enabled()).isTrue();
    }

    @Test
    void invalidVersionIsRejected() {
        for (var version : new String[] { "v1.0.0", "1.", ".1", "1..0", "1.2.3.4", "x", "-1", "01", "1.00" }) {
            assertThatThrownBy(() -> provider(version)).as(version).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void aVersionTooLargeToReadIsRejectedNamingTheProperty() {
        assertThatThrownBy(() -> provider("99999999999999999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ovsx.publisher-agreement.version");
    }

    @Test
    void aVersionNeedsNotSpellOutAllThreeNumbers() {
        for (var version : new String[] { "1", "1.0", "1.1", "1.0.0", "1.2.3", "1.0.0-beta", " 2 " }) {
            assertThat(provider(version).enabled()).as(version).isTrue();
        }
    }

    @Test
    void aUserWhoNeverSignedHasNone() {
        assertThat(provider("1.0.0").status(user).state()).isEqualTo(State.NONE);
    }

    @Test
    void aSignatureIsComparedByMajorWhateverTheFormOfTheVersion() {
        happened(PublisherAgreementEvent.Type.SIGNED, "1");

        for (var current : new String[] { "1", "1.0", "1.1", "1.0.0", "1.4.2" }) {
            assertThat(provider(current).status(user).state()).as(current).isEqualTo(State.SIGNED);
        }
        for (var current : new String[] { "2", "2.0", "2.1.0" }) {
            assertThat(provider(current).status(user).state()).as(current).isEqualTo(State.OUTDATED);
        }
    }

    @Test
    void aLowerConfiguredMajorStillCountsTheHigherSignature() {
        happened(PublisherAgreementEvent.Type.SIGNED, "2.1");

        assertThat(provider("1.5").status(user).state()).isEqualTo(State.SIGNED);
        assertThat(provider("3").status(user).state()).isEqualTo(State.OUTDATED);
    }

    @Test
    void theStatusCarriesWhatWasSigned() {
        happened(PublisherAgreementEvent.Type.SIGNED, "1.2");

        var status = provider("1").status(user);

        assertThat(status.version()).isEqualTo("1.2");
        assertThat(status.timestamp()).isEqualTo(AT);
        assertThat(status.loginLinked()).isFalse();
    }

    @Test
    void aRevocationEndsTheSigningAndASigningAfterItStartsOneAgain() {
        happened(PublisherAgreementEvent.Type.SIGNED, "1.0.0");
        happened(PublisherAgreementEvent.Type.REVOKED, null);
        assertThat(provider("1.0.0").status(user).state()).isEqualTo(State.NONE);

        // a reinstatement alone gives nothing back: the user has to sign again
        happened(PublisherAgreementEvent.Type.REINSTATED, null);
        assertThat(provider("1.0.0").status(user).state()).isEqualTo(State.NONE);

        happened(PublisherAgreementEvent.Type.SIGNED, "1.0.0");
        assertThat(provider("1.0.0").status(user).state()).isEqualTo(State.SIGNED);
    }

    @Test
    void anotherUsersSigningIsNotTheUsers() {
        var other = new PublisherAgreementEvent();
        other.setId(1);
        other.setUserDataId(99);
        other.setType(PublisherAgreementEvent.Type.SIGNED);
        other.setVersion("1.0.0");
        other.setTimestamp(AT);
        events.add(other);

        assertThat(provider("1.0.0").status(user).state()).isEqualTo(State.NONE);
    }

    @Test
    void signingGivesTheConfiguredVersionAsWritten() {
        var status = provider(" 1.2 ").sign(user);

        assertThat(status.state()).isEqualTo(State.SIGNED);
        assertThat(status.version()).isEqualTo("1.2");
        assertThat(status.timestamp()).isNotNull();
    }

    @Test
    void withdrawingChangesNothingForTheServiceRecordsTheRevocation() {
        provider("1").withdraw(user, new UserData());

        verifyNoMoreInteractions(repositories);
        Mockito.verify(repositories, Mockito.never()).savePublisherAgreementEvent(any());
    }
}
