/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Download {

    @Id
    @GeneratedValue(generator = "downloadSeq")
    @SequenceGenerator(name = "downloadSeq", sequenceName = "download_seq")
    long id;

    @Column(name = "file_resource_id_not_fk")
    long fileResourceId;

    LocalDateTime timestamp;

    int amount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getFileResourceId() {
        return fileResourceId;
    }

    public void setFileResourceId(long fileResourceId) {
        this.fileResourceId = fileResourceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}
