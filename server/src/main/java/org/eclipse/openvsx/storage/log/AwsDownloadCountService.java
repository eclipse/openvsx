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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.entities.Extension;
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
import software.amazon.awssdk.services.s3.model.*;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
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

/**
 * Pulls logs from an Amazon S3 bucket, extracts downloads from the logs and updates download counts in the database.
 * <p>
 * The following log file formats are supported:
 * <ul>
 *     <li>cloudfront</li>
 *     <li>fastly</li>
 * </ul>
 * <p>
 * See
 * <ul>
 *   <li><a href="https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/standard-logging.html">CloudFront standard logging</a></li>
 *   <li><a href="https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/standard-logs-reference.html">CloudFront log format</a></li>
 *   <li><a href="https://www.fastly.com/documentation/guides/integrations/streaming-logs/custom-log-formats/">Fastly custom log format</a></li>
 * </ul>
 */
@Component
public class AwsDownloadCountService {
    private final Logger logger = LoggerFactory.getLogger(AwsDownloadCountService.class);

    private static final String LOG_LOCATION_PREFIX = "AWSLogs/";
    private static final int MAX_KEYS = 100;

    private final AwsStorageService  awsStorageService;
    private final DownloadCountProcessor processor;

    @Value("${ovsx.logs.aws.bucket:}")
    String bucket;

    @Value("${ovsx.logs.aws.log-location-prefix:" + LOG_LOCATION_PREFIX + "}")
    String logLocationPrefix;

    @Value("${ovsx.logs.aws.format:cloudfront}")
    String logFormat;

    LogFileParser logFileParser;

    public AwsDownloadCountService(AwsStorageService awsStorageService, DownloadCountProcessor processor) {
        this.awsStorageService = awsStorageService;
        this.processor = processor;
    }

    @PostConstruct
    public void initialize() {
        logFileParser = switch (logFormat.toLowerCase()) {
            case "cloudfront" -> new CloudFrontLogFileParser();
            case "fastly" -> new FastlyLogFileParser();
            default -> throw new IllegalArgumentException("unsupported log file format '" + logFormat + "'");
        };
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
    @Recurring(id = "update-aws-download-counts", cron = "0 10 * * * *", zoneId = "UTC")
    public void updateDownloadCounts() {
        if (!isEnabled()) {
            return;
        }

        logger.info("[AwsDownloadCountService] >> updateDownloadCounts");

        // Note: need to align the next jobRunTime with the cron schedule when changing it.
        var nextJobRunTime = LocalDateTime.now().plusHours(1).withMinute(10);
        var maxExecutionTime = LocalDateTime.now().plusMinutes(50);

        var stopWatch = new StopWatch();

        String continuationToken = null;

        do {
            var objects = listObjects(continuationToken);

            var files = objects.contents().stream().map(S3Object::key).toList();
            if (!processResponse(files, stopWatch, maxExecutionTime, nextJobRunTime)) {
                break;
            }

            continuationToken = objects.isTruncated() ? objects.nextContinuationToken() : null;
        } while (continuationToken != null);

        logger.info("[AwsDownloadCountService] << updateDownloadCounts");
    }

    private boolean processResponse(
            List<String> files,
            StopWatch stopWatch,
            LocalDateTime maxExecutionTime,
            LocalDateTime nextJobRunTime
    ) {
        var logFiles = files.stream().filter(logFile -> logFile.endsWith(".gz")).collect(Collectors.toList());

        // determine log files that have already been processed -> delete them and do not re-process them
        var processedItems = processor.processedItems(FileResource.STORAGE_AWS, logFiles);
        processedItems.forEach(this::deleteFile);
        if (!processedItems.isEmpty()) {
            logger.info("[AwsDownloadCountService] deleting already analysed log files:");
            processedItems.forEach(item -> logger.info("  - {}", item));
        }
        logFiles.removeAll(processedItems);

        // determine log files that could not be processed before -> keep them for analysis and skip processing
        var failedItems = processor.failedItems(FileResource.STORAGE_AWS, logFiles);
        if (!failedItems.isEmpty()) {
            logger.info("[AwsDownloadCountService] skipping previously failed log files:");
            failedItems.forEach(item -> logger.info("  - {}", item));
        }
        logFiles.removeAll(failedItems);

        var allUpdatedExtensions = new HashMap<Long, Extension>();

        try {
            for (var name : logFiles) {
                var processedOn = LocalDateTime.now();

                if (processedOn.isAfter(maxExecutionTime)) {
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
                        updatedExtensions.forEach(extension -> allUpdatedExtensions.put(extension.getId(), extension));
                    }

                    success = true;
                } catch (Exception e) {
                    logger.error("failed to process log file: {}", name, e);
                }

                stopWatch.stop();
                var executionTime = (int) stopWatch.lastTaskInfo().getTimeMillis();
                processor.persistProcessedItem(name, FileResource.STORAGE_AWS, processedOn, executionTime, success);
                if (success) {
                    deleteFile(name);
                }
            }

            return true;
        } finally {
            // evict caches and update search entries for all updated extensions
            allUpdatedExtensions.values().forEach(processor::evictCaches);
            processor.updateSearchEntries(allUpdatedExtensions.values().stream().toList());
        }
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

                var record = logFileParser.parse(line);
                if (record == null) {
                    continue;
                }

                if (isGetOperation(record) && isStatusOk(record) && isExtensionPackageUri(record)) {
                    var uri = record.url();
                    var uriComponents = uri.split("/");
                    var vsixFile = UriUtils.decode(uriComponents[uriComponents.length - 1], StandardCharsets.UTF_8).toUpperCase();
                    fileCounts.merge(vsixFile, 1, Integer::sum);
                }
            }
            return fileCounts;
        }
    }

    private boolean isGetOperation(LogRecord record) {
        return record.method().equalsIgnoreCase("GET");
    }

    private boolean isStatusOk(LogRecord record) {
        return record.status() == 200;
    }

    private boolean isExtensionPackageUri(LogRecord record) {
        return record.url().endsWith(".vsix");
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

    private void deleteFile(String objectKey) {
        getS3Client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }

    private ListObjectsV2Response listObjects(String continuationToken) {
        var builder = ListObjectsV2Request.builder().bucket(bucket).maxKeys(MAX_KEYS).prefix(logLocationPrefix);

        if (continuationToken != null) {
            builder.continuationToken(continuationToken);
        }

        return getS3Client().listObjectsV2(builder.build());
    }
}

