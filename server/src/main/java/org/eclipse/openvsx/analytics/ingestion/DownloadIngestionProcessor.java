/********************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.analytics.ingestion;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.eclipse.openvsx.analytics.DownloadAnalyticsRepository;
import org.eclipse.openvsx.analytics.DownloadEvent;
import org.eclipse.openvsx.cache.CacheService;
import org.eclipse.openvsx.entities.DownloadIngestion;
import org.eclipse.openvsx.entities.Extension;
import org.eclipse.openvsx.entities.FileResource;
import org.eclipse.openvsx.repositories.RepositoryService;
import org.eclipse.openvsx.search.SearchUtilService;

@Component
public class DownloadIngestionProcessor {

    protected final Logger logger = LoggerFactory.getLogger(DownloadIngestionProcessor.class);

    private final EntityManager entityManager;
    private final RepositoryService repositories;
    private final CacheService cache;
    private final SearchUtilService search;
    private final ObservationRegistry observations;
    private final ObjectProvider<DownloadAnalyticsRepository> analyticsRepository;
    private final DownloadIngestionMetrics metrics;

    private final Cache<String, ResolvedExtension> resolutionCache = Caffeine.newBuilder()
            .maximumSize(65_536)
            .expireAfterWrite(Duration.ofHours(24))
            .build();

    public DownloadIngestionProcessor(
            EntityManager entityManager,
            RepositoryService repositories,
            CacheService cache,
            SearchUtilService search,
            ObservationRegistry observations,
            ObjectProvider<DownloadAnalyticsRepository> analyticsRepository,
            DownloadIngestionMetrics metrics
    ) {
        this.entityManager = entityManager;
        this.repositories = repositories;
        this.cache = cache;
        this.search = search;
        this.observations = observations;
        this.analyticsRepository = analyticsRepository;
        this.metrics = metrics;
    }

    /**
     * A download-relevant snapshot of the extension version a vsix file belongs to.
     */
    public record ResolvedExtension(
            long extensionId,
            long extensionVersionId,
            String namespace,
            String extensionName,
            String version,
            String targetPlatform
    ) {}

    /**
     * Processes one log file's download records: resolves vsix filenames to extension versions,
     * aggregates them into hourly {@link DownloadEvent}s and, in a single transaction, saves the
     * events, increments the extension download counters and writes the download ingestion entry.
     * Returns the extensions whose counters changed, for cache eviction and search updates.
     */
    @Transactional
    public List<Extension> process(
            String storageType,
            String fileName,
            LocalDateTime processedOn,
            int executionTime,
            List<RawDownloadRecord> records
    ) {
        return Observation.createNotStarted("DownloadIngestionProcessor#process", observations).observe(() -> {
            var resolved = resolveExtensions(storageType, records);
            var events = aggregate(records, resolved);
            if (!events.isEmpty()) {
                analyticsRepository.ifAvailable(repository -> repository.save(events));
            }

            var extensionDownloads = events.stream().collect(
                    Collectors.groupingBy(DownloadEvent::extensionId, Collectors.summingInt(DownloadEvent::count)));
            var extensions = extensionDownloads.isEmpty()
                    ? List.<Extension>of()
                    : increaseDownloadCounts(extensionDownloads);
            persistIngestion(fileName, storageType, processedOn, executionTime, true);

            metrics.recordLoaded(events.size(), events.stream().mapToInt(DownloadEvent::count).sum());
            records.stream().map(RawDownloadRecord::time).max(Instant::compareTo).ifPresent(
                    latest -> metrics.recordExtractLag(Duration.between(latest, Instant.now())));
            return extensions;
        });
    }

    /**
     * Records a single request-path download of a file that no {@link DownloadRecordSource}
     * covers. Client IP and user agent are taken from the current HTTP request, if any. The
     * event save participates in the caller's transaction, so it commits atomically with the
     * download counter.
     */
    public void captureDownload(FileResource resource) {
        analyticsRepository.ifAvailable(repository -> {
            var extVersion = resource.getExtension();
            if (extVersion == null) {
                logger.warn("no extension version found for download {}, skipping", resource.getName());
                return;
            }

            var extension = extVersion.getExtension();
            var request = currentRequest();
            var userAgent = request != null ? StringUtils.trimToNull(request.getHeader("User-Agent")) : null;
            var event = new DownloadEvent(
                    Instant.now(),
                    extension.getId(),
                    extVersion.getId(),
                    extension.getNamespace().getName(),
                    extension.getName(),
                    extVersion.getVersion(),
                    extVersion.getTargetPlatform(),
                    // no country information is available on the request path
                    null,
                    clientIp(request),
                    userAgent,
                    1);
            repository.save(List.of(event));
            metrics.recordLoaded(1, 1);
        });
    }

    private @Nullable HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }

    private @Nullable String clientIp(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private Map<String, ResolvedExtension> resolveExtensions(String storageType, List<RawDownloadRecord> records) {
        var filenames = records.stream().map(RawDownloadRecord::vsixFilename).distinct().sorted().toList();
        var resolved = new HashMap<String, ResolvedExtension>();
        var misses = new ArrayList<String>();
        for (var filename : filenames) {
            var cached = resolutionCache.getIfPresent(cacheKey(storageType, filename));
            if (cached != null) {
                resolved.put(filename, cached);
            } else {
                misses.add(filename);
            }
        }

        if (!misses.isEmpty()) {
            for (var resource : repositories.findDownloadsByStorageTypeAndName(storageType, misses)) {
                var extVersion = resource.getExtension();
                if (extVersion == null) {
                    logger.warn("no extension version found for download {}, skipping", resource.getName());
                    continue;
                }

                var extension = extVersion.getExtension();
                var entry = new ResolvedExtension(
                        extension.getId(),
                        extVersion.getId(),
                        extension.getNamespace().getName(),
                        extension.getName(),
                        extVersion.getVersion(),
                        extVersion.getTargetPlatform());
                var filename = resource.getName().toUpperCase();
                resolved.put(filename, entry);
                resolutionCache.put(cacheKey(storageType, filename), entry);
            }
        }

        return resolved;
    }

    private String cacheKey(String storageType, String filename) {
        return storageType + '|' + filename;
    }

    private List<DownloadEvent> aggregate(List<RawDownloadRecord> records, Map<String, ResolvedExtension> resolved) {
        var counts = new LinkedHashMap<EventKey, Integer>();
        var skipped = 0;
        for (var record : records) {
            var extension = resolved.get(record.vsixFilename());
            if (extension == null) {
                skipped++;
                continue;
            }

            var key = new EventKey(
                    record.time().truncatedTo(ChronoUnit.HOURS),
                    extension,
                    CountryCodes.toIsoCode(record.country()),
                    record.ip(),
                    record.rawUserAgent());
            counts.merge(key, 1, Integer::sum);
        }
        if (skipped > 0) {
            logger.warn("skipped {} download records referring to unknown vsix files", skipped);
        }

        return counts.entrySet().stream()
                .map(
                        entry -> new DownloadEvent(
                                entry.getKey().time(),
                                entry.getKey().extension().extensionId(),
                                entry.getKey().extension().extensionVersionId(),
                                entry.getKey().extension().namespace(),
                                entry.getKey().extension().extensionName(),
                                entry.getKey().extension().version(),
                                entry.getKey().extension().targetPlatform(),
                                entry.getKey().country(),
                                entry.getKey().ip(),
                                entry.getKey().userAgent(),
                                entry.getValue()))
                .toList();
    }

    private record EventKey(
            Instant time,
            ResolvedExtension extension,
            @Nullable String country,
            @Nullable String ip,
            @Nullable String userAgent
    ) {}

    @Transactional
    public void persistIngestion(
            String name,
            String storageType,
            LocalDateTime processedOn,
            int executionTime,
            boolean success
    ) {
        Observation.createNotStarted("DownloadIngestionProcessor#persistIngestion", observations).observe(() -> {
            var processedItem = new DownloadIngestion();
            processedItem.setName(name);
            processedItem.setStorageType(storageType);
            processedItem.setProcessedOn(processedOn);
            processedItem.setExecutionTime(executionTime);
            processedItem.setSuccess(success);
            entityManager.persist(processedItem);
        });
    }

    @Transactional
    public List<Extension> increaseDownloadCounts(Map<Long, Integer> extensionDownloads) {
        return Observation.createNotStarted("DownloadIngestionProcessor#increaseDownloadCounts", observations)
                .observe(() -> {
                    var extensions = repositories.findExtensions(extensionDownloads.keySet()).toList();
                    extensions.forEach(extension -> {
                        var downloads = extensionDownloads.get(extension.getId());
                        extension.setDownloadCount(extension.getDownloadCount() + downloads);
                    });

                    return extensions;
                });
    }

    @Transactional // needs transaction for lazy-loading versions
    public void evictCaches(Extension extension) {
        Observation.createNotStarted("DownloadIngestionProcessor#evictCaches", observations).observe(() -> {
            var mergedExtension = entityManager.merge(extension);
            cache.evictExtensionJsons(mergedExtension);
            cache.evictLatestExtensionVersion(mergedExtension);
        });
    }

    public void updateSearchEntries(List<Extension> extensions) {
        Observation.createNotStarted("DownloadIngestionProcessor#updateSearchEntries", observations).observe(() -> {
            logger.info("[DownloadIngestionProcessor] >> updateSearchEntries");
            var activeExtensions = extensions.stream()
                    .filter(Extension::isActive)
                    .collect(Collectors.toList());

            logger.info("[DownloadIngestionProcessor] total active extensions: {}", activeExtensions.size());
            var parts = Lists.partition(activeExtensions, 100);
            logger.info("[DownloadIngestionProcessor] partitions: {} | partition size: 100", parts.size());

            parts.forEach(search::updateSearchEntriesAsync);
            logger.info("[DownloadIngestionProcessor] << updateSearchEntries");
        });
    }

    public List<String> succeededIngestions(String storageType, List<String> blobNames) {
        return Observation.createNotStarted("DownloadIngestionProcessor#succeededIngestions", observations).observe(
                () -> repositories
                        .findAllSucceededDownloadIngestionsByStorageTypeAndNameIn(storageType, blobNames));
    }

    public List<String> failedIngestions(String storageType, List<String> blobNames) {
        return Observation.createNotStarted("DownloadIngestionProcessor#failedIngestions", observations).observe(
                () -> repositories
                        .findAllFailedDownloadIngestionsByStorageTypeAndNameIn(storageType, blobNames));
    }
}
