/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

package org.eclipse.openvsx.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(
    name = "ChangesResult",
    description = "Paginated feed of extension version changes"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangesResultJson extends ResultJson {

    public static ChangesResultJson error(String message) {
        var result = new ChangesResultJson();
        result.setError(message);
        return result;
    }

    @Schema(description = "Number of skipped entries according to the changes request")
    @NotNull
    @Min(0)
    private int offset;

    @Schema(description = "Total number of entries matching the changes request")
    @NotNull
    @Min(0)
    private int totalSize;

    @Schema(
        description = "Entries in ascending order of their last transition, limited to the size "
                + "specified in the changes request"
    )
    @NotNull
    private List<ChangeEntryJson> changes;

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public List<ChangeEntryJson> getChanges() {
        return changes;
    }

    public void setChanges(List<ChangeEntryJson> changes) {
        this.changes = changes;
    }
}
