/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.entities;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Additional information about Eclipse OAuth2 login and publisher agreement
 * status attached to {@link UserData}.
 * 
 * <p>This class is not mapped to a database entity, but parsed / serialized to
 * JSON via a column converter.
 */
public class EclipseData implements Cloneable {

    public String personId;

    public PublisherAgreement publisherAgreement;

    public static class PublisherAgreement {

        public boolean isActive;

        public String documentId;

        /** Version of the last signed publisher agreement. */
        public String version;

        /** Timestamp of the last signed publisher agreement. */
        public LocalDateTime timestamp;

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof PublisherAgreement))
                return false;
            var other = (PublisherAgreement) obj;
            if (this.isActive != other.isActive)
                return false;
            if (!Objects.equals(this.documentId, other.documentId))
                return false;
            if (!Objects.equals(this.version, other.version))
                return false;
            if (!Objects.equals(this.timestamp, other.timestamp))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(isActive, documentId, version, timestamp);
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof EclipseData))
            return false;
        var other = (EclipseData) obj;
        if (!Objects.equals(this.personId, other.personId))
            return false;
        if (!Objects.equals(this.publisherAgreement, other.publisherAgreement))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(personId, publisherAgreement);
    }

    @Override
    public EclipseData clone() {
        try {
			return (EclipseData) super.clone();
		} catch (CloneNotSupportedException exc) {
			throw new RuntimeException(exc);
		}
    }
    
}