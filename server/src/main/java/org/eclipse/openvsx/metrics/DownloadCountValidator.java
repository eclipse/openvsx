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

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.openvsx.entities.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Validates whether a download should increment the download count.
 * Prevents malicious actors from inflating download counts through:
 * - Deduplication per IP per extension per time window
 * - Filtering out automated/bot downloads
 *
 * Only active when Redis is enabled (ovsx.redis.enabled=true).
 * When Redis is unavailable, all downloads are counted (no filtering).
 */
@Service
@ConditionalOnProperty(value = "ovsx.redis.enabled", havingValue = "true")
public class DownloadCountValidator {

    private static final Logger logger = LoggerFactory.getLogger(DownloadCountValidator.class);

    private final StringRedisTemplate redisTemplate;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    /**
     * SpEL expression evaluated against the HttpServletRequest to extract the client IP.
     * Shares the same property as the rate limiter (ovsx.rate-limit.ip-address-function)
     * so both systems resolve IPs consistently.
     *
     * Default is getRemoteAddr() (TCP source IP — safe, but returns the proxy IP
     * when behind a reverse proxy). Override in application.yml for proxied deployments.
     */
    private final String ipAddressFunction;

    private static final Duration DEDUP_WINDOW = Duration.ofMinutes(30);

    public DownloadCountValidator(
            StringRedisTemplate redisTemplate,
            @Value("${ovsx.rate-limit.ip-address-function:getRemoteAddr()}") String ipAddressFunction
    ) {
        this.redisTemplate = redisTemplate;
        this.ipAddressFunction = ipAddressFunction;
    }

    /**
     * Determines if this download should increment the extension's download count.
     *
     * @param extension The extension being downloaded
     * @param request   The HTTP request with client information
     * @return true if the download should be counted, false otherwise
     */
    public boolean shouldCountDownload(Extension extension, HttpServletRequest request) {
        if (request == null) {
            return true;
        }

        String userAgent = extractUserAgent(request);
        if (isAutomatedClient(userAgent)) {
            return false;
        }

        String ipAddress = extractClientIp(request);
        if (ipAddress == null) {
            return true;
        }

        return isFirstDownloadInWindow(ipAddress, extension);
    }

    /**
     * Atomically checks if this is the first download from this IP for this
     * extension in the dedup window. Sets the key if absent.
     */
    private boolean isFirstDownloadInWindow(String ipAddress, Extension extension) {
        String key = buildDedupKey(ipAddress, extension);

        Boolean wasAbsent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", DEDUP_WINDOW);

        return Boolean.TRUE.equals(wasAbsent);
    }

    /**
     * Redis key for deduplication: download:dedup:{hashedIp}:{extensionId}
     */
    private String buildDedupKey(String ipAddress, Extension extension) {
        return String.format("download:dedup:%s:%d",
                hashIp(ipAddress),
                extension.getId()
        );
    }

    private String hashIp(String ipAddress) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ipAddress.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Extracts client IP by evaluating the same SpEL expression used by the rate limiter.
     * The expression runs against the HttpServletRequest as root object, so methods
     * like getHeader(), getRemoteAddr(), getParameter() are all available.
     *
     * @return the resolved IP, or null if it cannot be determined
     */
    private String extractClientIp(HttpServletRequest request) {
        try {
            var expr = expressionParser.parseExpression(ipAddressFunction);
            var result = expr.getValue(request, String.class);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            logger.warn("Failed to evaluate ip-address-function '{}': {}", ipAddressFunction, e.getMessage());
        }

        // Fallback to TCP source IP if the expression fails or returns empty
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : null;
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && !userAgent.isEmpty()) ? userAgent : null;
    }

    /**
     * Heuristic check for automated HTTP clients.
     * These downloads are served normally but don't inflate the count.
     */
    private boolean isAutomatedClient(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return false;
        }

        String ua = userAgent.toLowerCase();

        return ua.contains("python") ||
                ua.contains("requests") ||
                ua.contains("curl") ||
                ua.contains("wget") ||
                ua.contains("bot") ||
                ua.contains("crawler") ||
                ua.contains("script") ||
                ua.contains("automated");
    }

}
