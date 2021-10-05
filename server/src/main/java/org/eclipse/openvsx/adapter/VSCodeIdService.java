/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import java.util.UUID;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class VSCodeIdService {

    private static final String API_VERSION = "3.0-preview.1";

    protected final Logger logger = LoggerFactory.getLogger(VSCodeIdService.class);

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.vscode.upstream.gallery-url:}")
    String upstreamUrl;

    public void createPublicId(Extension extension) {
        var upstreamExtension = getUpstreamData(extension);
        if (upstreamExtension != null) {
            if (upstreamExtension.extensionId != null)
                extension.setPublicId(upstreamExtension.extensionId);
            if (upstreamExtension.publisher != null && upstreamExtension.publisher.publisherId != null)
                extension.getNamespace().setPublicId(upstreamExtension.publisher.publisherId);
        }
        if (extension.getPublicId() == null)
            extension.setPublicId(createRandomId());
        if (extension.getNamespace().getPublicId() == null)
            extension.getNamespace().setPublicId(createRandomId());
    }

    private String createRandomId() {
        return UUID.randomUUID().toString();
    }

    private ExtensionQueryResult.Extension getUpstreamData(Extension extension) {
        if (Strings.isNullOrEmpty(upstreamUrl)) {
            return null;
        }
        try {
            var requestUrl = UrlUtil.createApiUrl(upstreamUrl, "extensionquery");
            var requestData = createRequestData(extension);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.ACCEPT, "application/json;api-version=" + API_VERSION);
            var result = restTemplate.postForObject(requestUrl, new HttpEntity<>(requestData, headers), ExtensionQueryResult.class);

            if (result.results != null && result.results.size() > 0) {
                var item = result.results.get(0);
                if (item.extensions != null && item.extensions.size() > 0) {
                    return item.extensions.get(0);
                }
            }
        } catch (RestClientException exc) {
            logger.error("Failed to query extension id from upstream URL", exc);
        }
        return null;
    }

    private ExtensionQueryParam createRequestData(Extension extension) {
        var request = new ExtensionQueryParam();
        var filter = new ExtensionQueryParam.Filter();
        filter.criteria = Lists.newArrayList();
        var targetCriterion = new ExtensionQueryParam.Criterion();
        targetCriterion.filterType = ExtensionQueryParam.Criterion.FILTER_TARGET;
        targetCriterion.value = "Microsoft.VisualStudio.Code";
        filter.criteria.add(targetCriterion);
        var nameCriterion = new ExtensionQueryParam.Criterion();
        nameCriterion.filterType = ExtensionQueryParam.Criterion.FILTER_EXTENSION_NAME;
        nameCriterion.value = extension.getNamespace().getName() + "." + extension.getName();
        filter.criteria.add(nameCriterion);
        filter.pageNumber = 1;
        filter.pageSize = 1;
        request.filters = Lists.newArrayList(filter);
        return request;
    }
    
}