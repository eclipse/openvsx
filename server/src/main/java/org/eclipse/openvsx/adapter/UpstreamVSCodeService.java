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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.UpstreamProxyService;
import org.eclipse.openvsx.UrlConfigService;
import org.eclipse.openvsx.util.HttpHeadersUtil;
import org.eclipse.openvsx.util.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class UpstreamVSCodeService implements IVSCodeService {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamVSCodeService.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired(required = false)
    UpstreamProxyService proxy;

    @Autowired
    RestTemplate nonRedirectingRestTemplate;

    @Autowired
    UrlConfigService urlConfigService;

    public boolean isValid() {
        return !StringUtils.isEmpty(urlConfigService.getUpstreamUrl());
    }

    @Override
    public ExtensionQueryResult extensionQuery(ExtensionQueryParam param, int defaultPageSize) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/gallery/extensionquery";
        var request = new RequestEntity<>(param, HttpHeadersUtil.getForwardedHeaders(), HttpMethod.POST, URI.create(urlTemplate));
        ResponseEntity<ExtensionQueryResult> response;
        try {
            response = restTemplate.exchange(request, ExtensionQueryResult.class);
        } catch(RestClientException exc) {
            throw propagateRestException(exc, request.getMethod(), urlTemplate, null);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful()) {
            var json = response.getBody();
            return proxy != null ? proxy.rewriteUrls(json) : json;
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("POST {}: {}", urlTemplate, response);
        }

        throw new NotFoundException();
    }

    @Override
    public ResponseEntity<byte[]> browse(String namespaceName, String extensionName, String version, String path) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/unpkg/{namespace}/{extension}/{version}";
        var uriVariables = new HashMap<>(Map.of(
            "namespace", namespaceName,
            "extension", extensionName,
            "version", version
        ));

        if (path != null && !path.isBlank()) {
            var segments = path.split("/");
            for (var i = 0; i < segments.length; i++) {
                var varName = "seg" + i;
                urlTemplate = urlTemplate + "/{" + varName + "}";
                uriVariables.put(varName, segments[i]);
            }
        }

        ResponseEntity<byte[]> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, byte[].class, uriVariables);
        } catch(RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            var headers = new HttpHeaders();
            headers.addAll(response.getHeaders());
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.VARY);

            if(proxy != null && MediaType.APPLICATION_JSON.equals(headers.getContentType())) {
                try {
                    var mapper = new ObjectMapper();
                    var json = mapper.readTree(response.getBody());
                    json = proxy.rewriteUrls(json);
                    response = ResponseEntity.status(statusCode)
                            .headers(headers)
                            .body(mapper.writeValueAsString(json).getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logger.error("Failed to read/write JSON", e);
                }
            } else {
                response = new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
            }

            return response;
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String download(String namespace, String extension, String version, String targetPlatform) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage?targetPlatform={targetPlatform}";
        var uriVariables = Map.of(
                "namespace", namespace,
                "extension", extension,
                "version", version,
                "targetPlatform", targetPlatform
        );

        ResponseEntity<Void> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, Void.class, uriVariables);
        } catch(RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is3xxRedirection()) {
            var location = response.getHeaders().getLocation();
            if(proxy != null) {
                location = proxy.rewriteUrl(location);
            }

            return location.toString();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String getItemUrl(String namespace, String extension) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/item?itemName={namespace}.{extension}";
        var uriVariables = Map.of("namespace", namespace, "extension", extension);

        ResponseEntity<Void> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, Void.class, uriVariables);
        } catch (RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is3xxRedirection()) {
            var location = response.getHeaders().getLocation();
            if(proxy != null) {
                location = proxy.rewriteUrl(location);
            }

            return location.toString();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    @Override
    public ResponseEntity<byte[]> getAsset(String namespace, String extensionName, String version, String assetType, String targetPlatform, String restOfTheUrl) {
        var urlTemplate = urlConfigService.getUpstreamUrl() + "/vscode/asset/{namespace}/{extension}/{version}/{assetType}";
        var uriVariables = new HashMap<>(Map.of(
            "namespace", namespace,
            "extension", extensionName,
            "version", version,
            "assetType", assetType,
            "targetPlatform", targetPlatform
        ));

        if (restOfTheUrl != null && !restOfTheUrl.isBlank()) {
            var segments = restOfTheUrl.split("/");
            for (var i = 0; i < segments.length; i++) {
                var varName = "seg" + i;
                urlTemplate = urlTemplate + "/{" + varName + "}";
                uriVariables.put(varName, segments[i]);
            }
        }

        urlTemplate = urlTemplate + "?targetPlatform={targetPlatform}";
        ResponseEntity<byte[]> response;
        var method = HttpMethod.GET;
        try {
            response = nonRedirectingRestTemplate.exchange(urlTemplate, method, null, byte[].class, uriVariables);
        } catch (RestClientException exc) {
            throw propagateRestException(exc, method, urlTemplate, uriVariables);
        }

        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful()) {
            var headers = new HttpHeaders();
            headers.addAll(response.getHeaders());
            headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            headers.remove(HttpHeaders.VARY);
            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
        }
        if(statusCode.is3xxRedirection()) {
            var location = response.getHeaders().getLocation();
            if(proxy != null) {
                location = proxy.rewriteUrl(location);
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .headers(response.getHeaders())
                    .location(location)
                    .build();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            var url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
            logger.error("GET {}: {}", url, response);
        }

        throw new NotFoundException();
    }

    private NotFoundException propagateRestException(RestClientException exc, HttpMethod method, String urlTemplate,
        Map<String, String> uriVariables) {
        if (exc instanceof HttpStatusCodeException) {
            var statusCode = ((HttpStatusCodeException)exc).getStatusCode();
            if(statusCode == HttpStatus.NOT_FOUND) {
                return new NotFoundException();
            }
        }

        URI url;
        if (uriVariables != null) {
            url = UriComponentsBuilder.fromUriString(urlTemplate).build(uriVariables);
        } else {
            url = URI.create(urlTemplate);
        }
        logger.error("upstream: " + method + ": " + url, exc);
        return new NotFoundException();
    }

}
