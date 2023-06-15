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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.util.NamingUtil;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class VSCodeIdService {

    private static final String API_VERSION = "3.0-preview.1";

    protected final Logger logger = LoggerFactory.getLogger(VSCodeIdService.class);

    @Autowired
    RestTemplate vsCodeIdRestTemplate;

    @Autowired
    RepositoryService repositories;

    @Autowired
    UrlConfigService urlConfigService;

    public boolean setPublicIds(Extension extension) {
        var updateExistingPublicIds = false;
        var upstream = getUpstreamExtension(extension);
        if (upstream != null) {
            if (upstream.extensionId != null) {
                extension.setPublicId(upstream.extensionId);
                updateExistingPublicIds = true;
            }
            if (upstream.publisher != null && upstream.publisher.publisherId != null) {
                extension.getNamespace().setPublicId(upstream.publisher.publisherId);
            }
        }
        if (extension.getPublicId() == null) {
            extension.setPublicId(createRandomId());
        }
        if (extension.getNamespace().getPublicId() == null) {
            extension.getNamespace().setPublicId(createRandomId());
        }

        return updateExistingPublicIds;
    }

    private String createRandomId() {
        return UUID.randomUUID().toString();
    }

    private ExtensionQueryResult.Extension getUpstreamExtension(Extension extension) {
        var galleryUrl = urlConfigService.getUpstreamGalleryUrl();
        if (StringUtils.isEmpty(galleryUrl)) {
            return null;
        }

        var requestUrl = UrlUtil.createApiUrl(galleryUrl, "extensionquery");
        var requestData = createRequestData(extension);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "application/json;api-version=" + API_VERSION);
        var result = vsCodeIdRestTemplate.postForObject(requestUrl, new HttpEntity<>(requestData, headers), ExtensionQueryResult.class);

        if (result.results != null && result.results.size() > 0) {
            var item = result.results.get(0);
            if (item.extensions != null && item.extensions.size() > 0) {
                return item.extensions.get(0);
            }
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
        nameCriterion.value = NamingUtil.toExtensionId(extension);
        filter.criteria.add(nameCriterion);
        filter.pageNumber = 1;
        filter.pageSize = 1;
        request.filters = Lists.newArrayList(filter);
        return request;
    }
}
