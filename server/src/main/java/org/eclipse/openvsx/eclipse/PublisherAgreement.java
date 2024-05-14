/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.eclipse;

import java.time.LocalDateTime;
import java.util.Objects;

public class PublisherAgreement {

    public boolean isActive;

    public String documentId;

    /** Version of the last signed publisher agreement. */
    public String version;

    /** Timestamp of the last signed publisher agreement. */
    public LocalDateTime timestamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublisherAgreement that = (PublisherAgreement) o;
        return isActive == that.isActive
                && Objects.equals(documentId, that.documentId)
                && Objects.equals(version, that.version)
                && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isActive, documentId, version, timestamp);
    }
}
