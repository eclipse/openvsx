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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Objects;

@Schema(
    name = "Badge",
    description = "A badge to be shown in the sidebar of the extension page in the registry"
)
@JsonInclude(Include.NON_NULL)
public class BadgeJson implements Serializable {

    @Schema(description = "Image URL of the badge")
    public String url;

    @Schema(description = "The link users will follow when clicking the badge")
    public String href;

    public String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BadgeJson badgeJson = (BadgeJson) o;
        return Objects.equals(url, badgeJson.url)
                && Objects.equals(href, badgeJson.href)
                && Objects.equals(description, badgeJson.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, href, description);
    }
}
