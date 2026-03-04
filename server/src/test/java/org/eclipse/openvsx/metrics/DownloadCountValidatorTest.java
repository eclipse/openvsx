/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.metrics;

import org.eclipse.openvsx.metrics.config.DownloadCountValidationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DownloadCountValidatorTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOps;
    private DownloadCountValidationProperties properties;
    private DownloadCountValidator validator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);

        properties = new DownloadCountValidationProperties();
        properties.setEnabled(true);
        properties.setDedupWindowMinutes(30);
        properties.setKeyPrefix("download:dedup");
        properties.setLateArrivalHours(24);
        properties.setAutomatedClientKeywords(List.of("curl", "bot"));

        validator = new DownloadCountValidator(redisTemplate, "getRemoteAddr()", properties);
    }

    @Test
    public void shouldSkipValidationWhenDisabled() {
        properties.setEnabled(false);

        assertTrue(validator.shouldCountDownload(42L, "1.1.1.1", "curl/8.0", Instant.now()));
        Mockito.verifyNoInteractions(valueOps);
    }

    @Test
    public void shouldRejectAutomatedUserAgents() {
        assertFalse(validator.shouldCountDownload(42L, "1.1.1.1", "curl/8.0", Instant.now()));
        Mockito.verifyNoInteractions(valueOps);
    }

    @Test
    public void shouldUseConfiguredWindowAndKeyPrefix() {
        properties.setDedupWindowMinutes(45);
        properties.setLateArrivalHours(2);
        properties.setKeyPrefix("custom:dedup");
        Mockito.when(valueOps.setIfAbsent(Mockito.anyString(), Mockito.eq("1"), Mockito.any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        Instant eventTime = Instant.parse("2026-01-01T01:10:00Z");
        boolean result = validator.shouldCountDownload(99L, "10.10.10.10", "Mozilla/5.0", eventTime);

        assertTrue(result);
        Mockito.verify(valueOps).setIfAbsent(
                Mockito.argThat(key -> key.startsWith("custom:dedup:") && key.contains(":99:")),
                Mockito.eq("1"),
                Mockito.eq(Duration.ofMinutes(45).plusHours(2))
        );
    }

    @Test
    public void shouldNotCountWhenClientIpUnavailable() {
        assertFalse(validator.shouldCountDownload(42L, null, "Mozilla/5.0", Instant.now()));
        Mockito.verifyNoInteractions(valueOps);
    }

    @Test
    public void shouldUseEventTimeBucketInKey() {
        properties.setLateArrivalHours(24);
        properties.setDedupWindowMinutes(30);
        Mockito.when(valueOps.setIfAbsent(Mockito.anyString(), Mockito.eq("1"), Mockito.any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        // 2026-01-01T00:00:00Z = 0 epoch-minutes in window-30 → bucket = 0
        Instant eventTime = Instant.parse("2026-01-01T00:00:00Z");
        boolean result = validator.shouldCountDownload(99L, "10.0.0.1", "Mozilla/5.0", eventTime);

        assertTrue(result);
        // Key must end with the bucket index (0 for midnight) and TTL must include late-arrival buffer
        Mockito.verify(valueOps).setIfAbsent(
                Mockito.argThat(key -> key.startsWith("download:dedup:") && key.contains(":99:")),
                Mockito.eq("1"),
                Mockito.eq(Duration.ofMinutes(30).plusHours(24))
        );
    }

    @Test
    public void shouldNotCountWhenEventTimeIsMissingForEventTimeFlow() {
        boolean result = validator.shouldCountDownload(99L, "10.0.0.1", "Mozilla/5.0", null);
        assertFalse(result);
        Mockito.verifyNoInteractions(valueOps);
    }
}
