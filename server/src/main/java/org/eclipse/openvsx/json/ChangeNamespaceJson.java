/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.json;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Used to change a namespace
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeNamespaceJson {
    public String oldNamespace;
    public String newNamespace;
    public boolean removeOldNamespace;
    public boolean mergeIfNewNamespaceAlreadyExists;
}
