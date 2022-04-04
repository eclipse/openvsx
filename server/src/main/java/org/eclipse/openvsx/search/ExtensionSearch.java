/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.search;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "extensions")
public class ExtensionSearch {

    @Field(index = false)
    public long id;

    @Field(index = false, type = FieldType.Float)
    public double relevance;

    public String name;

    public String namespace;

    @Field(index = false)
    public String extensionId;

    public List<String> targetPlatforms;

    public String displayName;

    public String description;

    @Field(index = false)
    public long timestamp;

    @Nullable
    @Field(index = false, type = FieldType.Float)
    public Double averageRating;

    @Field(index = false)
    public int downloadCount;

    @Field(index = false)
    public List<String> categories;

    public List<String> tags;
}