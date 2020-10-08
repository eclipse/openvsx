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

/**
 * Additional information about Eclipse OAuth2 login and publisher agreement
 * status attached to {@link UserData}.
 * 
 * <p>This class is not mapped to a database entity, but parsed / serialized to
 * JSON via a column converter.
 */
public class EclipseData {

    public String personId;

    public PublisherAgreement publisherAgreement;

    public class PublisherAgreement {

        /** Version of the last signed publisher agreement. */
        public String signedVersion;

        public String documentId;

        /** Timestamp of the last signed publisher agreement. */
        public LocalDateTime signedTimestamp;

        public boolean isActive;

    }
    
}