record LogRecord(String method, int status, String url) {}

interface LogFileParser {
    @Nullable
    LogRecord parse(String line);
}

class CloudFrontLogFileParser implements LogFileParser {
    @Override
    public LogRecord parse(String line) {
        if (line.startsWith("#")) {
            return null;
        }

        // Format:
        // date	time x-edge-location sc-bytes c-ip cs-method cs(Host) cs-uri-stem sc-status	cs(Referer)	cs(User-Agent) cs-uri-query cs(Cookie) x-edge-result-type	x-edge-request-id	x-host-header	cs-protocol	cs-bytes	time-taken	x-forwarded-for	ssl-protocol	ssl-cipher	x-edge-response-result-type	cs-protocol-version	fle-status	fle-encrypted-fields	c-port	time-to-first-byte	x-edge-detailed-result-type	sc-content-type	sc-content-len	sc-range-start	sc-range-end
        var components = line.split("[ \t]+");
        return new LogRecord(components[5], Integer.parseInt(components[8]), components[7]);
    }
}

class FastlyLogFileParser implements LogFileParser {
    private final ObjectMapper mapper;

    public FastlyLogFileParser() {
        this.mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(LogRecord.class, new LogRecordDeserializer());
        mapper.registerModule(module);
    }

    @Override
    public @Nullable LogRecord parse(String line) {
        try {
            return mapper.readValue(line, LogRecord.class);
        } catch (JacksonException ex) {
            return null;
        }
    }
}

class LogRecordDeserializer extends StdDeserializer<LogRecord> {

    public LogRecordDeserializer() {
        this(null);
    }

    public LogRecordDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LogRecord deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String operation = node.get("request_method").asText();
        int status = (Integer) node.get("response_status").numberValue();
        String url = node.get("url").asText();
        return new LogRecord(operation, status, url);
    }
}