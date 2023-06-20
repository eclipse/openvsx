/********************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.adapter.ExtensionQueryResult;
import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(value="ovsx.upstream.proxy.enabled", havingValue = "true")
public class UpstreamProxyService {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamProxyService.class);

    public NamespaceJson rewriteUrls(NamespaceJson json) {
        rewriteUrlMap(json.extensions);
        if(!StringUtils.isEmpty(json.membersUrl)) {
            json.membersUrl = rewriteUrl(json.membersUrl);
        }
        if(!StringUtils.isEmpty(json.roleUrl)) {
            json.roleUrl = rewriteUrl(json.roleUrl);
        }

        return json;
    }

    public ExtensionJson rewriteUrls(ExtensionJson json) {
        json.namespaceUrl = rewriteUrl(json.namespaceUrl);
        json.reviewsUrl = rewriteUrl(json.reviewsUrl);
        rewriteUrlMap(json.files);
        rewriteUrlMap(json.allVersions);
        json.dependencies = rewriteUrlList(json.dependencies, this::rewriteUrls);
        json.bundledExtensions = rewriteUrlList(json.bundledExtensions, this::rewriteUrls);
        rewriteUrlMap(json.downloads);
        return json;
    }

    public SearchResultJson rewriteUrls(SearchResultJson json) {
        json.extensions = rewriteUrlList(json.extensions, this::rewriteUrls);
        return json;
    }

    public QueryResultJson rewriteUrls(QueryResultJson json) {
        json.extensions = rewriteUrlList(json.extensions, this::rewriteUrls);
        return json;
    }

    public ExtensionQueryResult rewriteUrls(ExtensionQueryResult json) {
        for(var result : json.results) {
            for(var extension : result.extensions) {
                for(var version : extension.versions) {
                    version.assetUri = rewriteUrl(version.assetUri);
                    version.fallbackAssetUri = rewriteUrl(version.fallbackAssetUri);
                    for (var file : version.files) {
                        file.source = rewriteUrl(file.source);
                    }
                }
            }
        }
        return json;
    }

    public VersionsJson rewriteUrls(VersionsJson json) {
        rewriteUrlMap(json.versions);
        return json;
    }

    public VersionReferencesJson rewriteUrls(VersionReferencesJson json) {
        json.versions = json.versions.stream()
                .map(this::rewriteUrls)
                .collect(Collectors.toList());

        return json;
    }

    public JsonNode rewriteUrls(JsonNode json) {
        if(json.isArray()) {
            var list = new ObjectMapper().createArrayNode();
            var array = (ArrayNode) json;
            array.forEach(url -> list.add(rewriteUrl(url.asText())));
            json = list;
        }

        return json;
    }

    public URI rewriteUrl(URI location) {
        return URI.create(rewriteUrl(location.toString()));
    }

    private SearchEntryJson rewriteUrls(SearchEntryJson json) {
        json.url = rewriteUrl(json.url);
        rewriteUrlMap(json.files);
        json.allVersions = rewriteUrlList(json.allVersions, this::rewriteUrls);

        return json;
    }

    private VersionReferenceJson rewriteUrls(VersionReferenceJson json) {
        json.url = rewriteUrl(json.url);
        rewriteUrlMap(json.files);

        return json;
    }

    private ExtensionReferenceJson rewriteUrls(ExtensionReferenceJson json) {
        json.url = rewriteUrl(json.url);
        return json;
    }

    private <T> List<T> rewriteUrlList(List<T> jsonList, Function<T,T> mapper) {
        return jsonList != null ? jsonList.stream().map(mapper).collect(Collectors.toList()) : jsonList;
    }

    private void rewriteUrlMap(Map<String, String> map) {
        if(map != null) {
            map.replaceAll((k, v) -> rewriteUrl(v));
        }
    }

    private String rewriteUrl(String url) {
        var baseUri = URI.create(UrlUtil.getBaseUrl());
        var uri = URI.create(url);

        var scheme = baseUri.getScheme();
        var userInfo = baseUri.getUserInfo();
        var host = baseUri.getHost();
        var port = baseUri.getPort();
        var path = uri.getPath();
        var query = uri.getQuery();
        var fragment = uri.getFragment();

        try {
            return new URI(scheme, userInfo, host, port, path, query, fragment).toString();
        } catch (URISyntaxException e) {
            logger.error("failed to rewrite URI: {}", uri);
            return null;
        }
    }
}
