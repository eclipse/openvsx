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
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Iterables;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.ReviewResultJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistryAPI {

    private final static int REVIEW_TITLE_SIZE = 255;
    private final static int REVIEW_COMMENT_SIZE = 2048;

    @Autowired
    LocalRegistryService local;

    @Autowired
    UpstreamRegistryService upstream;

    protected Iterable<IExtensionRegistry> getRegistries() {
        var registries = new ArrayList<IExtensionRegistry>();
        registries.add(local);
        if (upstream.isValid())
            registries.add(upstream);
        return registries;
    }

    @GetMapping(
        path = "/api/{namespace}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public NamespaceJson getNamespace(@PathVariable String namespace) {
        for (var registry : getRegistries()) {
            try {
                return registry.getNamespace(namespace);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ExtensionJson getExtension(@PathVariable String namespace,
                                      @PathVariable String extension) {
        for (var registry : getRegistries()) {
            try {
                return registry.getExtension(namespace, extension);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/{version}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ExtensionJson getExtension(@PathVariable String namespace,
                                      @PathVariable String extension,
                                      @PathVariable String version) {
        for (var registry : getRegistries()) {
            try {
                return registry.getExtension(namespace, extension, version);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping("/api/{namespace}/{extension}/file/{fileName:.+}")
    @CrossOrigin
    public ResponseEntity<byte[]> getFile(@PathVariable String namespace,
                                          @PathVariable String extension,
                                          @PathVariable String fileName) {
        for (var registry : getRegistries()) {
            try {
                var content = registry.getFile(namespace, extension, fileName);
                var headers = getFileResponseHeaders(fileName);
                return new ResponseEntity<>(content, headers, HttpStatus.OK);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping("/api/{namespace}/{extension}/{version}/file/{fileName:.+}")
    @CrossOrigin
    public ResponseEntity<byte[]> getFile(@PathVariable String namespace,
                                          @PathVariable String extension,
                                          @PathVariable String version,
                                          @PathVariable String fileName) {
        for (var registry : getRegistries()) {
            try {
                var content = registry.getFile(namespace, extension, version, fileName);
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
        headers.setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
        if (fileName.endsWith(".vsix")) {
            headers.add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        }
        return headers;
    }

    private MediaType getFileType(String fileName) {
        if (fileName.endsWith(".vsix")) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        var contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return MediaType.parseMediaType(contentType);
        }
        return MediaType.TEXT_PLAIN;
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/reviews",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ReviewListJson getReviews(@PathVariable String namespace,
                                     @PathVariable String extension) {
        for (var registry : getRegistries()) {
            try {
                return registry.getReviews(namespace, extension);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        path = "/api/-/search",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public SearchResultJson search(@RequestParam(required = false) String query,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(defaultValue = "18") int size,
                                   @RequestParam(defaultValue = "0") int offset) {
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
            if (!Iterables.any(previousResult, ext -> ext.namespace.equals(next.namespace) && ext.name.equals(next.name))) {
                result.extensions.add(next);
                mergedEntries++;
            }
        }
        return mergedEntries;
    }

    @PostMapping(
        path = "/api/-/publish",
        consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ExtensionJson publish(InputStream content,
                                 @RequestParam(name = "token") String token,
                                 @RequestParam(name = "create-publisher", defaultValue = "false") boolean createPublisher) {
        try {
            return local.publish(content, token, createPublisher);
        } catch (ErrorResultException exc) {
            return ExtensionJson.error(exc.getMessage());
        }
    }

    @PostMapping(
        path = "/api/{namespace}/{extension}/review",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ReviewResultJson postReview(@RequestBody(required = false) ReviewJson review,
                                       @PathVariable String namespace,
                                       @PathVariable String extension) {
        if (review == null) {
            return ReviewResultJson.error("No JSON input.");
        }
        if (review.rating < 0 || review.rating > 5) {
            return ReviewResultJson.error("The rating must be an integer number between 0 and 5.");
        }
        if (review.title != null && review.title.length() > REVIEW_TITLE_SIZE) {
            return ReviewResultJson.error("The title must not be longer than " + REVIEW_TITLE_SIZE + " characters.");
        }
        if (review.comment != null && review.comment.length() > REVIEW_COMMENT_SIZE) {
            return ReviewResultJson.error("The review must not be longer than " + REVIEW_COMMENT_SIZE + " characters.");
        }
        return local.postReview(review, namespace, extension);
    }

    @PostMapping(
        path = "/api/{namespace}/{extension}/review/delete",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ReviewResultJson deleteReview(@PathVariable String namespace,
                                         @PathVariable String extension) {
        return local.deleteReview(namespace, extension);
    }

}