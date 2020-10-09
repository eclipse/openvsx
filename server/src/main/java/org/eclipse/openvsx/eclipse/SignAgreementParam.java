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
public class SignAgreementParam {

    /** The version number of the document/agreement. */
    @JsonProperty("Version")
    public int version;

    public SignAgreementParam(String version) {
        this.version = Integer.parseInt(version);
    }

}