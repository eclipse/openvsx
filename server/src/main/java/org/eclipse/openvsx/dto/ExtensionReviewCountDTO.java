/** ******************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.dto;

public class ExtensionReviewCountDTO {

    private final long extensiondId;
    private final long reviewCount;

    public ExtensionReviewCountDTO(long extensiondId, long reviewCount) {
        this.extensiondId = extensiondId;
        this.reviewCount = reviewCount;
    }

    public long getExtensiondId() {
        return extensiondId;
    }

    public long getReviewCount() {
        return reviewCount;
    }
}
