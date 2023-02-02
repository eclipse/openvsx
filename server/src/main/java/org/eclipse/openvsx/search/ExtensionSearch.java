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

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "extensions")
public class ExtensionSearch implements Serializable {

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
    public Double rating;

    @Field(index = false)
    public int downloadCount;

    @Field(index = false)
    public List<String> categories;

    public List<String> tags;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionSearch that = (ExtensionSearch) o;
        return id == that.id
                && Double.compare(that.relevance, relevance) == 0
                && timestamp == that.timestamp
                && downloadCount == that.downloadCount
                && Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(extensionId, that.extensionId)
                && Objects.equals(targetPlatforms, that.targetPlatforms)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(description, that.description)
                && Objects.equals(rating, that.rating)
                && Objects.equals(categories, that.categories)
                && Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, relevance, name, namespace, extensionId, targetPlatforms, displayName, description, timestamp,
                rating, downloadCount, categories, tags
        );
    }
}