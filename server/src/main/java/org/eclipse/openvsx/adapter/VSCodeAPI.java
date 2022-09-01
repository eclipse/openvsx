/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.adapter;

import org.eclipse.openvsx.util.NotFoundException;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class VSCodeAPI {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    LocalVSCodeService local;

    @Autowired
    UpstreamVSCodeService upstream;

    private Iterable<IVSCodeService> getVSCodeServices() {
        var registries = new ArrayList<IVSCodeService>();
        registries.add(local);
        if (upstream.isValid())
            registries.add(upstream);
        return registries;
    }

    @PostMapping(
        path = "/vscode/gallery/extensionquery",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @CrossOrigin
    public ExtensionQueryResult extensionQuery(@RequestBody ExtensionQueryParam param) {
        var size = 0;
        if(param.filters != null && !param.filters.isEmpty()) {
            size = param.filters.get(0).pageSize;
        }
        if(size <= 0) {
            size = DEFAULT_PAGE_SIZE;
        }

        var totalCount = 0L;
        var resultItem = new ExtensionQueryResult.ResultItem();
        resultItem.extensions = new ArrayList<>(size);

        var services = getVSCodeServices().iterator();
        while(resultItem.extensions.size() < size && services.hasNext()) {
            try {
                var service = services.next();
                var subResult = service.extensionQuery(param, DEFAULT_PAGE_SIZE);
                var subExtensions = subResult.results.get(0).extensions;
                if (subExtensions != null && subExtensions.size() > 0) {
                    int limit = size - resultItem.extensions.size();
                    mergeExtensionQueryResults(resultItem, subExtensions, limit);
                }

                totalCount += getTotalCount(subResult);
            } catch (NotFoundException | ResponseStatusException exc) {
                // Try the next registry
            }
        }

        return toExtensionQueryResult(resultItem, totalCount);
    }

    private void mergeExtensionQueryResults(ExtensionQueryResult.ResultItem resultItem, List<ExtensionQueryResult.Extension> extensions, int limit) {
        var previous = new ArrayList<>(resultItem.extensions);
        var extensionsIter = extensions.iterator();
        while (extensionsIter.hasNext() && resultItem.extensions.size() < limit) {
            var next = extensionsIter.next();
            var noneMatch = previous.stream()
                    .noneMatch(prev -> {
                        var prevPublisher = prev.publisher.publisherName;
                        var nextPublisher = next.publisher.publisherName;
                        return prevPublisher.equals(nextPublisher) && prev.extensionName.equals(next.extensionName);
                    });

            if (noneMatch) {
                resultItem.extensions.add(next);
            }
        }
    }

    private long getTotalCount(ExtensionQueryResult subResult) {
        return subResult.results.get(0).resultMetadata.stream()
                .filter(metadata -> metadata.metadataType.equals("ResultCount"))
                .findFirst()
                .map(metadata -> metadata.metadataItems)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(item -> item.name.equals("TotalCount"))
                .findFirst()
                .map(item -> item.count)
                .orElse(0L);
    }

    private ExtensionQueryResult toExtensionQueryResult(ExtensionQueryResult.ResultItem resultItem, long totalCount) {
        var countMetadataItem = new ExtensionQueryResult.ResultMetadataItem();
        countMetadataItem.name = "TotalCount";
        countMetadataItem.count = totalCount;

        var countMetadata = new ExtensionQueryResult.ResultMetadata();
        countMetadata.metadataType = "ResultCount";
        countMetadata.metadataItems = List.of(countMetadataItem);

        resultItem.resultMetadata = List.of(countMetadata);

        var result = new ExtensionQueryResult();
        result.results = List.of(resultItem);
        return result;
    }

    @GetMapping("/vscode/asset/{namespace}/{extensionName}/{version}/{assetType}/**")
    @CrossOrigin
    public ResponseEntity<byte[]> getAsset(
            HttpServletRequest request, @PathVariable String namespace, @PathVariable String extensionName,
            @PathVariable String version, @PathVariable String assetType,
            @RequestParam(defaultValue = TargetPlatform.NAME_UNIVERSAL) String targetPlatform
    ) {
        var restOfTheUrl = UrlUtil.extractWildcardPath(request);
        for (var service : getVSCodeServices()) {
            try {
                return service.getAsset(namespace, extensionName, version, assetType, targetPlatform, restOfTheUrl);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/vscode/item")
    @CrossOrigin
    public ModelAndView getItemUrl(@RequestParam String itemName, ModelMap model) {
        var dotIndex = itemName.indexOf('.');
        if (dotIndex < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expecting an item of the form `{publisher}.{name}`");
        }

        var namespace = itemName.substring(0, dotIndex);
        var extension = itemName.substring(dotIndex + 1);
        for (var service : getVSCodeServices()) {
            try {
                var itemUrl = service.getItemUrl(namespace, extension);
                return new ModelAndView("redirect:" + itemUrl, model);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }

        return new ModelAndView(null, HttpStatus.NOT_FOUND);
    }

    @GetMapping("/vscode/gallery/publishers/{namespace}/vsextensions/{extension}/{version}/vspackage")
    @CrossOrigin
    public ModelAndView download(
            @PathVariable String namespace, @PathVariable String extension, @PathVariable String version,
            @RequestParam(defaultValue = TargetPlatform.NAME_UNIVERSAL) String targetPlatform, ModelMap model
    ) {
        for (var service : getVSCodeServices()) {
            try {
                var downloadUrl = service.download(namespace, extension, version, targetPlatform);
                return new ModelAndView("redirect:" + downloadUrl, model);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }

        return new ModelAndView(null, HttpStatus.NOT_FOUND);
    }

    @GetMapping("/vscode/unpkg/{namespaceName}/{extensionName}/{version}/**")
    @CrossOrigin
    public ResponseEntity<byte[]> browse(
            HttpServletRequest request,
            @PathVariable String namespaceName,
            @PathVariable String extensionName,
            @PathVariable String version
    ) {
        var path = UrlUtil.extractWildcardPath(request);
        for (var service : getVSCodeServices()) {
            try {
                return service.browse(namespaceName, extensionName, version, path);
            } catch (NotFoundException exc) {
                // Try the next registry
            }
        }

        return ResponseEntity.notFound().build();
    }
}
