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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

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

    @JsonProperty("publisher_agreements")
    @JsonDeserialize(using = PublisherAgreements.Deserializer.class)
    public PublisherAgreements publisherAgreements;

    public static class PublisherAgreements {

        @JsonProperty("open-vsx")
        public PublisherAgreement openVsx;

        public static class Deserializer extends JsonDeserializer<PublisherAgreements> {

            private static final TypeReference<List<PublisherAgreement>> TYPE_LIST_AGREEMENT = new TypeReference<List<PublisherAgreement>>() {};

			@Override
			public PublisherAgreements deserialize(JsonParser p, DeserializationContext ctxt)
					throws IOException, JsonProcessingException {
				if (p.currentToken() == JsonToken.START_ARRAY) {
                    var list = p.getCodec().readValue(p, TYPE_LIST_AGREEMENT);
                    var result = new PublisherAgreements();
                    if (!list.isEmpty())
                        result.openVsx = list.get(0);
                    return result;
                }
                return p.getCodec().readValue(p, PublisherAgreements.class);
            }

        }
    }

    public static class PublisherAgreement {

        public String version;

    }
    
}