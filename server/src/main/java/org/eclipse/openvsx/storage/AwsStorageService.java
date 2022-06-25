/********************************************************************************
 * Copyright (c) 2022 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.openvsx.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.util.TargetPlatform;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AwsStorageService implements IStorageService {

    @Value("${ovsx.storage.aws.access-key-id:}")
    String accessKeyId;

    @Value("${ovsx.storage.aws.secret-access-key:}")
    String secretAccessKey;

    @Value("${ovsx.storage.aws.region:}")
    String region;

    @Value("${ovsx.storage.aws.service-endpoint:}")
    String serviceEndpoint;

    @Value("${ovsx.storage.aws.bucket:}")
    String bucket;

    @Value("${ovsx.storage.aws.path-style-access:false}")
    boolean pathStyleAccess;

    private AmazonS3 s3Client;

    protected AmazonS3 getS3Client() {
        if (s3Client == null) {
            var credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
            this.s3Client = AmazonS3ClientBuilder.standard()
                .withPathStyleAccessEnabled(pathStyleAccess)
                .withEndpointConfiguration(new EndpointConfiguration(serviceEndpoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
        }
        return s3Client;
    }

    @Override
    public boolean isEnabled() {
        return !Strings.isNullOrEmpty(serviceEndpoint);
    }

    @Override
    public void uploadFile(FileResource resource) {
        var client = getS3Client();
        var key = getObjectKey(resource);
        var stream = new ByteArrayInputStream(resource.getContent());
        var resourceName = resource.getName();

        var metadata = new ObjectMetadata();
        metadata.setContentLength(resource.getContent().length);
        metadata.setContentType(StorageUtil.getFileType(resourceName).toString());

        if (resourceName.endsWith(".vsix")) {
            metadata.setContentDisposition("attachment; filename=\"" + resourceName + "\"");
        } else {
            metadata.setCacheControl(StorageUtil.getCacheControl(resourceName).getHeaderValue());
        }
        client.putObject(bucket, key, stream, metadata);
    }

    @Override
    public void removeFile(FileResource resource) {
        var client = getS3Client();
        client.deleteObject(bucket, getObjectKey(resource));
    }

    @Override
    public URI getLocation(FileResource resource) {
        var client = getS3Client();
        var objectKey = getObjectKey(resource);

        try {
            return client.generatePresignedUrl(bucket, objectKey, null).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getObjectKey(FileResource resource) {
        var extensionVersion = resource.getExtension();
        var extension = extensionVersion.getExtension();
        var namespace = extension.getNamespace();
        var segments = new String[] {namespace.getName(), extension.getName()};
        if (!TargetPlatform.isUniversal(extensionVersion)) {
            segments = ArrayUtils.add(segments, extensionVersion.getTargetPlatform());
        }

        segments = ArrayUtils.add(segments, extensionVersion.getVersion());
        segments = ArrayUtils.addAll(segments, resource.getName().split("/"));
        return UrlUtil.createApiUrl("", segments).substring(1); // remove first '/'
    }
}
