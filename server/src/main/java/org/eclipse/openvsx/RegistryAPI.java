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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Iterables;

import io.swagger.annotations.*;
import org.eclipse.openvsx.json.ExtensionJson;
import org.eclipse.openvsx.json.NamespaceJson;
import org.eclipse.openvsx.json.QueryParamJson;
import org.eclipse.openvsx.json.QueryResultJson;
import org.eclipse.openvsx.json.ResultJson;
import org.eclipse.openvsx.json.ReviewJson;
import org.eclipse.openvsx.json.ReviewListJson;
import org.eclipse.openvsx.json.SearchEntryJson;
import org.eclipse.openvsx.json.SearchResultJson;
import org.eclipse.openvsx.search.ISearchService;
import org.eclipse.openvsx.util.*;
import org.elasticsearch.common.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;

@RestController
public class RegistryAPI {
    private final static int REVIEW_TITLE_SIZE = 255;
    private final static int REVIEW_COMMENT_SIZE = 2048;
    private final static String VERSION_PATH_PARAM_REGEX = "(?:" + SemanticVersion.VERSION_PATH_PARAM_REGEX + ")|latest|pre-release";

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
            message = "The namespace metadata are returned in JSON format"
        ),
        @ApiResponse(
            code = 404,
            message = "The specified namespace could not be found"
        )
    })
    public ResponseEntity<NamespaceJson> getNamespace(
            @PathVariable @ApiParam(value = "Namespace name", example = "redhat")
            String namespace
        ) {
        for (var registry : getRegistries()) {
            try {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                        .body(registry.getNamespace(namespace));
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        var json = NamespaceJson.error("Namespace not found: " + namespace);
        return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
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
            message = "The extension metadata are returned in JSON format"
        ),
        @ApiResponse(
            code = 404,
            message = "The specified extension could not be found"
        )
    })
    public ResponseEntity<ExtensionJson> getExtension(
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension
        ) {
        for (var registry : getRegistries()) {
            try {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache().cachePublic())
                        .body(registry.getExtension(namespace, extension, null));
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        var json = ExtensionJson.error("Extension not found: " + namespace + "." + extension);
        return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/{targetPlatform:" + TargetPlatform.NAMES_PATH_PARAM_REGEX + "}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of the latest version of an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The extension metadata are returned in JSON format"
        ),
        @ApiResponse(
            code = 404,
            message = "The specified extension could not be found"
        )
    })
    public ResponseEntity<ExtensionJson> getExtension(
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension,
            @PathVariable @ApiParam(value = "Target platform", example = TargetPlatform.NAME_LINUX_ARM64, allowableValues = TargetPlatform.NAMES_PARAM_METADATA)
            CharSequence targetPlatform
        ) {
        for (var registry : getRegistries()) {
            try {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                        .body(registry.getExtension(namespace, extension, targetPlatform.toString()));
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        var json = ExtensionJson.error("Extension not found: " + namespace + "." + extension + " (" + targetPlatform + ")");
        return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/{version:" + VERSION_PATH_PARAM_REGEX + "}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of a specific version of an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The extension metadata are returned in JSON format"
        ),
        @ApiResponse(
            code = 404,
            message = "The specified extension could not be found"
        )
    })
    public ResponseEntity<ExtensionJson> getExtension(
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension,
            @PathVariable @ApiParam(value = "Extension version", example = "0.65.0")
            String version
        ) {
        for (var registry : getRegistries()) {
            try {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.noCache().cachePublic())
                        .body(registry.getExtension(namespace, extension, null, version));
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        var json = ExtensionJson.error("Extension not found: " + namespace + "." + extension + " " + version);
        return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
    }

    @GetMapping(
        path = "/api/{namespace}/{extension}/{targetPlatform:" + TargetPlatform.NAMES_PATH_PARAM_REGEX + "}/{version:" + VERSION_PATH_PARAM_REGEX + "}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of a specific version of an extension")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The extension metadata are returned in JSON format"
        ),
        @ApiResponse(
            code = 404,
            message = "The specified extension could not be found"
        )
    })
    public ResponseEntity<ExtensionJson> getExtension(
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension,
            @PathVariable @ApiParam(value = "Target platform", example = TargetPlatform.NAME_LINUX_ARM64, allowableValues = TargetPlatform.NAMES_PARAM_METADATA)
            String targetPlatform,
            @PathVariable @ApiParam(value = "Extension version", example = "0.65.0")
            String version
        ) {
        for (var registry : getRegistries()) {
            try {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                        .body(registry.getExtension(namespace, extension, targetPlatform, version));
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        var json = ExtensionJson.error("Extension not found: " + namespace + "." + extension + " " + version + " (" + targetPlatform + ")");
        return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
    }

    @GetMapping("/api/{namespace}/{extension}/{version:" + VERSION_PATH_PARAM_REGEX + "}/file/**")
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
    public ResponseEntity<byte[]> getFile(
            HttpServletRequest request,
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension,
            @PathVariable @ApiParam(value = "Extension version", example = "0.65.0")
            String version
    ) {
        var fileName = UrlUtil.extractWildcardPath(request, "/api/{namespace}/{extension}/{version}/file/**");
        for (var registry : getRegistries()) {
            try {
                return registry.getFile(namespace, extension, TargetPlatform.NAME_UNIVERSAL, version, fileName);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        throw new NotFoundException();
    }

    @GetMapping("/api/{namespace}/{extension}/{targetPlatform:" + TargetPlatform.NAMES_PATH_PARAM_REGEX + "}/{version:" + VERSION_PATH_PARAM_REGEX + "}/file/**")
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
    public ResponseEntity<byte[]> getFile(
            HttpServletRequest request,
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension,
            @PathVariable @ApiParam(value = "Target platform", example = TargetPlatform.NAME_LINUX_ARM64, allowableValues = TargetPlatform.NAMES_PARAM_METADATA)
            String targetPlatform,
            @PathVariable @ApiParam(value = "Extension version", example = "0.65.0")
            String version
        ) {
        var fileName = UrlUtil.extractWildcardPath(request, "/api/{namespace}/{extension}/{targetPlatform}/{version}/file/**");
        for (var registry : getRegistries()) {
            try {
                return registry.getFile(namespace, extension, targetPlatform, version, fileName);
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
            message = "The reviews are returned in JSON format"
        ),
        @ApiResponse(
            code = 404,
            message = "The specified extension could not be found"
        )
    })
    public ResponseEntity<ReviewListJson> getReviews(
            @PathVariable @ApiParam(value = "Extension namespace", example = "redhat")
            String namespace,
            @PathVariable @ApiParam(value = "Extension name", example = "java")
            String extension
        ) {
        for (var registry : getRegistries()) {
            try {
                return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                        .body(registry.getReviews(namespace, extension));
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }
        var json = ReviewListJson.error("Extension not found: " + namespace + "." + extension);
        return new ResponseEntity<>(json, HttpStatus.NOT_FOUND);
    }

    @GetMapping(
        path = "/api/-/search",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Search extensions via text entered by a user")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "The search results are returned in JSON format"
        ),
        @ApiResponse(
            code = 400,
            message = "The request contains an invalid parameter value"
        )
    })
    public ResponseEntity<SearchResultJson> search(
            @RequestParam(required = false)
            @ApiParam(value = "Query text for searching", example = "javascript")
            String query,
            @RequestParam(required = false)
            @ApiParam(value = "Extension category as shown in the UI", example = "Programming Languages")
            String category,
            @RequestParam(required = false)
            @ApiParam(value = "Target platform", example = TargetPlatform.NAME_LINUX_ARM64, allowableValues = TargetPlatform.NAMES_PARAM_METADATA)
            String targetPlatform,
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
            var json = SearchResultJson.error("The parameter 'size' must not be negative.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        if (offset < 0) {
            var json = SearchResultJson.error("The parameter 'offset' must not be negative.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }

        var options = new ISearchService.Options(query, category, targetPlatform, size, offset, sortOrder, sortBy, includeAllVersions);
        var result = new SearchResultJson();
        result.extensions = new ArrayList<>(size);
        for (var registry : getRegistries()) {
            if (result.extensions.size() >= size) {
                return ResponseEntity.ok(result);
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
                return exc.toResponseEntity(SearchResultJson.class);
            }
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(result);
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

    @GetMapping(
        path = "/api/-/query",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of extensions matching the given parameters")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Returns the (possibly empty) query results"
        ),
        @ApiResponse(
            code = 400,
            message = "The request contains an invalid parameter value"
        )
    })
    public ResponseEntity<QueryResultJson> getQuery(
            @RequestParam(required = false)
            @ApiParam(value = "Name of a namespace", example = "foo")
            String namespaceName,
            @RequestParam(required = false)
            @ApiParam(value = "Name of an extension", example = "bar")
            String extensionName,
            @RequestParam(required = false)
            @ApiParam(value = "Version of an extension", example = "1")
            String extensionVersion,
            @RequestParam(required = false)
            @ApiParam(value = "Identifier in the form {namespace}.{extension}", example = "foo.bar")
            String extensionId,
            @RequestParam(required = false)
            @ApiParam(value = "Universally unique identifier of an extension", example = "5678")
            String extensionUuid,
            @RequestParam(required = false)
            @ApiParam(value = "Universally unique identifier of a namespace", example = "1234")
            String namespaceUuid,
            @RequestParam(required = false)
            @ApiParam(value = "Whether to include all versions of an extension, ignored if extensionVersion is specified")
            boolean includeAllVersions,
            @RequestParam(required = false)
            @ApiParam(value = "Target platform", example = TargetPlatform.NAME_LINUX_X64, allowableValues = TargetPlatform.NAMES_PARAM_METADATA)
            String targetPlatform
        ) {
        var param = new QueryParamJson();
        param.namespaceName = namespaceName;
        param.extensionName = extensionName;
        param.extensionVersion = extensionVersion;
        param.extensionId = extensionId;
        param.extensionUuid = extensionUuid;
        param.namespaceUuid = namespaceUuid;
        param.includeAllVersions = includeAllVersions;
        param.targetPlatform = targetPlatform;

        var result = new QueryResultJson();
        for (var registry : getRegistries()) {
            try {
                var subResult = registry.query(param);
                if (subResult.extensions != null) {
                    if (result.extensions == null)
                        result.extensions = subResult.extensions;
                    else
                        result.extensions.addAll(subResult.extensions);
                }
            } catch (NotFoundException exc) {
                // Try the next registry
            } catch (ErrorResultException exc) {
                return exc.toResponseEntity(QueryResultJson.class);
            }
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(result);
    }

    @PostMapping(
        path = "/api/-/query",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    @ApiOperation("Provides metadata of extensions matching the given parameters")
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Returns the (possibly empty) query results"
        ),
        @ApiResponse(
            code = 400,
            message = "The request contains an invalid parameter value"
        )
    })
    public ResponseEntity<QueryResultJson> postQuery(
            @RequestBody @ApiParam("Parameters of the metadata query")
            QueryParamJson param
        ) {
        var result = new QueryResultJson();
        for (var registry : getRegistries()) {
            try {
                var subResult = registry.query(param);
                if (subResult.extensions != null) {
                    if (result.extensions == null)
                        result.extensions = subResult.extensions;
                    else
                        result.extensions.addAll(subResult.extensions);
                }
            } catch (NotFoundException exc) {
                // Try the next registry
            } catch (ErrorResultException exc) {
                return exc.toResponseEntity(QueryResultJson.class);
            }
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .body(result);
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
            examples = @Example(@ExampleProperty(value="{ \"success\": \"Created namespace foobar\" }", mediaType = "application/json")),
            responseHeaders = @ResponseHeader(
                name = "Location",
                description = "The URL of the namespace metadata",
                response = String.class
            )
        ),
        @ApiResponse(
            code = 400,
            message = "The namespace could not be created",
            examples = @Example(@ExampleProperty(value="{ \"error\": \"Invalid access token.\" }", mediaType = "application/json"))
        )
    })
    public ResponseEntity<ResultJson> createNamespace(
            @RequestBody @ApiParam("Describes the namespace to create")
            NamespaceJson namespace,
            @RequestParam @ApiParam("A personal access token")
            String token
        ) {
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
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(url))
                    .body(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity();
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
            message = "Successfully published the extension",
            responseHeaders = @ResponseHeader(
                name = "Location",
                description = "The URL of the extension metadata",
                response = String.class
            )
        ),
        @ApiResponse(
            code = 400,
            message = "The extension could not be published",
            examples = @Example(@ExampleProperty(value="{ \"error\": \"Invalid access token.\" }", mediaType = "application/json"))
        )
    })
    public ResponseEntity<ExtensionJson> publish(
            InputStream content,
            @RequestParam @ApiParam("A personal access token") String token
        ) {
        try {
            var json = local.publish(content, token);
            var serverUrl = UrlUtil.getBaseUrl();
            var url = UrlUtil.createApiVersionUrl(serverUrl, json);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .location(URI.create(url))
                    .body(json);
        } catch (ErrorResultException exc) {
            return exc.toResponseEntity(ExtensionJson.class);
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
            var json = ResultJson.error("No JSON input.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        if (review.rating < 0 || review.rating > 5) {
            var json = ResultJson.error("The rating must be an integer number between 0 and 5.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        if (review.title != null && review.title.length() > REVIEW_TITLE_SIZE) {
            var json = ResultJson.error("The title must not be longer than " + REVIEW_TITLE_SIZE + " characters.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        if (review.comment != null && review.comment.length() > REVIEW_COMMENT_SIZE) {
            var json = ResultJson.error("The review must not be longer than " + REVIEW_COMMENT_SIZE + " characters.");
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
        var json = local.postReview(review, namespace, extension);
        if (json.error == null) {
            return new ResponseEntity<>(json, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(
        path = "/api/{namespace}/{extension}/review/delete",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiIgnore
    public ResponseEntity<ResultJson> deleteReview(@PathVariable String namespace,
                                                   @PathVariable String extension) {
        var json = local.deleteReview(namespace, extension);
        if (json.error == null) {
            return ResponseEntity.ok(json);
        } else {
            return new ResponseEntity<>(json, HttpStatus.BAD_REQUEST);
        }
    }

}