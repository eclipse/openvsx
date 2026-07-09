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
package org.eclipse.openvsx.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.openvsx.entities.Namespace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@SpringBootTest(properties = {
        "ovsx.elasticsearch.enabled=false"
})
@ActiveProfiles("test_db")
@Transactional
class NamespaceRepositoryTest {

    @Autowired
    NamespaceRepository repo;

    @Autowired
    EntityManager em;

    @BeforeEach
    void persistNamespaces() {
        persistNamespace("github", "The GitHub Org");
        persistNamespace("other", "Other Org");
    }

    @Test
    void findConflictingNamespaceMatchesNameIgnoringCase() {
        var conflict = repo.findConflictingNamespace("GITHUB", "other");

        assertThat(conflict).isPresent();
        assertThat(conflict.get().getName()).isEqualTo("github");
    }

    @Test
    void findConflictingNamespaceMatchesDisplayNameIgnoringCase() {
        var conflict = repo.findConflictingNamespace("the github org", "other");

        assertThat(conflict).isPresent();
        assertThat(conflict.get().getName()).isEqualTo("github");
    }

    @Test
    void findConflictingNamespaceExcludesOwnNamespaceIgnoringCase() {
        var conflict = repo.findConflictingNamespace("The GitHub Org", "GitHub");

        assertThat(conflict).isEmpty();
    }

    @Test
    void findConflictingNamespaceStillFindsOtherNamespaces() {
        persistNamespace("dup", "The GitHub Org");

        var conflict = repo.findConflictingNamespace("the github org", "github");

        assertThat(conflict).isPresent();
        assertThat(conflict.get().getName()).isEqualTo("dup");
    }

    @Test
    void findConflictingNamespaceEmptyWhenNothingMatches() {
        var conflict = repo.findConflictingNamespace("Brand New", "github");

        assertThat(conflict).isEmpty();
    }

    private void persistNamespace(String name, String displayName) {
        var namespace = new Namespace();
        namespace.setName(name);
        namespace.setDisplayName(displayName);
        em.persist(namespace);
    }
}
