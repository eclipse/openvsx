/********************************************************************************
 * Copyright (c) 2021 Precies. Software and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.storage;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.PagedResponse;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobListDetails;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.util.TempFile;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.spring.annotations.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pulls logs from Azure Blob Storage, extracts downloads from the logs
 * and updates download counts in the database.
 */
@Component
public class AzureDownloadCountService {

    protected final Logger logger = LoggerFactory.getLogger(AzureDownloadCountService.class);

    @Autowired
    AzureDownloadCountProcessor processor;

    @Value("${ovsx.logs.azure.sas-token:}")
    String sasToken;

    @Value("${ovsx.logs.azure.service-endpoint:}")
    String logsServiceEndpoint;

    @Value("${ovsx.logs.azure.blob-container:insights-logs-storageread}")
    String logsBlobContainer;

    @Value("${ovsx.storage.azure.service-endpoint:}")
    String storageServiceEndpoint;

    @Value("${ovsx.storage.azure.blob-container:openvsx-resources}")
    String storageBlobContainer;

    private BlobContainerClient containerClient;
    private ObjectMapper objectMapper;
    private Pattern blobItemNamePattern;

    /**
     * Indicates whether the download service is enabled by application config.
     */
    public boolean isEnabled() {
        var logsEnabled = !StringUtils.isEmpty(logsServiceEndpoint);
        var storageEnabled = !StringUtils.isEmpty(storageServiceEndpoint);
        if(logsEnabled && !storageEnabled) {
            logger.warn("The ovsx.storage.azure.service-endpoint value must be set to enable AzureDownloadCountService");
        }

        return logsEnabled && storageEnabled;
    }

    /**
     * Task scheduled once per hour to pull logs from Azure Blob Storage and update extension download counts.
     */
    @Job(name = "Update Download Counts", retries = 0)
    @Recurring(id = "update-download-counts", cron = "0 5 * * * *", zoneId = "UTC")
    public void updateDownloadCounts() {
        if (!isEnabled()) {
            return;
        }

        logger.info(">> updateDownloadCounts");
        var maxExecutionTime = LocalDateTime.now().withMinute(55);
        var blobs = listBlobs();
        var iterableByPage = blobs.iterableByPage();

        var stopWatch = new StopWatch();
        while(iterableByPage != null) {
            PagedResponse<BlobItem> response = null;
            var iterator = iterableByPage.iterator();
            if(iterator.hasNext()) {
                response = iterator.next();
                var blobNames = getBlobNames(response.getValue());
                blobNames.removeAll(processor.processedItems(blobNames));
                for (var name : blobNames) {
                    if(LocalDateTime.now().isAfter(maxExecutionTime)) {
                        var nextJobRunTime = LocalDateTime.now().plusHours(1).withMinute(5);
                        logger.info("Failed to process all download counts within timeslot, next job run is at {}", nextJobRunTime);
                        logger.info("<< updateDownloadCounts");
                        return;
                    }

                    var processedOn = LocalDateTime.now();
                    var success = false;
                    stopWatch.start();
                    try {
                        var files = processBlobItem(name);
                        if(!files.isEmpty()) {
                            var extensionDownloads = processor.processDownloadCounts(files);
                            var updatedExtensions = processor.increaseDownloadCounts(extensionDownloads);
                            processor.evictCaches(updatedExtensions);
                            processor.updateSearchEntries(updatedExtensions);
                        }

                        success = true;
                    } catch (Exception e) {
                        logger.error("Failed to process BlobItem: " + name, e);
                    }

                    stopWatch.stop();
                    var executionTime = (int) stopWatch.getLastTaskTimeMillis();
                    processor.persistProcessedItem(name, processedOn, executionTime, success);
                }
            }

            var continuationToken = response != null ? response.getContinuationToken() : "";
            iterableByPage = !StringUtils.isEmpty(continuationToken) ? blobs.iterableByPage(continuationToken) : null;
        }

        logger.info("<< updateDownloadCounts");
    }

    private Map<String, List<LocalDateTime>> processBlobItem(String blobName) {
        try (
                var downloadsTempFile = downloadBlobItem(blobName);
                var reader = Files.newBufferedReader(downloadsTempFile.getPath())
        ) {
            return reader.lines()
                    .map(line -> {
                        try {
                            return getObjectMapper().readTree(line);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(node -> {
                        var operationName = node.get("operationName").asText();
                        var statusCode = node.get("statusCode").asInt();
                        var uri = node.get("uri").asText();
                        return operationName.equals("GetBlob") && statusCode == 200 && uri.endsWith(".vsix");
                    }).map(node -> {
                        var uri = node.get("uri").asText();
                        var pathParams = uri.substring(storageServiceEndpoint.length()).split("/");
                        return new AbstractMap.SimpleEntry<>(pathParams, node.get("time").asText());
                    })
                    .filter(entry -> storageBlobContainer.equals(entry.getKey()[1]))
                    .map(entry -> {
                        var pathParams = entry.getKey();
                        var fileName = UriUtils.decode(pathParams[pathParams.length - 1], StandardCharsets.UTF_8).toUpperCase();
                        var time = LocalDateTime.parse(entry.getValue(), DateTimeFormatter.ISO_ZONED_DATE_TIME);
                        return new AbstractMap.SimpleEntry<>(fileName, time);
                    })
                    .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TempFile downloadBlobItem(String blobName) throws IOException {
        var downloadsTempFile = new TempFile("azure-downloads-", ".json");
        getContainerClient().getBlobClient(blobName).downloadToFile(downloadsTempFile.getPath().toAbsolutePath().toString(), true);
        return downloadsTempFile;
    }

    private List<String> getBlobNames(List<BlobItem> items) {
        var blobNames = new ArrayList<String>();
        for(var item : items) {
            var name = item.getName();
            if(isCorrectName(name)) {
                blobNames.add(name);
            }
        }

        return blobNames;
    }

    private PagedIterable<BlobItem> listBlobs() {
        var details = new BlobListDetails()
                .setRetrieveCopy(false)
                .setRetrieveMetadata(false)
                .setRetrieveDeletedBlobs(false)
                .setRetrieveTags(false)
                .setRetrieveSnapshots(false)
                .setRetrieveUncommittedBlobs(false)
                .setRetrieveVersions(false);

        var options = new ListBlobsOptions().setMaxResultsPerPage(100).setDetails(details);
        return getContainerClient().listBlobs(options, Duration.ofMinutes(5));
    }

    private BlobContainerClient getContainerClient() {
        if (containerClient == null) {
            containerClient = new BlobContainerClientBuilder()
                    .endpoint(logsServiceEndpoint)
                    .sasToken(sasToken)
                    .containerName(logsBlobContainer)
                    .buildClient();
        }

        return containerClient;
    }

    private ObjectMapper getObjectMapper() {
        if(objectMapper == null) {
            objectMapper = new ObjectMapper();
        }

        return objectMapper;
    }

    private boolean isCorrectName(String name) {
        return getBlobItemNamePattern().matcher(name).matches();
    }

    private Pattern getBlobItemNamePattern() {
        if(blobItemNamePattern == null) {
            var host = URI.create(storageServiceEndpoint).getHost();
            var storageAccount = host.substring(0, host.indexOf('.'));

            var regex = "^resourceId=/subscriptions/.*/resourceGroups/.*/providers/Microsoft\\.Storage/storageAccounts/" + storageAccount + "/blobServices/default/.*$";
            blobItemNamePattern = Pattern.compile(regex);
        }

        return blobItemNamePattern;
    }
}
