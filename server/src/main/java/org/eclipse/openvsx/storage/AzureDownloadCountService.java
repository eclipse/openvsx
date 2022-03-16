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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.eclipse.openvsx.entities.AzureDownloadCountProcessedItem;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;
import org.springframework.web.util.UriUtils;

import javax.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static org.eclipse.openvsx.entities.FileResource.STORAGE_AZURE;

/**
 * Pulls logs from Azure Blob Storage, extracts downloads from the logs
 * and updates download counts in the database.
 */
@Component
public class AzureDownloadCountService {

    protected final Logger logger = LoggerFactory.getLogger(AzureDownloadCountService.class);

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    EntityManager entityManager;

    @Autowired
    RepositoryService repositories;

    @Autowired
    StorageUtilService storageUtil;

    @Autowired
    SearchUtilService search;

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
        var logsEnabled = !Strings.isNullOrEmpty(logsServiceEndpoint);
        var storageEnabled = !Strings.isNullOrEmpty(storageServiceEndpoint);
        if(logsEnabled && !storageEnabled) {
            logger.warn("The ovsx.storage.azure.service-endpoint value must be set to enable AzureDownloadCountService");
        }

        return logsEnabled && storageEnabled;
    }

    /**
     * Task scheduled once per hour to pull logs from Azure Blob Storage and update extension download counts.
     */
    @Scheduled(cron = "0 5 * * * *", zone = "UTC")
    @SchedulerLock(name = "updateDownloadCounts", lockAtLeastFor = "5m", lockAtMostFor = "55m")
    public void updateDownloadCounts() {
        if (!isEnabled()) {
            return;
        }

        var blobs = listBlobs();
        var iterableByPage = blobs.iterableByPage();

        var stopWatch = new StopWatch();
        while(iterableByPage != null) {
            PagedResponse<BlobItem> response = null;
            var iterator = iterableByPage.iterator();
            if(iterator.hasNext()) {
                response = iterator.next();
                var blobNames = getBlobNames(response.getValue());
                blobNames.removeAll(repositories.findAllSucceededAzureDownloadCountProcessedItemsByNameIn(blobNames));
                for (var name : blobNames) {
                    try {
                        transactions.executeWithoutResult(status -> {
                            var processedItem = new AzureDownloadCountProcessedItem();
                            processedItem.setName(name);
                            processedItem.setProcessedOn(LocalDateTime.now());
                            entityManager.persist(processedItem);

                            stopWatch.start();
                            try {
                                transactions.executeWithoutResult(s -> processBlobItem(name));
                                processedItem.setSuccess(true);
                            } catch (Exception e) {
                                logger.error("Failed to process BlobItem: " + name, e);
                            }

                            stopWatch.stop();
                            processedItem.setExecutionTime((int) stopWatch.getLastTaskTimeMillis());
                        });
                    } catch(TransactionException e) {
                        logger.error("Transaction failed", e);
                    }
                }
            }

            var continuationToken = response != null ? response.getContinuationToken() : "";
            iterableByPage = !Strings.isNullOrEmpty(continuationToken) ? blobs.iterableByPage(continuationToken) : null;
        }
    }

    private void processBlobItem(String blobName) {
        try (var outputStream = new ByteArrayOutputStream()) {
            getContainerClient().getBlobClient(blobName).download(outputStream);
            var bytes = outputStream.toByteArray();

            var files = new HashMap<String, List<LocalDateTime>>();
            var jsonObjects = new String(bytes).split("\n");
            for (var jsonObject : jsonObjects) {
                var node = getObjectMapper().readTree(jsonObject);
                var operationName = node.get("operationName").asText();
                var statusCode = node.get("statusCode").asInt();

                String[] pathParams = null;
                if (operationName.equals("GetBlob") && statusCode == 200) {
                    var blobUri = URI.create(node.get("uri").asText());
                    pathParams = blobUri.getPath().split("/");
                }

                var matchesStorageBlobContainer = false;
                if(pathParams != null) {
                    var container = pathParams[1];
                    matchesStorageBlobContainer = storageBlobContainer.equals(container);
                }
                if(matchesStorageBlobContainer) {
                    var fileName = UriUtils.decode(pathParams[pathParams.length - 1], StandardCharsets.UTF_8).toUpperCase();
                    var timestamps = files.getOrDefault(fileName, new ArrayList<>());
                    timestamps.add(LocalDateTime.parse(node.get("time").asText(), DateTimeFormatter.ISO_ZONED_DATE_TIME));
                    files.put(fileName, timestamps);
                }
            }

            var fileResources = repositories.findDownloadsByStorageTypeAndName(STORAGE_AZURE, files.keySet());
            for (var fileResource : fileResources) {
                var timestamps = files.get(fileResource.getName().toUpperCase());
                storageUtil.increaseDownloadCount(fileResource.getExtension(), fileResource, timestamps);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
