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

import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Iterables;

import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.PublisherJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.ReviewResultJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.util.NotFoundException;

@RestController
public class RegistryAPI {

    @Autowired
    LocalRegistryService local;

    @Autowired
    UpstreamRegistryService upstream;

    @Value("#{environment.OVSX_WEBUI_URL}")
    String webuiUrl;

    protected Iterable<IExtensionRegistry> getRegistries() {
        var registries = new ArrayList<IExtensionRegistry>();
        registries.add(local);
        if (upstream.isValid())
            registries.add(upstream);
        return registries;
    }

    @GetMapping(
        value = "/api/{publisher}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public PublisherJson getPublisher(@PathVariable("publisher") String publisherName) {
        for (var registry : getRegistries()) {
            try {
                return registry.getPublisher(publisherName);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        value = "/api/{publisher}/{extension}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ExtensionJson getExtension(@PathVariable("publisher") String publisherName,
                                      @PathVariable("extension") String extensionName) {
        for (var registry : getRegistries()) {
            try {
                return registry.getExtension(publisherName, extensionName);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        value = "/api/{publisher}/{extension}/{version}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ExtensionJson getExtension(@PathVariable("publisher") String publisherName,
                                      @PathVariable("extension") String extensionName,
                                      @PathVariable("version") String version) {
        for (var registry : getRegistries()) {
            try {
                return registry.getExtension(publisherName, extensionName, version);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping("/api/{publisher}/{extension}/file/{fileName}")
    @CrossOrigin
    public ResponseEntity<byte[]> getFile(@PathVariable("publisher") String publisherName,
                                          @PathVariable("extension") String extensionName,
                                          @PathVariable("fileName") String fileName) {
        for (var registry : getRegistries()) {
            try {
                var content = registry.getFile(publisherName, extensionName, fileName);
                var headers = getFileResponseHeaders(fileName);
                return new ResponseEntity<>(content, headers, HttpStatus.OK);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping("/api/{publisher}/{extension}/{version}/file/{fileName}")
    @CrossOrigin
    public ResponseEntity<byte[]> getFile(@PathVariable("publisher") String publisherName,
                                          @PathVariable("extension") String extensionName,
                                          @PathVariable("version") String version,
                                          @PathVariable("fileName") String fileName) {
        for (var registry : getRegistries()) {
            try {
                var content = registry.getFile(publisherName, extensionName, version, fileName);
                var headers = getFileResponseHeaders(fileName);
                return new ResponseEntity<>(content, headers, HttpStatus.OK);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    private HttpHeaders getFileResponseHeaders(String fileName) {
        var headers = new HttpHeaders();
        headers.setContentType(getFileType(fileName));
        return headers;
    }

    private MediaType getFileType(String fileName) {
        if (fileName.endsWith(".vsix"))
            return MediaType.APPLICATION_OCTET_STREAM;
        var contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null)
            return MediaType.parseMediaType(contentType);
        return MediaType.TEXT_PLAIN;
    }

    @GetMapping(
        value = "/api/{publisher}/{extension}/reviews",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ReviewListJson getReviews(@PathVariable("publisher") String publisherName,
                                     @PathVariable("extension") String extensionName) {
        for (var registry : getRegistries()) {
            try {
                return registry.getReviews(publisherName, extensionName);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        value = "/api/-/search",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public SearchResultJson search(@RequestParam(name = "query", required = false) String query,
                                   @RequestParam(name = "category", required = false) String category,
                                   @RequestParam(name = "size", defaultValue = "18") int size,
                                   @RequestParam(name = "offset", defaultValue = "0") int offset) {
        if (size < 0) {
            return SearchResultJson.error("The parameter 'size' must not be negative.");
        }
        if (offset < 0) {
            return SearchResultJson.error("The parameter 'offset' must not be negative.");
        }

        var result = new SearchResultJson();
        result.extensions = new ArrayList<>(size);
        for (var registry : getRegistries()) {
            if (result.extensions.size() >= size) {
                return result;
            }
            try {
                var subResult = registry.search(query, category, size, offset);
                if (subResult.extensions != null && subResult.extensions.size() > 0) {
                    int limit = size - result.extensions.size();
                    var subResultSize = mergeSearchResults(result, subResult.extensions, limit);
                    result.offset += subResult.offset;
                    offset = Math.max(offset - subResult.offset - subResultSize, 0);
                }
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        return result;
    }

    private int mergeSearchResults(SearchResultJson result, List<SearchEntryJson> entries, int limit) {
        var previousResult = Iterables.limit(result.extensions, result.extensions.size());
        var entriesIter = entries.iterator();
        int mergedEntries = 0;
        while (entriesIter.hasNext() && result.extensions.size() < limit) {
            var next = entriesIter.next();
            if (!Iterables.any(previousResult, ext -> ext.publisher.equals(next.publisher) && ext.name.equals(next.name))) {
                result.extensions.add(next);
                mergedEntries++;
            }
        }
        return mergedEntries;
    }

    @PostMapping(
        value = "/api/-/publish",
        consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ExtensionJson publish(InputStream content) {
        return local.publish(content);
    }

    @PostMapping(
        value = "/api/{publisher}/{extension}/review",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ReviewResultJson> review(@RequestBody(required = false) ReviewJson review,
                                                   @PathVariable("publisher") String publisherName,
                                                   @PathVariable("extension") String extensionName,
                                                   @CookieValue(name = "sessionid", required = false) String sessionId) {
        ReviewResultJson json;
        if (sessionId == null) {
            json = ReviewResultJson.error("Not logged in.");
        } else if (review == null) {
            json = ReviewResultJson.error("No JSON input.");
        } else if (review.rating < 0 || review.rating > 5) {
            json = ReviewResultJson.error("The rating must be an integer number between 0 and 5.");
        } else {
            json = local.review(review, publisherName, extensionName, sessionId);
        }
        return new ResponseEntity<>(json, getReviewHeaders(), HttpStatus.OK);
    }

    private HttpHeaders getReviewHeaders() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!Strings.isNullOrEmpty(webuiUrl)) {
            headers.setAccessControlAllowOrigin(webuiUrl);
            headers.setAccessControlAllowCredentials(true);
            headers.setAccessControlAllowHeaders(Arrays.asList("Content-Type"));
        }
        return headers;
    }

}