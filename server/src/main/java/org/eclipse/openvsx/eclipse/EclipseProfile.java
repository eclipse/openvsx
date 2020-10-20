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

public class EclipseProfile {

    public String uid;

    public String name;

    public String mail;

    public String picture;

    @JsonProperty("first_name")
    public String firstName;

    @JsonProperty("last_name")
    public String lastName;

    @JsonProperty("full_name")
    public String fullName;

    @JsonProperty("github_handle")
    public String githubHandle;

    @JsonProperty("twitter_handle")
    public String twitterHandle;
    
}