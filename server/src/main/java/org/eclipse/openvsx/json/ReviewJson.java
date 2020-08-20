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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;;

@ApiModel(
    value = "Review",
    description = "A review of an extension"
)
@JsonInclude(Include.NON_NULL)
public class ReviewJson {

    @ApiModelProperty("Data of the user who posted this review")
    @NotNull
    public UserJson user;

    @ApiModelProperty("Date and time when this review was posted (ISO-8601)")
    @NotNull
    public String timestamp;

    @ApiModelProperty(hidden = true)
    public String title;

    public String comment;

    @ApiModelProperty(value = "Number of stars")
    @NotNull
    @Min(0)
    @Max(5)
    public int rating;

}