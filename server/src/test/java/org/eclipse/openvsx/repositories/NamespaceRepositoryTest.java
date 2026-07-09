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

    Namespace github;
    Namespace other;

    @BeforeEach
    void persistNamespaces() {
        github = persistNamespace("github", "The GitHub Org");
        other = persistNamespace("other", "Other Org");
    }

    @Test
    void findMultipleConflictingNamespaceMatches() {
        persistNamespace("github2", "github");
        var conflicts = repo.findConflictingNamespaces("GITHUB", other);

        assertThat(conflicts).isNotEmpty();
        assertThat(conflicts.getFirst().getName()).isEqualTo("github");
    }

    @Test
    void findConflictingNamespaceMatchesNameIgnoringCase() {
        var conflicts = repo.findConflictingNamespaces("GITHUB", other);

        assertThat(conflicts).isNotEmpty();
        assertThat(conflicts.getFirst().getName()).isEqualTo("github");
    }

    @Test
    void findConflictingNamespaceMatchesDisplayNameIgnoringCase() {
        var conflicts = repo.findConflictingNamespaces("the github org", other);

        assertThat(conflicts).isNotEmpty();
        assertThat(conflicts.getFirst().getName()).isEqualTo("github");
    }

    @Test
    void findConflictingNamespaceExcludesOwnNamespaceIgnoringCase() {
        var conflicts = repo.findConflictingNamespaces("The GitHub Org", github);

        assertThat(conflicts).isEmpty();
    }

    @Test
    void findConflictingNamespaceStillFindsOtherNamespaces() {
        var dup = persistNamespace("dup", "The GitHub Org");

        var conflicts = repo.findConflictingNamespaces("the github org", github);

        assertThat(conflicts).isNotEmpty();
        assertThat(conflicts.getFirst().getName()).isEqualTo(dup.getName());
    }

    @Test
    void findConflictingNamespaceEmptyWhenNothingMatches() {
        var conflicts = repo.findConflictingNamespaces("Brand New", github);

        assertThat(conflicts).isEmpty();
    }

    private Namespace persistNamespace(String name, String displayName) {
        var namespace = new Namespace();
        namespace.setName(name);
        namespace.setDisplayName(displayName);
        em.persist(namespace);
        return namespace;
    }
}
