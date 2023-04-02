/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.search.ISearchService;
import org.springframework.http.ResponseEntity;

/**
 * Declaration of the registry API methods that can be accessed without authentication.
 */
public interface IExtensionRegistry {

    NamespaceJson getNamespace(String namespace);

    ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform);

    ExtensionJson getExtension(String namespace, String extensionName, String targetPlatform, String version);

    ResponseEntity<byte[]> getFile(String namespace, String extensionName, String targetPlatform, String version, String fileName);

    ReviewListJson getReviews(String namespace, String extension);

    SearchResultJson search(ISearchService.Options options);

    QueryResultJson query(QueryRequest request);

    QueryResultJson queryV2(QueryRequestV2 request);

    NamespaceDetailsJson getNamespaceDetails(String namespace);

    ResponseEntity<byte[]> getNamespaceLogo(String namespaceName, String fileName);
}