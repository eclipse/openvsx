/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.adapter;

import org.springframework.http.ResponseEntity;

public interface IVSCodeService {

    ExtensionQueryResult extensionQuery(ExtensionQueryParam param, int defaultPageSize);

    ResponseEntity<byte[]> browse(String namespaceName, String extensionName, String version, String targetPlatform, String path);

    String download(String namespace, String extension, String version, String targetPlatform);

    String getItemUrl(String namespace, String extension);

    ResponseEntity<byte[]> getAsset(String namespace, String extensionName, String version, String assetType,
                                    String targetPlatform, String restOfTheUrl);
}
