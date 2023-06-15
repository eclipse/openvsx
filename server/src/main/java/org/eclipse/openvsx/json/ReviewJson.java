/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.json;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(
    name = "Review",
    description = "A review of an extension"
)
@JsonInclude(Include.NON_NULL)
public class ReviewJson {

    @Schema(description = "Data of the user who posted this review")
    @NotNull
    public UserJson user;

    @Schema(description = "Date and time when this review was posted (ISO-8601)")
    @NotNull
    public String timestamp;

    @Schema(hidden = true)
    public String title;

    public String comment;

    @Schema(description = "Number of stars")
    @NotNull
    @Min(0)
    @Max(5)
    public int rating;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewJson that = (ReviewJson) o;
        return rating == that.rating
                && user.equals(that.user)
                && timestamp.equals(that.timestamp)
                && Objects.equals(title, that.title)
                && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, timestamp, title, comment, rating);
    }
}
