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

import java.util.Arrays;

import com.google.common.base.Strings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.util.NotFoundException;

@Component
public class UpstreamRegistryService implements IExtensionRegistry {

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
    public byte[] getFile(String namespace, String extension, String version, String fileName) {
        return getFile(createApiUrl(upstreamUrl, "api", namespace, extension, version, "file", fileName));
    }

    private byte[] getFile(String url) {
        var headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), byte[].class);
        switch (response.getStatusCode()) {
            case OK:
                return response.getBody();
            case NOT_FOUND:
                throw new NotFoundException();
            default:
                throw new ResponseStatusException(response.getStatusCode(),
                        "Upstream registry responded with status \"" + response.getStatusCode().getReasonPhrase() + "\".");
        }
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
	public SearchResultJson search(String query, String category, int size, int offset, String sortOrder, String sortBy) {
		try {
            var searchUrl = createApiUrl(upstreamUrl, "api", "-", "search");
            var requestUrl = addQuery(searchUrl, "query", query, "category", category,
                    "size", Integer.toString(size), "offset", Integer.toString(offset));
            return restTemplate.getForObject(requestUrl, SearchResultJson.class);
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