/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.mirror;

import org.eclipse.openvsx.adapter.*;
import org.eclipse.openvsx.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
public class MirrorExtensionQueryRequestHandler implements IExtensionQueryRequestHandler {

    protected final Logger logger = LoggerFactory.getLogger(MirrorExtensionQueryRequestHandler.class);

    @Autowired
    LocalVSCodeService local;

    @Autowired
    UpstreamVSCodeService upstream;

    @Autowired
    DataMirrorService dataMirror;

    @Override
    public ExtensionQueryResult getResult(ExtensionQueryParam param, int pageSize, int defaultPageSize) {
        if (upstream.isValid()) {
            try {
                // we trust upstream to know about latest if we did not sync yet everything
                // for individual extensions, versions, assets we check another order first local and then upstream
                var result = upstream.extensionQuery(param, pageSize);
                if (!dataMirror.needsMatch()) {
                    return result;
                }
                return local.toQueryResult(result.results.get(0).extensions.stream().filter(e ->
                        dataMirror.match(e.publisher.publisherName, e.extensionName)
                ).collect(Collectors.toList()));
            } catch (NotFoundException | ResponseStatusException exc) {
                // expected issues with upstream, try local
            } catch (Throwable t) {
                // unexpected issues with upstream, log and try local
                logger.error("vscode: mirror: failed to query upstream:", t);
            }
        }
        return local.extensionQuery(param, pageSize);
    }
}
