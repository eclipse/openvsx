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

    private String uid;

    private String name;

    private String mail;

    private String picture;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("github_handle")
    private String githubHandle;

    @JsonProperty("twitter_handle")
    private String twitterHandle;

    @JsonProperty("publisher_agreements")
    @JsonDeserialize(using = PublisherAgreements.Deserializer.class)
    private PublisherAgreements publisherAgreements;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGithubHandle() {
        return githubHandle;
    }

    public void setGithubHandle(String githubHandle) {
        this.githubHandle = githubHandle;
    }

    public String getTwitterHandle() {
        return twitterHandle;
    }

    public void setTwitterHandle(String twitterHandle) {
        this.twitterHandle = twitterHandle;
    }

    public PublisherAgreements getPublisherAgreements() {
        return publisherAgreements;
    }

    public void setPublisherAgreements(PublisherAgreements publisherAgreements) {
        this.publisherAgreements = publisherAgreements;
    }

    public static class PublisherAgreements {

        @JsonProperty("open-vsx")
        private PublisherAgreement openVsx;

        public PublisherAgreement getOpenVsx() {
            return openVsx;
        }

        public void setOpenVsx(PublisherAgreement openVsx) {
            this.openVsx = openVsx;
        }

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
        private String version;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}