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

import com.google.common.base.Strings;

import org.eclipse.openvsx.json.*;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UpstreamRegistryService implements IExtensionRegistry {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamRegistryService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    UrlConfigService urlConfigService;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(urlConfigService.getUpstreamUrl());
    }

    @Override
    public NamespaceJson getNamespace(String namespace) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}";
        var uriVariables = Map.of("namespace", namespace);
        try {
            return restTemplate.getForObject(urlTemplate, NamespaceJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            throw new NotFoundException();
        }
    }

    @Override
    public NamespaceDetailsJson getNamespaceDetails(String namespace) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}/details";
        var uriVariables = Map.of("namespace", namespace);
        try {
            return restTemplate.getForObject(urlTemplate, NamespaceDetailsJson.class, uriVariables);
        } catch (RestClientException exc) {
            handleError(exc);
            throw exc;
        }
    }

    @Override
    public ResponseEntity<byte[]> getNamespaceLogo(String namespaceName, String fileName) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}/logo/{file}";
        var uriVariables = Map.of("namespace", namespaceName, "file", fileName);
        return getFile(urlTemplate, uriVariables);
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String targetPlatform) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}/{extension}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);
        if(targetPlatform != null) {
            urlTemplate += "/{targetPlatform}";
            uriVariables.put("targetPlatform", targetPlatform);
        }

        try {
            var json = restTemplate.getForObject(urlTemplate, ExtensionJson.class, uriVariables);
            makeDownloadsCompatible(json);
            return json;
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            throw new NotFoundException();
        }
    }

    @Override
    public ExtensionJson getExtension(String namespace, String extension, String targetPlatform, String version) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}/{extension}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);
        if(targetPlatform != null) {
            urlTemplate += "/{targetPlatform}";
            uriVariables.put("targetPlatform", targetPlatform);
        }
        if(version != null) {
            urlTemplate += "/{version}";
            uriVariables.put("version", version);
        }

        try {
            var json = restTemplate.getForObject(urlTemplate, ExtensionJson.class, uriVariables);
            makeDownloadsCompatible(json);
            return json;
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            throw new NotFoundException();
        }
    }

    @Override
    public ResponseEntity<byte[]> getFile(String namespace, String extension, String targetPlatform, String version, String fileName) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}/{extension}";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);
        if(TargetPlatform.isUniversal(targetPlatform)) {
            targetPlatform = null;
        }
        if(targetPlatform != null) {
            urlTemplate += "/{targetPlatform}";
            uriVariables.put("targetPlatform", targetPlatform);
        }

        urlTemplate += "/{version}/file/{fileName}";
        uriVariables.put("version", version);
        uriVariables.put("fileName", fileName);
        return getFile(urlTemplate, uriVariables);
    }

    private ResponseEntity<byte[]> getFile(String urlTemplate, Map<String, ?> uriVariables) {
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.exchange(urlTemplate, HttpMethod.HEAD, null, byte[].class, uriVariables);
        } catch(RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("HEAD " + url, exc);
            }

            throw new NotFoundException();
        }
        var statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(UriComponentsBuilder.fromHttpUrl(urlTemplate).build(uriVariables))
                    .build();
        }
        if (statusCode.is3xxRedirection()) {
            return response;
        }
        if (statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("HEAD {}: {}", url, response);
        }
        throw new NotFoundException();
    }

    @Override
    public ReviewListJson getReviews(String namespace, String extension) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/{namespace}/{extension}/reviews";
        var uriVariables = new HashMap<String, String>();
        uriVariables.put("namespace", namespace);
        uriVariables.put("extension", extension);

        try {
            return restTemplate.getForObject(urlTemplate, ReviewListJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }

            throw new NotFoundException();
        }
    }

	@Override
	public SearchResultJson search(ISearchService.Options options) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/-/search";
        var uriVariables = new HashMap<String,String>();
        uriVariables.put("size", Integer.toString(options.requestedSize));
        uriVariables.put("offset", Integer.toString(options.requestedOffset));
        uriVariables.put("includeAllVersions", Boolean.toString(options.includeAllVersions));
        uriVariables.put("query", options.queryString);
        uriVariables.put("category", options.category);
        uriVariables.put("sortOrder", options.sortOrder);
        uriVariables.put("sortBy", options.sortBy);
        uriVariables.put("targetPlatform", options.targetPlatform);

        var queryString = uriVariables.entrySet().stream()
                .filter(entry -> !Strings.isNullOrEmpty(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(key -> key + "={" + key + "}")
                .collect(Collectors.joining("&"));
        if(!queryString.isEmpty()) {
            urlTemplate += "?" + queryString;
        }

        try {
            return restTemplate.getForObject(urlTemplate, SearchResultJson.class, uriVariables);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
                logger.error("GET " + url, exc);
            }
            throw new NotFoundException();
        }
    }

    @Override
    public QueryResultJson query(QueryParamJson param) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/-/query";
        var queryParams = new HashMap<String,String>();
        queryParams.put("namespaceName", param.namespaceName);
        queryParams.put("extensionName", param.extensionName);
        queryParams.put("extensionVersion", param.extensionVersion);
        queryParams.put("extensionId", param.extensionId);
        queryParams.put("extensionUuid", param.extensionUuid);
        queryParams.put("namespaceUuid", param.namespaceUuid);
        queryParams.put("includeAllVersions", String.valueOf(param.includeAllVersions));
        queryParams.put("targetPlatform", param.targetPlatform);


        var queryString = queryParams.entrySet().stream()
            .filter(entry -> !Strings.isNullOrEmpty(entry.getValue()))
            .map(Map.Entry::getKey)
            .map(key -> key + "={" + key + "}")
            .collect(Collectors.joining("&"));
        if(!queryString.isEmpty()) {
            urlTemplate += "?" + queryString;
        }

        try {
            return restTemplate.getForObject(urlTemplate, QueryResultJson.class, queryParams);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(queryParams);
                logger.error("GET " + url, exc);
            }
            throw new NotFoundException();
        }
    }

    @Override
    public QueryResultJson queryV2(QueryParamJsonV2 param) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/api/v2/-/query";
        var queryParams = new HashMap<String,String>();
        queryParams.put("namespaceName", param.namespaceName);
        queryParams.put("extensionName", param.extensionName);
        queryParams.put("extensionVersion", param.extensionVersion);
        queryParams.put("extensionId", param.extensionId);
        queryParams.put("extensionUuid", param.extensionUuid);
        queryParams.put("namespaceUuid", param.namespaceUuid);
        queryParams.put("includeAllVersions", String.valueOf(param.includeAllVersions));
        queryParams.put("targetPlatform", param.targetPlatform);

        var queryString = queryParams.entrySet().stream()
                .filter(entry -> !Strings.isNullOrEmpty(entry.getValue()))
                .map(Map.Entry::getKey)
                .map(key -> key + "={" + key + "}")
                .collect(Collectors.joining("&"));
        if(!queryString.isEmpty()) {
            urlTemplate += "?" + queryString;
        }

        try {
            return restTemplate.getForObject(urlTemplate, QueryResultJson.class, queryParams);
        } catch (RestClientException exc) {
            if(!isNotFound(exc)) {
                var url = UriComponentsBuilder.fromUriString(urlTemplate).build(queryParams);
                logger.error("GET " + url, exc);
            }
            throw new NotFoundException();
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

    private void makeDownloadsCompatible(ExtensionJson json) {
        if(json.downloads == null && json.files.containsKey("download")) {
            json.downloads = new HashMap<>();
            json.downloads.put(TargetPlatform.NAME_UNIVERSAL, json.files.get("download"));
        }
    }

    private boolean isNotFound(RestClientException exc) {
        return exc instanceof HttpStatusCodeException
                && ((HttpStatusCodeException) exc).getStatusCode() == HttpStatus.NOT_FOUND;
    }
}
