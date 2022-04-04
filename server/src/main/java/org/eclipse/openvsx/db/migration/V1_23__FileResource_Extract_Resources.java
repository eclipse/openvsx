/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */

package org.eclipse.openvsx.db.migration;

import org.eclipse.openvsx.ExtensionProcessor;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.storage.AzureBlobStorageService;
import org.eclipse.openvsx.storage.GoogleCloudStorageService;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class V1_23__FileResource_Extract_Resources extends BaseJavaMigration {

    private static final String COL_FR_ID = "fr_id";
    private static final String COL_FR_NAME = "fr_name";
    private static final String COL_FR_TYPE = "fr_type";
    private static final String COL_FR_CONTENT = "fr_content";
    private static final String COL_FR_STORAGE_TYPE = "fr_storage_type";
    private static final String COL_EV_ID = "ev_id";
    private static final String COL_EV_VERSION = "ev_version";
    private static final String COL_E_NAME = "e_name";
    private static final String COL_N_NAME = "n_name";

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    GoogleCloudStorageService googleStorage;

    @Autowired
    AzureBlobStorageService azureStorage;

    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        var downloads = getAllDownloads(connection);
        var resources = extractResources(downloads);
        uploadResources(resources);
        deleteResources(connection);
        insertResources(resources, connection);
        deleteWebResources(connection);
    }

    private List<FileResource> getAllDownloads(Connection connection) throws SQLException {
        var query = "SELECT " +
                "fr.id " + COL_FR_ID + ", " +
                "fr.name " + COL_FR_NAME + ", " +
                "fr.type " + COL_FR_TYPE + ", " +
                "fr.content " + COL_FR_CONTENT + ", " +
                "fr.storage_type " + COL_FR_STORAGE_TYPE + ", " +
                "ev.id " + COL_EV_ID + ", " +
                "ev.version " + COL_EV_VERSION + ", " +
                "e.name " + COL_E_NAME + ", " +
                "n.name " + COL_N_NAME + " " +
                "FROM file_resource fr " +
                "JOIN extension_version ev ON ev.id = fr.extension_id " +
                "JOIN extension e ON e.id = ev.extension_id " +
                "JOIN namespace n ON n.id = e.namespace_id " +
                "WHERE fr.type = 'download'";

        var downloads = new ArrayList<FileResource>();
        try(var statement = connection.prepareStatement(query)) {
            try(var result = statement.executeQuery()) {
                while(result.next()) {
                    downloads.add(toFileResource(result));
                }
            }
        }

        return downloads;
    }

    private FileResource toFileResource(ResultSet result) throws SQLException {
        var namespace = new Namespace();
        namespace.setName(result.getString(COL_N_NAME));

        var extension = new Extension();
        extension.setName(result.getString(COL_E_NAME));
        extension.setNamespace(namespace);

        var extVersion = new ExtensionVersion();
        extVersion.setId(result.getLong(COL_EV_ID));
        extVersion.setVersion(result.getString(COL_EV_VERSION));
        extVersion.setExtension(extension);

        var resource = new FileResource();
        resource.setId(result.getLong(COL_FR_ID));
        resource.setName(result.getString(COL_FR_NAME));
        resource.setType(result.getString(COL_FR_TYPE));
        resource.setContent(result.getBytes(COL_FR_CONTENT));
        resource.setStorageType(result.getString(COL_FR_STORAGE_TYPE));
        resource.setExtension(extVersion);

        return resource;
    }

    private List<FileResource> extractResources(List<FileResource> downloads) {
        var storages = Map.of(
                FileResource.STORAGE_GOOGLE, googleStorage,
                FileResource.STORAGE_AZURE, azureStorage
        );

        var resources = new ArrayList<FileResource>();
        for(var download : downloads) {
            byte[] content;
            if(download.getStorageType().equals(FileResource.STORAGE_DB)) {
                content = download.getContent();
            } else {
                var storage = storages.get(download.getStorageType());
                var uri = storage.getLocation(download);
                content = restTemplate.getForObject(uri, byte[].class);
            }

            try(var processor = new ExtensionProcessor(new ByteArrayInputStream(content))) {
                var processedResources = processor.getResources(download.getExtension()).stream()
                        .filter(resource -> resource.getType().equals(FileResource.RESOURCE))
                        .map(resource -> {
                            resource.setStorageType(download.getStorageType());
                            return resource;
                        })
                        .collect(Collectors.toList());

                resources.addAll(processedResources);
            }
        }

        return resources;
    }

    private void uploadResources(List<FileResource> resources) {
        var storages = Map.of(
                FileResource.STORAGE_GOOGLE, googleStorage,
                FileResource.STORAGE_AZURE, azureStorage
        );

        for(var resource : resources) {
            if(!resource.getStorageType().equals(FileResource.STORAGE_DB)) {
                var storage = storages.get(resource.getStorageType());
                storage.uploadFile(resource);
                resource.setContent(null);
            }
        }
    }

    private void insertResources(List<FileResource> resources, Connection connection) throws SQLException {
        var query = "INSERT INTO file_resource(id, type, content, extension_id, name, storage_type) VALUES(nextval('hibernate_sequence'), ?, ?, ?, ?, ?)";
        try(var statement = connection.prepareStatement(query)) {
            for(var resource : resources) {
                statement.setString(1, resource.getType());
                statement.setBytes(2, resource.getContent());
                statement.setLong(3, resource.getExtension().getId());
                statement.setString(4, resource.getName());
                statement.setString(5, resource.getStorageType());
                statement.executeUpdate();
            }
        }
    }

    private void deleteResources(Connection connection) throws SQLException {
        var query = "DELETE FROM file_resource WHERE type = 'resource'";
        try(var statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
        }
    }

    private void deleteWebResources(Connection connection) throws SQLException {
        var query = "DELETE FROM file_resource WHERE type = 'web-resource'";
        try(var statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
        }
    }
}
