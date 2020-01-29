/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.repositories;

import org.springframework.data.repository.Repository;

import org.eclipse.openvsx.entities.ExtensionIcon;
import org.eclipse.openvsx.entities.ExtensionVersion;

public interface ExtensionIconRepository extends Repository<ExtensionIcon, Long> {

    ExtensionIcon findByExtension(ExtensionVersion extVersion);

}