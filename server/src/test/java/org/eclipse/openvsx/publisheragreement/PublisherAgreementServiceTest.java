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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import org.eclipse.openvsx.ExtensionService;
import org.eclipse.openvsx.entities.PublisherAgreementEvent;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.json.PublisherAgreementEntryJson;
import org.eclipse.openvsx.json.UserJson;
import org.eclipse.openvsx.publisheragreement.AgreementStatus.State;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.ErrorResultException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The service against a mocked provider and an in-memory event store: what holds whatever the provider is.
 */
class PublisherAgreementServiceTest {

    private static final LocalDateTime SIGNED_AT = LocalDateTime.of(2026, 10, 1, 10, 0);

    private final List<PublisherAgreementEvent> events = new ArrayList<>();
    private final RepositoryService repositories = mock(RepositoryService.class);
    private final ExtensionService extensions = mock(ExtensionService.class);
    private final PublisherAgreementProvider provider = mock(PublisherAgreementProvider.class);
    private final UserData user = new UserData();
    private final UserData admin = new UserData();
    private PublisherAgreementService service;

    @BeforeEach
    void setup() {
        user.setId(7);
        user.setLoginName("user");
        user.setProvider("github");
        admin.setId(9);
        admin.setLoginName("admin");
        admin.setProvider("github");

        when(repositories.findLatestPublisherAgreementEvent(anyLong(), any())).thenAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            Collection<PublisherAgreementEvent.Type> types = invocation.getArgument(1);
            return events.stream()
                    .filter(e -> e.getUserDataId() == userId && types.contains(e.getType()))
                    .max(Comparator.comparingLong(PublisherAgreementEvent::getId))
                    .orElse(null);
        });
        when(repositories.findPublisherAgreementEvents(anyLong())).thenAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            return events.stream()
                    .filter(e -> e.getUserDataId() == userId)
                    .sorted(Comparator.comparingLong(PublisherAgreementEvent::getId).reversed())
                    .toList();
        });
        when(repositories.savePublisherAgreementEvent(any())).thenAnswer(invocation -> {
            PublisherAgreementEvent event = invocation.getArgument(0);
            event.setId(events.size() + 1);
            events.add(event);
            return event;
        });

        when(provider.enabled()).thenReturn(true);
        givenStatus(State.NONE);
        service = new PublisherAgreementService(provider, repositories, extensions);
    }

    private AgreementStatus givenStatus(State state) {
        return givenStatus(new AgreementStatus(state, state == State.NONE ? null : "1.0.0", SIGNED_AT, false));
    }

    private AgreementStatus givenStatus(AgreementStatus status) {
        when(provider.status(any())).thenReturn(status);
        when(provider.statusForAdmin(any())).thenReturn(status);
        return status;
    }

    private void givenSigningBecomes(AgreementStatus status) {
        when(provider.sign(any())).thenReturn(status);
    }

    private static ErrorResultException refusedWith(HttpStatus status, Runnable action) {
        var thrown = org.assertj.core.api.Assertions.catchThrowableOfType(
                ErrorResultException.class,
                action::run);
        assertThat(thrown).isNotNull();
        assertThat(thrown.getStatus()).isEqualTo(status);
        return thrown;
    }

    private List<PublisherAgreementEvent.Type> eventTypes() {
        return events.stream().map(PublisherAgreementEvent::getType).toList();
    }

    // ---- who may publish

    @Test
    void everyoneMayPublishWhileAgreementsAreDisabled() {
        when(provider.enabled()).thenReturn(false);

        assertThat(service.mayPublish(user)).isTrue();
        assertThatCode(() -> service.requireAgreement(user)).doesNotThrowAnyException();
        verify(provider, never()).status(any());
    }

    @Test
    void aSignedUserMayPublish() {
        givenStatus(State.SIGNED);

        assertThat(service.mayPublish(user)).isTrue();
        assertThatCode(() -> service.requireAgreement(user)).doesNotThrowAnyException();
    }

    @Test
    void aUserWhoHasNotSignedIsToldToSign() {
        var thrown = refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));

        assertThat(thrown.getMessage()).contains("must sign the publisher agreement");
        assertThat(service.mayPublish(user)).isFalse();
    }

    @Test
    void aUserWithoutTheLoginTheProviderNeedsIsToldToLogInFirst() {
        when(provider.requiredLogin()).thenReturn(new ExternalLogin("eclipse", "Eclipse"));
        givenStatus(new AgreementStatus(State.NONE, null, null, false));

        var thrown = refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));

        assertThat(thrown.getMessage()).contains("log in with Eclipse");
    }

    @Test
    void aUserWithTheLoginLinkedIsOnlyToldToSign() {
        when(provider.requiredLogin()).thenReturn(new ExternalLogin("eclipse", "Eclipse"));
        givenStatus(new AgreementStatus(State.NONE, null, null, true));

        var thrown = refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));

        assertThat(thrown.getMessage()).doesNotContain("log in with").contains("must sign");
    }

    @Test
    void anOutdatedAgreementIsRefusedWithItsVersion() {
        givenStatus(State.OUTDATED);

        var thrown = refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));

        assertThat(thrown.getMessage()).contains("outdated").contains("1.0.0");
    }

    /** Nobody is told to sign because a lookup failed, and nobody publishes on a guess. */
    @Test
    void anAgreementTheProviderCannotTellAboutIsRefusedAsUnverifiable() {
        givenStatus(State.UNKNOWN);

        refusedWith(HttpStatus.SERVICE_UNAVAILABLE, () -> service.requireAgreement(user));
        assertThat(service.mayPublish(user)).isFalse();
    }

    @Test
    void aProviderThatFailsIsTreatedAsUnknown() {
        when(provider.status(any())).thenThrow(new IllegalStateException("remote down"));

        refusedWith(HttpStatus.SERVICE_UNAVAILABLE, () -> service.requireAgreement(user));
    }

    @Test
    void aUserCreatedInTheDatabaseIsNotHeldToTheAgreement() {
        var dbUser = new UserData();
        dbUser.setId(8);

        assertThat(service.mayPublish(dbUser)).isTrue();
        verify(provider, never()).status(any());
    }

    @Test
    void aRevokedUserCreatedInTheDatabaseIsStillBlocked() {
        var dbUser = new UserData();
        dbUser.setId(8);
        service.revoke(dbUser, admin);

        refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(dbUser));
    }

    // ---- the block

    @Test
    void aRevokedUserIsBlockedWhateverTheProviderSays() {
        givenStatus(State.SIGNED);
        service.revoke(user, admin);

        var thrown = refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));

        assertThat(thrown.getMessage()).contains("revoked");
    }

    @Test
    void aRevokedUserCannotSignAgain() {
        service.revoke(user, admin);

        refusedWith(HttpStatus.FORBIDDEN, () -> service.sign(user, new UserJson()));
        verify(provider, never()).sign(any());
        verify(extensions, never()).reactivateExtensions(any());
    }

    @Test
    void revokingRecordsTheAdminAndBlocksOnce() {
        service.revoke(user, admin);
        service.revoke(user, admin);

        assertThat(eventTypes()).containsExactly(PublisherAgreementEvent.Type.REVOKED);
        assertThat(events.getFirst().getActorId()).isEqualTo(9L);
        assertThat(events.getFirst().getUserDataId()).isEqualTo(7L);
    }

    @Test
    void revokingBlocksEvenAUserWhoNeverSigned() {
        givenStatus(State.NONE);

        service.revoke(user, admin);

        verify(provider, never()).withdraw(any(), any());
        assertThat(service.mayPublish(user)).isFalse();
    }

    @Test
    void revokingWithdrawsWhatTheProviderKnowsToExist() {
        for (var state : List.of(State.SIGNED, State.OUTDATED)) {
            Mockito.clearInvocations(provider);
            givenStatus(state);

            service.revoke(user, admin);

            verify(provider).withdraw(user, admin);
        }
    }

    @Test
    void revokingDoesNotAskTheProviderToWithdrawWhatIsNotConfirmed() {
        for (var state : List.of(State.NONE, State.UNKNOWN)) {
            givenStatus(state);

            service.revoke(user, admin);

            verify(provider, never()).withdraw(any(), any());
        }
    }

    /** The block does not depend on the provider succeeding. */
    @Test
    void aFailedWithdrawalIsReportedAfterTheBlockIsInPlace() {
        givenStatus(State.SIGNED);
        Mockito.doThrow(
                new PublisherAgreementException(
                        PublisherAgreementException.Reason.UNAVAILABLE,
                        "provider down"))
                .when(provider).withdraw(any(), any());

        refusedWith(HttpStatus.SERVICE_UNAVAILABLE, () -> service.revoke(user, admin));

        assertThat(eventTypes()).containsExactly(PublisherAgreementEvent.Type.REVOKED);
        refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));
    }

    @Test
    void revokingDoesNothingWhileAgreementsAreDisabled() {
        when(provider.enabled()).thenReturn(false);

        service.revoke(user, admin);

        assertThat(events).isEmpty();
    }

    @Test
    void reinstatingLiftsTheBlockSoTheProviderDecidesAgain() {
        service.revoke(user, admin);

        service.reinstate(user, admin);

        assertThat(eventTypes())
                .containsExactly(PublisherAgreementEvent.Type.REVOKED, PublisherAgreementEvent.Type.REINSTATED);
        assertThat(events.getLast().getActorId()).isEqualTo(9L);
        givenStatus(State.SIGNED);
        assertThat(service.mayPublish(user)).isTrue();
    }

    @Test
    void reinstatingAUserWhoIsNotBlockedDoesNothing() {
        service.reinstate(user, admin);

        assertThat(events).isEmpty();
    }

    @Test
    void aReinstatedUserMaySignAgain() {
        service.revoke(user, admin);
        service.reinstate(user, admin);
        givenSigningBecomes(new AgreementStatus(State.SIGNED, "2.0", SIGNED_AT, false));

        assertThat(service.sign(user, new UserJson())).isTrue();
    }

    @Test
    void aSecondRevocationBlocksAgainAfterAReinstatement() {
        service.revoke(user, admin);
        service.reinstate(user, admin);

        service.revoke(user, admin);

        assertThat(eventTypes()).containsExactly(
                PublisherAgreementEvent.Type.REVOKED,
                PublisherAgreementEvent.Type.REINSTATED,
                PublisherAgreementEvent.Type.REVOKED);
        refusedWith(HttpStatus.FORBIDDEN, () -> service.requireAgreement(user));
    }

    // ---- signing

    @Test
    void signingRecordsTheVersionAndReactivatesTheExtensions() {
        var signed = new AgreementStatus(State.SIGNED, "2.0", SIGNED_AT, false);
        givenSigningBecomes(signed);
        var json = new UserJson();

        assertThat(service.sign(user, json)).isTrue();

        assertThat(eventTypes()).containsExactly(PublisherAgreementEvent.Type.SIGNED);
        assertThat(events.getFirst().getVersion()).isEqualTo("2.0");
        assertThat(events.getFirst().getActorId()).isEqualTo(7L);
        verify(extensions).reactivateExtensions(user);
        // answered from what signing returned: the provider may still show the state before it
        verify(provider, times(1)).status(user);
        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("signed");
        assertThat(json.getPublisherAgreement().getVersion()).isEqualTo("2.0");
    }

    @Test
    void signingWhatIsAlreadySignedChangesNothing() {
        givenStatus(State.SIGNED);
        var json = new UserJson();

        assertThat(service.sign(user, json)).isFalse();

        verify(provider, never()).sign(any());
        verify(extensions, never()).reactivateExtensions(any());
        assertThat(events).isEmpty();
        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("signed");
    }

    @Test
    void signingAnOutdatedAgreementSignsTheCurrentOne() {
        givenStatus(State.OUTDATED);
        givenSigningBecomes(new AgreementStatus(State.SIGNED, "2.0", SIGNED_AT, false));

        assertThat(service.sign(user, new UserJson())).isTrue();

        assertThat(events).extracting(PublisherAgreementEvent::getVersion).containsExactly("2.0");
    }

    /** A double click: the provider answers with the version that was just recorded. */
    @Test
    void theSameSigningIsNotRecordedTwice() {
        var signed = new AgreementStatus(State.SIGNED, "1.0", SIGNED_AT, false);
        givenSigningBecomes(signed);

        service.sign(user, new UserJson());
        service.sign(user, new UserJson());

        assertThat(events).hasSize(1);
    }

    @Test
    void signingIsNotSupportedWhileAgreementsAreDisabled() {
        when(provider.enabled()).thenReturn(false);

        refusedWith(HttpStatus.NOT_FOUND, () -> service.sign(user, new UserJson()));
    }

    @Test
    void signingIsRefusedWhenTheProviderCannotTellWhereTheUserStands() {
        givenStatus(State.UNKNOWN);

        refusedWith(HttpStatus.SERVICE_UNAVAILABLE, () -> service.sign(user, new UserJson()));
        verify(provider, never()).sign(any());
    }

    @Test
    void aProviderFailureWhileSigningBecomesTheMatchingResponseAndChangesNothing() {
        var statuses = new Object[][] {
            { PublisherAgreementException.Reason.LOGIN_REQUIRED, HttpStatus.FORBIDDEN },
            { PublisherAgreementException.Reason.REJECTED, HttpStatus.BAD_REQUEST },
            { PublisherAgreementException.Reason.UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE },
            { PublisherAgreementException.Reason.UNSUPPORTED, HttpStatus.NOT_FOUND }
        };
        for (var pair : statuses) {
            Mockito.doThrow(new PublisherAgreementException((PublisherAgreementException.Reason) pair[0], "no"))
                    .when(provider).sign(any());

            var thrown = refusedWith((HttpStatus) pair[1], () -> service.sign(user, new UserJson()));

            assertThat(thrown.getMessage()).isEqualTo("no");
        }
        assertThat(events).isEmpty();
        verify(extensions, never()).reactivateExtensions(any());
    }

    // ---- what clients are shown

    @Test
    void theUserSeesTheirAgreementWithTheLoginTheProviderNeeds() {
        when(provider.requiredLogin()).thenReturn(new ExternalLogin("eclipse", "Eclipse"));
        givenStatus(new AgreementStatus(State.SIGNED, "1.0.0", SIGNED_AT, true));
        var json = new UserJson();

        service.enrichUserJson(json, user);

        var agreement = json.getPublisherAgreement();
        assertThat(agreement.getStatus()).isEqualTo("signed");
        assertThat(agreement.getVersion()).isEqualTo("1.0.0");
        assertThat(agreement.getTimestamp()).isEqualTo("2026-10-01T10:00Z");
        assertThat(agreement.getLoginProvider()).isEqualTo("eclipse");
        assertThat(agreement.getLoginProviderName()).isEqualTo("Eclipse");
        assertThat(json.getAdditionalLogins()).extracting(UserJson::getProvider).containsExactly("eclipse");
    }

    @Test
    void aLoginIsOnlyReportedAsLinkedWhenItIs() {
        when(provider.requiredLogin()).thenReturn(new ExternalLogin("eclipse", "Eclipse"));
        givenStatus(new AgreementStatus(State.NONE, null, null, false));
        var json = new UserJson();

        service.enrichUserJson(json, user);

        assertThat(json.getAdditionalLogins()).isNull();
    }

    @Test
    void theStatusNamesAreWhatClientsExpect() {
        var expected = new Object[][] {
            { State.NONE, "none" },
            { State.SIGNED, "signed" },
            { State.OUTDATED, "outdated" }
        };
        for (var pair : expected) {
            givenStatus((State) pair[0]);
            var json = new UserJson();

            service.enrichUserJson(json, user);

            assertThat(json.getPublisherAgreement().getStatus()).isEqualTo(pair[1]);
        }
    }

    @Test
    void aRevokedUserIsShownAsRevoked() {
        givenStatus(State.SIGNED);
        service.revoke(user, admin);
        var json = new UserJson();

        service.enrichUserJson(json, user);

        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("revoked");
        assertThat(json.getPublisherAgreement().getVersion()).isNull();
    }

    @Test
    void anAgreementTheProviderCannotTellAboutIsNoneToTheUser() {
        givenStatus(State.UNKNOWN);
        var json = new UserJson();

        service.enrichUserJson(json, user);

        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("none");
    }

    /** An admin must not mistake what could not be determined for none. */
    @Test
    void anAgreementTheProviderCannotTellAboutIsLeftUnsetForAdmins() {
        givenStatus(State.UNKNOWN);
        var json = new UserJson();

        service.enrichUserJsonForAdmin(json, user);

        assertThat(json.getPublisherAgreement()).isNull();
    }

    @Test
    void adminsAreShownTheStatusSeenFromAdminsAndNoLinkedLogin() {
        when(provider.requiredLogin()).thenReturn(new ExternalLogin("eclipse", "Eclipse"));
        when(provider.statusForAdmin(any())).thenReturn(new AgreementStatus(State.OUTDATED, "0.9", SIGNED_AT, true));
        var json = new UserJson();

        service.enrichUserJsonForAdmin(json, user);

        assertThat(json.getPublisherAgreement().getStatus()).isEqualTo("outdated");
        assertThat(json.getPublisherAgreement().getLoginProvider()).isEqualTo("eclipse");
        assertThat(json.getAdditionalLogins()).isNull();
        verify(provider, never()).status(any());
    }

    @Test
    void nothingIsAddedWhileAgreementsAreDisabled() {
        when(provider.enabled()).thenReturn(false);
        var json = new UserJson();

        service.enrichUserJson(json, user);
        service.enrichUserJsonForAdmin(json, user);

        assertThat(json.getPublisherAgreement()).isNull();
    }

    // ---- compliance and history

    @Test
    void aPublisherIsCompliantWhileTheirAgreementIsCurrent() {
        givenStatus(State.SIGNED);

        assertThat(service.compliance(user)).isEqualTo(PublisherCompliance.COMPLIANT);
    }

    @Test
    void aPublisherWithoutACurrentAgreementIsNotCompliant() {
        for (var state : List.of(State.NONE, State.OUTDATED)) {
            givenStatus(state);

            assertThat(service.compliance(user)).as(state.name()).isEqualTo(PublisherCompliance.NON_COMPLIANT);
        }
    }

    @Test
    void aRevokedPublisherIsNotCompliantWhateverTheProviderSays() {
        givenStatus(State.SIGNED);
        service.revoke(user, admin);

        assertThat(service.compliance(user)).isEqualTo(PublisherCompliance.NON_COMPLIANT);
    }

    @Test
    void aPublisherTheProviderCannotTellAboutIsNeitherCompliantNorNot() {
        givenStatus(State.UNKNOWN);
        assertThat(service.compliance(user)).isEqualTo(PublisherCompliance.UNKNOWN);

        when(provider.statusForAdmin(any())).thenThrow(new IllegalStateException("remote down"));
        assertThat(service.compliance(user)).isEqualTo(PublisherCompliance.UNKNOWN);
    }

    @Test
    void aUserCreatedInTheDatabaseIsCompliantAndNobodyIsWhileAgreementsAreDisabled() {
        var dbUser = new UserData();
        dbUser.setId(8);
        assertThat(service.compliance(dbUser)).isEqualTo(PublisherCompliance.COMPLIANT);

        when(provider.enabled()).thenReturn(false);
        assertThat(service.compliance(user)).isEqualTo(PublisherCompliance.COMPLIANT);
    }

    @Test
    void theHistoryListsWhatHappenedLatestFirst() {
        givenSigningBecomes(new AgreementStatus(State.SIGNED, "1.0", SIGNED_AT, false));
        service.sign(user, new UserJson());
        service.revoke(user, admin);
        service.reinstate(user, admin);

        var history = service.history(user);

        assertThat(history).extracting(PublisherAgreementEntryJson::type)
                .containsExactly("reinstated", "revoked", "signed");
        assertThat(history).extracting(PublisherAgreementEntryJson::actorId).containsExactly(9L, 9L, 7L);
        assertThat(history.getLast().version()).isEqualTo("1.0");
        assertThat(history.getFirst().version()).isNull();
    }
}
