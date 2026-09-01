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
package org.eclipse.openvsx.accesstoken;

import jakarta.persistence.EntityManager;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import org.eclipse.openvsx.AbstractPostgresContainerTest;
import org.eclipse.openvsx.entities.PersonalAccessToken;
import org.eclipse.openvsx.entities.PersonalAccessTokenType;
import org.eclipse.openvsx.entities.UserData;
import org.eclipse.openvsx.search.SearchUtilService;
import org.eclipse.openvsx.util.TimeUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The paths that write a token row each touch a different column, so one must not carry the others back
 * to whatever it happened to load. Only a real database shows this: it is Hibernate's generated UPDATE
 * that decides, and with the default full-row update the loser's stale columns win.
 */
@SpringBootTest
class AccessTokenConcurrentWriteTest extends AbstractPostgresContainerTest {

    @Autowired
    EntityManager em;

    @Autowired
    PlatformTransactionManager txManager;

    @MockitoBean
    SearchUtilService search;

    @MockitoBean
    JobRequestScheduler scheduler;

    @Test
    void upgradingATokenDoesNotResurrectOneRevokedMeanwhile() {
        var tokenId = persistLegacyToken();

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            // what the upgrade job holds: a snapshot taken before the revoke below, so its copy still
            // says active = true
            var token = em.find(PersonalAccessToken.class, tokenId);
            assertThat(token.isActive()).isTrue();

            revokeInAnotherTransaction(tokenId);

            // and now it writes the two columns it actually cares about
            token.setValue("hashed-" + token.getValue());
            token.setVersion(1);
        });

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var token = em.find(PersonalAccessToken.class, tokenId);
            assertThat(token.getVersion()).isEqualTo(1);
            assertThat(token.getValue()).startsWith("hashed-");
            // the revoke has to survive: without @DynamicUpdate the upgrade's full-row UPDATE writes
            // active = true back over it, handing a revoked token back to its holder
            assertThat(token.isActive()).isFalse();
        });

        cleanUp(tokenId);
    }

    private long persistLegacyToken() {
        return new TransactionTemplate(txManager).execute(status -> {
            var user = new UserData();
            user.setLoginName("concurrent-write-user");
            user.setProvider("github");
            em.persist(user);

            var token = new PersonalAccessToken();
            token.setUser(user);
            token.setValue("concurrent-write-token-value");
            token.setActive(true);
            token.setCreatedTimestamp(TimeUtil.getCurrentUTC());
            token.setVersion(0);
            token.setType(PersonalAccessTokenType.LLT);
            token.setDescription("legacy token");
            em.persist(token);
            em.flush();
            return token.getId();
        });
    }

    private void revokeInAnotherTransaction(long tokenId) {
        var requiresNew = new TransactionTemplate(txManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNew.executeWithoutResult(
                status -> em.createNativeQuery("UPDATE personal_access_token SET active = false WHERE id = :id")
                        .setParameter("id", tokenId)
                        .executeUpdate());
    }

    /** The container is shared with every other test, so leave nothing of this one behind. */
    private void cleanUp(long tokenId) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            var token = em.find(PersonalAccessToken.class, tokenId);
            var userId = token.getUser().getId();
            em.remove(token);
            em.flush();
            em.createQuery("delete from UserData u where u.id = :id").setParameter("id", userId).executeUpdate();
        });
    }
}
