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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Iterables;

import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.search.SearchService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.UrlUtil;
import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
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

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import io.swagger.annotations.ResponseHeader;
import springfox.documentation.annotations.ApiIgnore;

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
    @ApiOperation("Provides metadata of a namespace")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The 'error' property indicates whether the request failed"
        )
    })
    public NamespaceJson getNamespace(@PathVariable @ApiParam(value = "Namespace name", example = "redhat")
                                      String namespace) {
        for (var registry : getRegistries()) {
            try {
                return registry.getNamespace(namespace);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        return NamespaceJson.error("Namespace not found: " + namespace);
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of the latest version of an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The 'error' property indicates whether the request failed"
        )
    })
    public ExtensionJson getExtension(@PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
                                      String namespace,
                                      @PathVariable @ApiParam(value = "Extension name", example = "java")
                                      String extension) {
        for (var registry : getRegistries()) {
            try {
                return registry.getExtension(namespace, extension);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        return ExtensionJson.error("Extension not found: " + namespace + "." + extension);
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/{version}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of a specific version of an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The 'error' property indicates whether the request failed"
        )
    })
    public ExtensionJson getExtension(@PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
                                      String namespace,
                                      @PathVariable @ApiParam(value = "Extension name", example = "java")
                                      String extension,
                                      @PathVariable @ApiParam(value = "Extension version", example = "0.65.0")
                                      String version) {
        for (var registry : getRegistries()) {
            try {
                return registry.getExtension(namespace, extension, version);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        return ExtensionJson.error("Extension not found: " + namespace + "." + extension + " version " + version);
    }

    @GetMapping("/api/{namespace}/{extension}/{version}/file/{fileName:.+}")
    @CrossOrigin
    @ApiOperation("Access a file packaged by an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The file content is returned"
        ),
        @ApiResponse(
            code = 302,
            message = "The file is found at the specified location",
            responseHeaders = @ResponseHeader(
                name = "Location",
                description = "The actual URL where the file can be accessed",
                response = String.class
            )
        ),
        @ApiResponse(
            code = 404,
            message = "The specified file could not be found"
        )
    })
    public ResponseEntity<byte[]> getFile(@PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
                                          String namespace,
                                          @PathVariable @ApiParam(value = "Extension name", example = "java")
                                          String extension,
                                          @PathVariable @ApiParam(value = "Extension version", example = "0.65.0")
                                          String version,
                                          @PathVariable @ApiParam(value = "Name of the file to access", example = "LICENSE.txt")
                                          String fileName) {
        for (var registry : getRegistries()) {
            try {
                return registry.getFile(namespace, extension, version, fileName);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/reviews",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Returns the list of reviews of an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The 'error' property indicates whether the request failed"
        )
    })
    public ReviewListJson getReviews(@PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
                                     String namespace,
                                     @PathVariable @ApiParam(value = "Extension name", example = "java")
                                     String extension) {
        for (var registry : getRegistries()) {
            try {
                return registry.getReviews(namespace, extension);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        return ReviewListJson.error("Extension not found: " + namespace + "." + extension);
    }

    @GetMapping(
        path = "/api/-/search",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Search extensions via a query string")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The 'error' property indicates whether the request failed"
        )
    })
    public SearchResultJson search(
            @RequestParam(required = false)
            @ApiParam(value = "Query string for searching", example = "javascript")
            String query,
            @RequestParam(required = false)
            @ApiParam(value = "Extension category as shown in the UI", example = "Programming Languages")
            String category,
            @RequestParam(defaultValue = "18")
            @ApiParam(value = "Maximal number of entries to return", allowableValues = "range[0,infinity]")
            int size,
            @RequestParam(defaultValue = "0")
            @ApiParam(value = "Number of entries to skip (usually a multiple of the page size)", allowableValues = "range[0,infinity]")
            int offset,
            @RequestParam(defaultValue = "desc") 
            @ApiParam(value = "Descending or ascending sort order", allowableValues = "asc,desc")
            String sortOrder,
            @RequestParam(defaultValue = "relevance")
            @ApiParam(value = "Sort key (relevance is a weighted mix of various properties)", allowableValues = "relevance,timestamp,averageRating,downloadCount")
            String sortBy,
            @RequestParam(required = false)
            @ApiParam(value = "Whether to include information on all available versions for each returned entry")
            boolean includeAllVersions
        ) {
        if (size < 0) {
            return SearchResultJson.error("The parameter 'size' must not be negative.");
        }
        if (offset < 0) {
            return SearchResultJson.error("The parameter 'offset' must not be negative.");
        }

        var options = new SearchService.Options(query, category, size, offset, sortOrder, sortBy, includeAllVersions);
        var result = new SearchResultJson();
        result.extensions = new ArrayList<>(size);
        for (var registry : getRegistries()) {
            if (result.extensions.size() >= size) {
                return result;
            }
            try {
                var subResult = registry.search(options);
                if (subResult.extensions != null && subResult.extensions.size() > 0) {
                    int limit = size - result.extensions.size();
                    var subResultSize = mergeSearchResults(result, subResult.extensions, limit);
                    result.offset += subResult.offset;
                    offset = Math.max(offset - subResult.offset - subResultSize, 0);
                }
                result.totalSize += subResult.totalSize;
            } catch (NotFoundException exc) {
                // Try the next registry
            } catch (ErrorResultException exc) {
                return SearchResultJson.error(exc.getMessage());
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
        path = "/api/-/namespace/create",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation("Create a namespace")
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = "Successfully created the namespace",
            examples = @Example(@ExampleProperty(value="{ \"success\": \"Created namespace foobar\" }", mediaType = "application/json"))
        ),
        @ApiResponse(
            code = 200,
            message = "The namespace could not be created",
            examples = @Example(@ExampleProperty(value="{ \"error\": \"Invalid access token.\" }", mediaType = "application/json"))
        )
    })
    public ResponseEntity<ResultJson> createNamespace(@RequestBody(required = false) @ApiParam("Describes the namespace to create")
                                      NamespaceJson namespace,
                                      @RequestParam @ApiParam("A personal access token")
                                      String token) {
        if (namespace == null) {
            return ResponseEntity.ok(ResultJson.error("No JSON input."));
        }
        if (Strings.isNullOrEmpty(namespace.name)) {
            return ResponseEntity.ok(ResultJson.error("Missing required property 'name'."));
        }
        try {
            var json = local.createNamespace(namespace, token);
            var serverUrl = UrlUtil.getBaseUrl();
            var url = UrlUtil.createApiUrl(serverUrl, "api", namespace.name);
            return new ResponseEntity<>(json, location(url), HttpStatus.CREATED);
        } catch (ErrorResultException exc) {
            return ResponseEntity.ok(ResultJson.error(exc.getMessage()));
        }
    }

    @PostMapping(
        path = "/api/-/publish",
        consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation("Publish an extension by uploading a vsix file")
    @ApiImplicitParams({
        @ApiImplicitParam(
            name = "content",
            paramType = "body",
            value = "Uploaded vsix file to publish",
            required = true
        )
    })
    @ApiResponses({
        @ApiResponse(
            code = 201,
            message = "Successfully published the extension"
        ),
        @ApiResponse(
            code = 200,
            message = "The extension could not be published",
            examples = @Example(@ExampleProperty(value="{ \"error\": \"Invalid access token.\" }", mediaType = "application/json"))
        )
    })
    public ResponseEntity<ExtensionJson> publish(InputStream content,
                                 @RequestParam @ApiParam("A personal access token") String token) {
        try {
            var json = local.publish(content, token);
            var serverUrl = UrlUtil.getBaseUrl();
            var url = UrlUtil.createApiUrl(serverUrl, "api", json.namespace, json.name, json.version);
            return new ResponseEntity<>(json, location(url), HttpStatus.CREATED);
        } catch (ErrorResultException exc) {
            return ResponseEntity.ok(ExtensionJson.error(exc.getMessage()));
        }
    }

    @PostMapping(
        path = "/api/{namespace}/{extension}/review",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiIgnore
    public ResponseEntity<ResultJson> postReview(@RequestBody(required = false) ReviewJson review,
                                 @PathVariable String namespace,
                                 @PathVariable String extension) {
        if (review == null) {
            return ResponseEntity.ok(ResultJson.error("No JSON input."));
        }
        if (review.rating < 0 || review.rating > 5) {
            return ResponseEntity.ok(ResultJson.error("The rating must be an integer number between 0 and 5."));
        }
        if (review.title != null && review.title.length() > REVIEW_TITLE_SIZE) {
            return ResponseEntity.ok(ResultJson.error("The title must not be longer than " + REVIEW_TITLE_SIZE + " characters."));
        }
        if (review.comment != null && review.comment.length() > REVIEW_COMMENT_SIZE) {
            return ResponseEntity.ok(ResultJson.error("The review must not be longer than " + REVIEW_COMMENT_SIZE + " characters."));
        }
        var json = local.postReview(review, namespace, extension);
        if (json.error == null) {
            return new ResponseEntity<>(json, HttpStatus.CREATED);
        } else {
            return ResponseEntity.ok(json);
        }
    }

    @PostMapping(
        path = "/api/{namespace}/{extension}/review/delete",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiIgnore
    public ResultJson deleteReview(@PathVariable String namespace,
                                   @PathVariable String extension) {
        return local.deleteReview(namespace, extension);
    }

    private HttpHeaders location(String value) {
        try {
            var headers = new HttpHeaders();
			headers.setLocation(new URI(value));
            return headers;
		} catch (URISyntaxException exc) {
			throw new RuntimeException(exc);
		}
    }

}