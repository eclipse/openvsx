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
package org.eclipse.openvsx.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * One signing of the publisher agreement by a user; rows are only appended or marked revoked, so they
 * form the audit trail. See {@code SimplePublisherAgreementService}.
 */
@Entity
@Table(name = "publisher_agreement")
public class PublisherAgreement implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "publisherAgreementSeq")
    @SequenceGenerator(name = "publisherAgreementSeq", sequenceName = "publisher_agreement_seq", allocationSize = 1)
    private long id;

    @Column(name = "user_data_id", nullable = false)
    private long userDataId;

    @Column(nullable = false)
    private int version;

    @Column(name = "signed_timestamp", nullable = false)
    private LocalDateTime signedTimestamp;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserDataId() {
        return userDataId;
    }

    public void setUserDataId(long userDataId) {
        this.userDataId = userDataId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getSignedTimestamp() {
        return signedTimestamp;
    }

    public void setSignedTimestamp(LocalDateTime signedTimestamp) {
        this.signedTimestamp = signedTimestamp;
    }

    @Column(name = "revoked_timestamp")
    private LocalDateTime revokedTimestamp;

    @Column(name = "revoked_by_id")
    private Long revokedById;

    public LocalDateTime getRevokedTimestamp() {
        return revokedTimestamp;
    }

    public void setRevokedTimestamp(LocalDateTime revokedTimestamp) {
        this.revokedTimestamp = revokedTimestamp;
    }

    public Long getRevokedById() {
        return revokedById;
    }

    public void setRevokedById(Long revokedById) {
        this.revokedById = revokedById;
    }
}
