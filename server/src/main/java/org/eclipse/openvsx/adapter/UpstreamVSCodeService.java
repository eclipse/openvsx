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

import com.google.common.base.Strings;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URI;

@Component
public class UpstreamVSCodeService implements IVSCodeService {

    protected final Logger logger = LoggerFactory.getLogger(UpstreamVSCodeService.class);

    @Autowired
    RestTemplate restTemplate;

    @Value("${ovsx.upstream.url:}")
    String upstreamUrl;

    public boolean isValid() {
        return !Strings.isNullOrEmpty(upstreamUrl);
    }

    @Override
    public ExtensionQueryResult extensionQuery(ExtensionQueryParam param, int defaultPageSize) {
        var apiUrl = UrlUtil.createApiUrl(upstreamUrl, "vscode", "gallery", "extensionquery");
        var request = new RequestEntity<>(param, HttpMethod.POST, URI.create(apiUrl));
        var response = restTemplate.exchange(request, ExtensionQueryResult.class);
        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful()) {
            return response.getBody();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("POST {}: {}", apiUrl, response);
        }

        throw new NotFoundException();
    }

    @Override
    public ResponseEntity<byte[]> browse(String namespaceName, String extensionName, String version, String path) {
        var segments = new String[]{ "vscode", "unpkg", namespaceName, extensionName, version, path };
        var apiUrl = UrlUtil.createApiUrl(upstreamUrl, segments);
        var request = new RequestEntity<Void>(HttpMethod.GET, URI.create(apiUrl));
        var response = restTemplate.exchange(request, byte[].class);
        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            return response;
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("GET {}: {}", apiUrl, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String download(String namespace, String extension, String version, String targetPlatform) {
        var segments = new String[]{ "vscode", "gallery", "publishers", namespace, "vsextensions", extension, version, "vspackage" };
        var apiUrl = UrlUtil.createApiUrl(upstreamUrl, segments);
        var request = new RequestEntity<Void>(HttpMethod.GET, URI.create(apiUrl));
        var response = nonRedirectingRestTemplate().exchange(request, Void.class);
        var statusCode = response.getStatusCode();
        if(statusCode.is3xxRedirection()) {
            return response.getHeaders().getLocation().toString();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("GET {}: {}", apiUrl, response);
        }

        throw new NotFoundException();
    }

    @Override
    public String getItemUrl(String namespace, String extension) {
        var apiUrl = UrlUtil.createApiUrl(upstreamUrl, "vscode", "item");
        apiUrl = UrlUtil.addQuery(apiUrl, "itemName", String.join(".", namespace, extension));
        var request = new RequestEntity<Void>(HttpMethod.GET, URI.create(apiUrl));
        var response = nonRedirectingRestTemplate().exchange(request, Void.class);
        var statusCode = response.getStatusCode();
        if(statusCode.is3xxRedirection()) {
            return response.getHeaders().getLocation().toString();
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("GET {}: {}", apiUrl, response);
        }

        throw new NotFoundException();
    }

    @Override
    public ResponseEntity<byte[]> getAsset(String namespace, String extensionName, String version, String assetType, String targetPlatform, String restOfTheUrl) {
        var segments = new String[]{ "vscode", "asset", namespace, extensionName, version, assetType, restOfTheUrl };
        var apiUrl = UrlUtil.createApiUrl(upstreamUrl, segments);
        var request = new RequestEntity<Void>(HttpMethod.GET, URI.create(apiUrl));
        var response = restTemplate.exchange(request, byte[].class);
        var statusCode = response.getStatusCode();
        if(statusCode.is2xxSuccessful() || statusCode.is3xxRedirection()) {
            return response;
        }
        if(statusCode.isError() && statusCode != HttpStatus.NOT_FOUND) {
            logger.error("GET {}: {}", apiUrl, response);
        }

        throw new NotFoundException();
    }

    private RestTemplate nonRedirectingRestTemplate() {
        return new RestTemplate(new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod ) {
                connection.setInstanceFollowRedirects(false);
            }
        });
    }
}
