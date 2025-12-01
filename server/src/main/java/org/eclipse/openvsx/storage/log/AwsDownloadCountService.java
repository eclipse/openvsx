/********************************************************************************
 * Copyright (c) 2025 Eclipse Foundation and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage.log;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.storage.AwsStorageService;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;


@Component
public class AwsDownloadCountService {
    private final Logger logger = LoggerFactory.getLogger(AwsDownloadCountService.class);

    private static final String LOG_LOCATION_PREFIX = "AWSLogs/";
    private static final int MAX_KEYS = 100;

    private final AwsStorageService  awsStorageService;
    private final DownloadCountProcessor processor;

    @Value("${ovsx.logs.aws.bucket:}")
    String bucket;

    public AwsDownloadCountService(AwsStorageService awsStorageService, DownloadCountProcessor processor) {
        this.awsStorageService = awsStorageService;
        this.processor = processor;
    }

    /**
     * Indicates whether the download service is enabled by application config.
     */
    public boolean isEnabled() {
        return !StringUtils.isEmpty(bucket) && awsStorageService.isEnabled();
    }

    private S3Client getS3Client() {
        return awsStorageService.getS3Client();
    }

    /**
     * Task scheduled once per hour to pull logs from AWS S3 Storage and update extension download counts.
     */
    @Job(name = "Update AWS Download Counts", retries = 0)
    @Recurring(id = "update-aws-download-counts", cron = "0 * * * * *", zoneId = "UTC")
    public void updateDownloadCounts() {
        if (!isEnabled()) {
            return;
        }

        logger.info("[AwsDownloadCountService] >> updateDownloadCounts");
        var maxExecutionTime = LocalDateTime.now().plusMinutes(50);

        var stopWatch = new StopWatch();

        String continuationToken = null;

        do {
            var objects = listObjects(continuationToken);

            var files = objects.contents().stream().map(S3Object::key).toList();
            if (!processResponse(files, stopWatch, maxExecutionTime)) {
                break;
            }

            continuationToken = objects.isTruncated() ? objects.nextContinuationToken() : null;
        } while (continuationToken != null);

        logger.info("[AwsDownloadCountService] << updateDownloadCounts");
    }

    private boolean processResponse(List<String> files, StopWatch stopWatch, LocalDateTime maxExecutionTime) {
        var logFiles = files.stream().filter(logFile -> logFile.endsWith(".gz")).collect(Collectors.toList());
        var processedItems = processor.processedItems(FileResource.STORAGE_AWS, logFiles);
        processedItems.forEach(this::deleteLogFile);
        logFiles.removeAll(processedItems);
        for (var name : logFiles) {
            var processedOn = LocalDateTime.now();

            if (processedOn.isAfter(maxExecutionTime)) {
                var nextJobRunTime = LocalDateTime.now().plusHours(1).withMinute(5);
                logger.info("Failed to process all download counts within timeslot, next job run is at {}", nextJobRunTime);
                return false;
            }

            var success = false;
            stopWatch.start();
            try {
                var counts = processLogFile(name);
                if (!counts.isEmpty()) {
                    var extensionDownloads = processor.processDownloadCounts(FileResource.STORAGE_AWS, counts);
                    var updatedExtensions = processor.increaseDownloadCounts(extensionDownloads);
                    processor.evictCaches(updatedExtensions);
                    processor.updateSearchEntries(updatedExtensions);
                }

                success = true;
            } catch (Exception e) {
                logger.error("failed to process log file: {}", name, e);
            }

            stopWatch.stop();
            var executionTime = (int) stopWatch.lastTaskInfo().getTimeMillis();
            processor.persistProcessedItem(name, FileResource.STORAGE_AWS, processedOn, executionTime, success);
            if (success) {
                deleteLogFile(name);
            }
        }

        return true;
    }

    private Map<String, Integer> processLogFile(String fileName) throws IOException {
        try (
                var downloadsTempFile = downloadFile(fileName);
                var fileStream = new FileInputStream(downloadsTempFile.getPath().toFile());
                var gzipStream = new GZIPInputStream(fileStream);
                var reader = new BufferedReader(new InputStreamReader(gzipStream, StandardCharsets.UTF_8));
        ) {
            var fileCounts = new HashMap<String, Integer>();
            var lines = reader.lines().iterator();
            while (lines.hasNext()) {
                var line = lines.next();
                if (line.startsWith("#")) {
                    continue;
                }

                // Format:
                // date	time x-edge-location sc-bytes c-ip cs-method cs(Host) cs-uri-stem sc-status	cs(Referer)	cs(User-Agent) cs-uri-query cs(Cookie) x-edge-result-type	x-edge-request-id	x-host-header	cs-protocol	cs-bytes	time-taken	x-forwarded-for	ssl-protocol	ssl-cipher	x-edge-response-result-type	cs-protocol-version	fle-status	fle-encrypted-fields	c-port	time-to-first-byte	x-edge-detailed-result-type	sc-content-type	sc-content-len	sc-range-start	sc-range-end
                var components = line.split("[ \t]+");

                if (isGetOperation(components) && isStatusOk(components) && isExtensionPackageUri(components)) {
                    var uri = components[7];
                    var uriComponents = uri.split("/");
                    var vsixFile = UriUtils.decode(uriComponents[uriComponents.length - 1], StandardCharsets.UTF_8).toUpperCase();
                    fileCounts.merge(vsixFile, 1, Integer::sum);
                }
            }
            return fileCounts;
        }
    }

    private boolean isGetOperation(String[] components) {
        return components[5].equalsIgnoreCase("GET");
    }

    private boolean isStatusOk(String[] components) {
        return Integer.parseInt(components[8]) == 200;
    }

    private boolean isExtensionPackageUri(String[] components) {
        return components[7].endsWith(".vsix");
    }

    private TempFile downloadFile(String objectKey) throws IOException {
        var downloadsTempFile = new TempFile("aws-downloads-", ".gz");
        var inputStream =
                getS3Client().getObject(
                        GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(objectKey)
                                .build(),
                        ResponseTransformer.toInputStream()
                );
        Files.copy(inputStream, downloadsTempFile.getPath(), StandardCopyOption.REPLACE_EXISTING);
        return downloadsTempFile;
    }

    private void deleteLogFile(String objectKey) {
//        getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    private ListObjectsV2Response listObjects(String continuationToken) {
        var builder = ListObjectsV2Request.builder().bucket(bucket).maxKeys(MAX_KEYS).prefix(LOG_LOCATION_PREFIX);

        if (continuationToken != null) {
            builder.continuationToken(continuationToken);
        }

        return getS3Client().listObjectsV2(builder.build());
    }
}
