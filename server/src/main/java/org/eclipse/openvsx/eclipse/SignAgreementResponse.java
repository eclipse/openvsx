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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * https://eclipsefdn.github.io/openvsx-publisher-agreement-specs/#/paths/~1publisher_agreement/post
 */
public class SignAgreementResponse {

    /** Unique identifier for an addressable object in the API. */
    @JsonProperty("PersonID")
    String personID;

    /** Unique identifier for an addressable object in the API. */
    @JsonProperty("DocumentID")
    String documentID;

    /** The version number for the current document. */
    @JsonProperty("Version")
    String version;

    /** Date string in the RFC 3339 format. */
    @JsonProperty("EffectiveDate")
    String effectiveDate;

    /** Date string in the RFC 3339 format. */
    @JsonProperty("ReceivedDate")
    String receivedDate;

    /** Date string in the RFC 3339 format. */
    @JsonProperty("ExpirationDate")
    String expirationDate;

    /** The signed document as a blob entity. */
    @JsonProperty("ScannedDocumentBLOB")
    String scannedDocumentBLOB;

    @JsonProperty("ScannedDocumentBytes")
    String scannedDocumentBytes;

    /** The MIME type for the posted document blob. */
    @JsonProperty("ScannedDocumentMime")
    String scannedDocumentMime;

    /** The name of the document being posted. */
    @JsonProperty("ScannedDocumentFileName")
    String scannedDocumentFileName;

    @JsonProperty("SysDocument")
    SysDocument sysDocument;

    public static class SysDocument {

        /** Unique identifier for an addressable object in the API. */
        @JsonProperty("DocumentID")
        String documentID;

        /** The version string for the current system document. */
        @JsonProperty("Version")
        String version;

        /** Literal document content blob entity. */
        @JsonProperty("DocumentBLOB")
        String documentBLOB;

        /** Description of the system document. */
        @JsonProperty("Description")
        String description;

        @JsonProperty("Comments")
        String comments;

        @JsonProperty("IsActive")
        String isActive;

        @JsonProperty("Type")
        String type;

        @JsonProperty("ContentType")
        String contentType;
        
    }
}