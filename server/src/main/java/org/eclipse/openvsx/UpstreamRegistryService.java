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

import static org.eclipse.openvsx.util.UrlUtil.addQuery;
import static org.eclipse.openvsx.util.UrlUtil.createApiUrl;

import java.net.URI;

import com.google.common.base.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.QueryParamJson;
import org.eclipse.openvsx.json.QueryResultJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UpstreamRegistryService implements IExtensionRegistry {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamRegistryService.class);

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.upstream.url:}")
    String upstreamUrl;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(upstreamUrl);
    }

    @Override
    public NamespaceJson getNamespace(String namespace) {
        try {
            String requestUrl = createApiUrl(upstreamUrl, "api", namespace);
            return restTemplate.getForObject(requestUrl, NamespaceJson.class);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension) {
        try {
            String requestUrl = createApiUrl(upstreamUrl, "api", namespace, extension);
            return restTemplate.getForObject(requestUrl, ExtensionJson.class);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String version) {
        try {
            String requestUrl = createApiUrl(upstreamUrl, "api", namespace, extension, version);
            return restTemplate.getForObject(requestUrl, ExtensionJson.class);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extension, String version, String fileName) {
        return getFile(createApiUrl(upstreamUrl, "api", namespace, extension, version, "file", fileName));
    }

    private ResponseEntity<byte[]> getFile(String url) {
        var upstreamLocation = URI.create(url);
        var request = new RequestEntity<Void>(HttpMethod.HEAD, upstreamLocation);
        var response = restTemplate.exchange(request, byte[].class);
        var statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(upstreamLocation)
                    .build();
        }
        if (statusCode.is3xxRedirection()) {
            return response;
        }
        if (statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("HEAD " + url + ": " + response.toString());
        }
        throw new NotFoundException();
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extension) {
        try {
            String requestUrl = createApiUrl(upstreamUrl, "api", namespace, extension, "reviews");
            return restTemplate.getForObject(requestUrl, ReviewListJson.class);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }

	@Override
	public SearchResultJson search(ISearchService.Options options) {
		try {
            var searchUrl = createApiUrl(upstreamUrl, "api", "-", "search");
            var requestUrl = addQuery(searchUrl,
                "query", options.queryString,
                "category", options.category,
                "size", Integer.toString(options.requestedSize),
                "offset", Integer.toString(options.requestedOffset),
                "sortOrder", options.sortOrder,
                "sortBy", options.sortBy,
                "includeAllVersions", Boolean.toString(options.includeAllVersions)
            );
            return restTemplate.getForObject(requestUrl, SearchResultJson.class);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        try {
            String requestUrl = createApiUrl(upstreamUrl, "api", "-", "query");
            return restTemplate.postForObject(requestUrl, param, QueryResultJson.class);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }
    
    private void handleError(Throwable exc) throws RuntimeException {
        if (exc instanceof HttpStatusCodeException) {
            var status = ((HttpStatusCodeException) exc).getStatusCode();
            if (status == HttpStatus.NOT_FOUND)
                throw new NotFoundException();
            else
                throw new ResponseStatusException(status,
                        "Upstream registry responded with status \"" + status.getReasonPhrase() + "\".", exc);
        } else if (exc.getCause() != null && exc.getCause() != exc) {
            handleError(exc.getCause());
        }
    }

}