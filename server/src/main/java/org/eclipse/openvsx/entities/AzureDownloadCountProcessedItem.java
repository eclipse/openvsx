/********************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class AzureDownloadCountProcessedItem {

    @Id
    @GeneratedValue(generator = "azureDownloadCountProcessedItemSeq")
    @SequenceGenerator(name = "azureDownloadCountProcessedItemSeq", sequenceName = "azure_download_count_processed_item_seq")
    long id;

    String name;

    LocalDateTime processedOn;

    int executionTime;

    boolean success;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getProcessedOn() {
        return processedOn;
    }

    public void setProcessedOn(LocalDateTime processedOn) {
        this.processedOn = processedOn;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
