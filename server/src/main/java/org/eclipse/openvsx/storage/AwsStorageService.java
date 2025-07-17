/********************************************************************************
 * Copyright (c) 2022 Marshall Walker and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/

package org.eclipse.openvsx.storage;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.cache.FilesCacheKeyGenerator;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.entities.Namespace;
import org.eclipse.openvsx.util.FileUtil;
import org.eclipse.openvsx.util.TempFile;
import org.eclipse.openvsx.util.UrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;

import static org.eclipse.openvsx.cache.CacheService.CACHE_EXTENSION_FILES;
import static org.eclipse.openvsx.cache.CacheService.GENERATOR_FILES;

@Component
public class AwsStorageService implements IStorageService {

    private final FileCacheDurationConfig fileCacheDurationConfig;
    private final FilesCacheKeyGenerator filesCacheKeyGenerator;

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

    private S3Client s3Client;

    public AwsStorageService(FileCacheDurationConfig fileCacheDurationConfig, FilesCacheKeyGenerator filesCacheKeyGenerator) {
        this.fileCacheDurationConfig = fileCacheDurationConfig;
        this.filesCacheKeyGenerator = filesCacheKeyGenerator;
    }

    protected S3Client getS3Client() {
        if (s3Client == null) {
            var credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            var s3ClientBuilder = S3Client.builder()
                    .defaultsMode(DefaultsMode.STANDARD)
                    .forcePathStyle(pathStyleAccess)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region));

            if(StringUtils.isNotEmpty(serviceEndpoint)) {
                var endpointParams = S3EndpointParams.builder()
                        .endpoint(serviceEndpoint)
                        .region(Region.of(region))
                        .build();

                var endpoint = S3EndpointProvider
                        .defaultProvider()
                        .resolveEndpoint(endpointParams).join();

                s3ClientBuilder = s3ClientBuilder.endpointOverride(endpoint.url());
            }

            s3Client = s3ClientBuilder.build();
        }
        return s3Client;
    }

    private S3Presigner getS3Presigner() {
        var credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        var builder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region));

        if(StringUtils.isNotEmpty(serviceEndpoint)) {
            var endpointParams = S3EndpointParams.builder()
                    .endpoint(serviceEndpoint)
                    .region(Region.of(region))
                    .build();

            var endpoint = S3EndpointProvider
                    .defaultProvider()
                    .resolveEndpoint(endpointParams).join();

            builder = builder.endpointOverride(endpoint.url());
        }

        return builder.build();
    }

    @Override
    public boolean isEnabled() {
        return !StringUtils.isEmpty(accessKeyId);
    }

    @Override
    public void uploadFile(TempFile tempFile) {
        var resource = tempFile.getResource();
        uploadFile(tempFile, resource.getName(), getObjectKey(resource));
    }

    @Override
    public void uploadNamespaceLogo(TempFile logoFile) {
        var namespace = logoFile.getNamespace();
        uploadFile(logoFile, namespace.getLogoName(), getObjectKey(namespace));
    }

    protected void uploadFile(TempFile file, String fileName, String objectKey) {
        var metadata = new HashMap<String, String>();
        metadata.put("Content-Type", StorageUtil.getFileType(fileName).toString());
        if (fileName.endsWith(".vsix")) {
            metadata.put("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        } else {
            metadata.put("Cache-Control", StorageUtil.getCacheControl(fileName).getHeaderValue());
        }

        var request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .metadata(metadata)
                .build();

        getS3Client().putObject(request, file.getPath());
    }

    @Override
    public void removeFile(FileResource resource) {
        removeFile(getObjectKey(resource));
    }

    @Override
    public void removeNamespaceLogo(Namespace namespace) {
        removeFile(getObjectKey(namespace));
    }

    private void removeFile(String objectKey) {
        var request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        getS3Client().deleteObject(request);
    }

    @Override
    public URI getLocation(FileResource resource) {
        return getLocation(getObjectKey(resource));
    }

    @Override
    public URI getNamespaceLogoLocation(Namespace namespace) {
        return getLocation(getObjectKey(namespace));
    }

    private URI getLocation(String objectKey) {
        var objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(fileCacheDurationConfig.getCacheDuration())
                .getObjectRequest(objectRequest)
                .build();

        try (var presigner = getS3Presigner()) {
            var presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.httpRequest().getUri();
        }
    }

    @Override
    public TempFile downloadFile(FileResource resource) throws IOException {
        var objectKey = getObjectKey(resource);
        var request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        var tempFile = new TempFile("temp_file_", "");
        try (var stream = getS3Client().getObject(request)) {
            Files.copy(stream, tempFile.getPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        tempFile.setResource(resource);
        return tempFile;
    }

    @Override
    public void copyFiles(List<Pair<FileResource, FileResource>> pairs) {
        pairs.forEach(pair -> copy(getObjectKey(pair.getFirst()), getObjectKey(pair.getSecond())));
    }

    @Override
    public void copyNamespaceLogo(Namespace oldNamespace, Namespace newNamespace) {
        copy(getObjectKey(oldNamespace), getObjectKey(newNamespace));
    }

    private void copy(String oldObjectKey, String newObjectKey) {
        var request = CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(oldObjectKey)
                .destinationBucket(bucket)
                .destinationKey(newObjectKey)
                .build();

        getS3Client().copyObject(request);
    }

    @Override
    @Cacheable(value = CACHE_EXTENSION_FILES, keyGenerator = GENERATOR_FILES, cacheManager = "fileCacheManager")
    public Path getCachedFile(FileResource resource) {
        var objectKey = getObjectKey(resource);
        var request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        var path = filesCacheKeyGenerator.generateCachedExtensionPath(resource);
        FileUtil.writeSync(path, p -> {
            try (var stream = getS3Client().getObject(request)) {
                Files.copy(stream, p);
            } catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return path;
    }

    protected String getObjectKey(FileResource resource) {
        var extVersion = resource.getExtension();
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        var segments = new String[] {namespace.getName(), extension.getName()};
        if (!extVersion.isUniversalTargetPlatform()) {
            segments = ArrayUtils.add(segments, extVersion.getTargetPlatform());
        }

        segments = ArrayUtils.add(segments, extVersion.getVersion());
        segments = ArrayUtils.addAll(segments, resource.getName().split("/"));
        return UrlUtil.createApiUrl("", segments).substring(1); // remove first '/'
    }

    protected String getObjectKey(Namespace namespace) {
        return UrlUtil.createApiUrl("", namespace.getName(), "logo", namespace.getLogoName()).substring(1);
    }
}
