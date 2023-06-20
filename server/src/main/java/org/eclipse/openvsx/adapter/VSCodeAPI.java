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

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;

@RestController
public class VSCodeAPI {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    LocalVSCodeService local;

    @Autowired
    UpstreamVSCodeService upstream;

    @Autowired
    IExtensionQueryRequestHandler extensionQueryRequestHandler;

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

        return extensionQueryRequestHandler.getResult(param, size, DEFAULT_PAGE_SIZE);
    }

    @GetMapping("/vscode/asset/{namespaceName}/{extensionName}/{version}/{assetType}/**")
    @CrossOrigin
    public ResponseEntity<byte[]> getAsset(
            HttpServletRequest request, @PathVariable String namespaceName, @PathVariable String extensionName,
            @PathVariable String version, @PathVariable String assetType,
            @RequestParam(defaultValue = TargetPlatform.NAME_UNIVERSAL) String targetPlatform
    ) {
        var restOfTheUrl = UrlUtil.extractWildcardPath(request);
        for (var service : getVSCodeServices()) {
            try {
                return service.getAsset(namespaceName, extensionName, version, assetType, targetPlatform, restOfTheUrl);
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

    @GetMapping("/vscode/gallery/publishers/{namespaceName}/vsextensions/{extensionName}/{version}/vspackage")
    @CrossOrigin
    public ModelAndView download(
            @PathVariable String namespaceName, @PathVariable String extensionName, @PathVariable String version,
            @RequestParam(defaultValue = TargetPlatform.NAME_UNIVERSAL) String targetPlatform, ModelMap model
    ) {
        for (var service : getVSCodeServices()) {
            try {
                var downloadUrl = service.download(namespaceName, extensionName, version, targetPlatform);
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