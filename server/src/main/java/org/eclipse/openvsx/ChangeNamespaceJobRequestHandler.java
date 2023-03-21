/** ******************************************************************************
 * Copyright (c) 2023 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx;

import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.storage.StorageUtilService;
import org.eclipse.openvsx.util.ErrorResultException;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ChangeNamespaceJobRequestHandler implements JobRequestHandler<ChangeNamespaceJobRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeNamespaceJobRequestHandler.class);

    private static final Map<String, Object> LOCKS;

    static {
        var MAX_SIZE = 100;
        LOCKS = Collections.synchronizedMap(new LinkedHashMap<>(MAX_SIZE) {
            protected boolean removeEldestEntry(Map.Entry eldest){
                return size() > MAX_SIZE;
            }
        });
    }

    @Autowired
    ExtensionValidator validator;

    @Autowired
    RepositoryService repositories;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    ChangeNamespaceService service;

    @Override
    public void run(ChangeNamespaceJobRequest jobRequest) throws Exception {
        var oldNamespace = jobRequest.getData().oldNamespace;
        synchronized (LOCKS.computeIfAbsent(oldNamespace, key -> new Object())) {
            execute(jobRequest);
        }
    }

    private void execute(ChangeNamespaceJobRequest jobRequest) {
        var json = jobRequest.getData();
        LOGGER.info(">> Change namespace from {} to {}", json.oldNamespace, json.newNamespace);
        var oldNamespace = repositories.findNamespace(json.oldNamespace);
        if(oldNamespace == null) {
            return;
        }

        var oldResources = repositories.findFileResources(oldNamespace);
        var newNamespaceOptional = Optional.ofNullable(repositories.findNamespace(json.newNamespace));
        var createNewNamespace = newNamespaceOptional.isEmpty();
        var newNamespace = newNamespaceOptional.orElseGet(() -> {
            validateNamespace(json.newNamespace);
            var namespace = new Namespace();
            namespace.setName(json.newNamespace);
            return namespace;
        });

        var copyResources = oldResources.stream()
                .findFirst()
                .map(storageUtil::shouldStoreExternally)
                .orElse(false);

        List<Pair<FileResource, FileResource>> pairs = null;
        List<FileResource> updatedResources;
        if(copyResources) {
            pairs = copyResources(oldResources, newNamespace);
            storageUtil.copyFiles(pairs);
            updatedResources = pairs.stream()
                    .filter(pair -> pair.getFirst().getType().equals(FileResource.DOWNLOAD))
                    .map(pair -> {
                        var oldResource = pair.getFirst();
                        var newResource = pair.getSecond();
                        oldResource.setName(newResource.getName());
                        return oldResource;
                    })
                    .collect(Collectors.toList());
        } else {
            updatedResources = oldResources
                    .filter(resource -> resource.getType().equals(FileResource.DOWNLOAD))
                    .map(resource -> {
                        resource.setName(newResourceName(newNamespace, resource));
                        return resource;
                    })
                    .toList();
        }

        service.changeNamespaceInDatabase(newNamespace, oldNamespace, updatedResources, createNewNamespace, json.removeOldNamespace);
        if(copyResources) {
            // remove the old resources from external storage
            pairs.stream()
                    .map(Pair::getFirst)
                    .forEach(storageUtil::removeFile);
        }
        LOGGER.info("<< Changed namespace from {} to {}", json.oldNamespace, json.newNamespace);
    }

    private void validateNamespace(String namespace) {
        var namespaceIssue = validator.validateNamespace(namespace);
        if (namespaceIssue.isPresent()) {
            throw new ErrorResultException(namespaceIssue.get().toString());
        }
    }

    private List<Pair<FileResource, FileResource>> copyResources(Streamable<FileResource> resources, Namespace newNamespace) {
        var extVersions = resources.stream()
                .map(FileResource::getExtension)
                .collect(Collectors.toMap(ExtensionVersion::getId, ev -> ev, (ev1, ev2) -> ev1));

        var extensions = extVersions.values().stream()
                .map(ExtensionVersion::getExtension)
                .collect(Collectors.groupingBy(Extension::getId))
                .entrySet().stream()
                .map(entry -> {
                    var extension = entry.getValue().get(0);
                    var newExtension = new Extension();
                    newExtension.setId(extension.getId());
                    newExtension.setName(extension.getName());
                    newExtension.setNamespace(newNamespace);
                    return new AbstractMap.SimpleEntry<>(entry.getKey(), newExtension);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for(var key : extVersions.keySet()) {
            var extVersion = extVersions.get(key);
            var newExtVersion = new ExtensionVersion();
            newExtVersion.setId(extVersion.getId());
            newExtVersion.setExtension(extensions.get(extVersion.getExtension().getId()));
            newExtVersion.setVersion(extVersion.getVersion());
            newExtVersion.setTargetPlatform(extVersion.getTargetPlatform());
            extVersions.put(key, newExtVersion);
        }

        return resources.stream()
                .map(resource -> {
                    var newExtVersion = extVersions.get(resource.getExtension().getId());
                    var newResource = new FileResource();
                    newResource.setId(resource.getId());
                    newResource.setExtension(newExtVersion);
                    newResource.setType(resource.getType());
                    newResource.setStorageType(resource.getStorageType());
                    var newResourceName = resource.getType().equals(FileResource.DOWNLOAD)
                            ? newResourceName(newNamespace, resource)
                            : resource.getName();

                    newResource.setName(newResourceName);
                    return Pair.of(resource, newResource);
                })
                .collect(Collectors.toList());
    }

    private String newResourceName(Namespace newNamespace, FileResource resource) {
        var extVersion = resource.getExtension();
        var extension = extVersion.getExtension();

        var newExtension = new Extension();
        newExtension.setNamespace(newNamespace);
        newExtension.setName(extension.getName());

        var newExtVersion = new ExtensionVersion();
        newExtVersion.setVersion(extVersion.getVersion());
        newExtVersion.setTargetPlatform(extVersion.getTargetPlatform());
        newExtVersion.setExtension(newExtension);
        try(var processor = new ExtensionProcessor(null)) {
            var newResourceName = processor.getBinaryName(newExtVersion);
            LOGGER.info("newResourceName: {}", newResourceName);
            return newResourceName;
        }
    }
}
