/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.openvsx.config.ApplicationConfig;
import org.eclipse.openvsx.entities.ExtensionVersion;
import org.eclipse.openvsx.json.ExtensionJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriUtils;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public final class UrlUtil {

    private static final Logger logger = LoggerFactory.getLogger(UrlUtil.class);
    
    private static ApplicationConfig config;

    @Autowired
    public UrlUtil(ApplicationConfig applicationConfig) {
        config = applicationConfig;
    }

    private UrlUtil() {
    }


    public static String createApiFileUrl(String baseUrl, ExtensionVersion extVersion, String fileName) {
        var extension = extVersion.getExtension();
        var namespaceName = extension.getNamespace().getName();
        return createApiFileUrl(baseUrl, namespaceName, extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion(), fileName);
    }

    public static String createApiFileUrl(String baseUrl, String namespaceName, String extensionName, String targetPlatform, String version, String fileName) {
        return createApiFileUrl(createApiFileBaseUrl(baseUrl, namespaceName, extensionName, targetPlatform, version), fileName);
    }

    public static String createApiFileUrl(String fileBaseUrl, String fileName) {
        var segments = ArrayUtils.addAll(new String[]{"file"}, fileName.split("/"));
        return createApiUrl(fileBaseUrl, segments);
    }

    public static String createApiFileBaseUrl(String baseUrl, ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        var namespaceName = extension.getNamespace().getName();
        return createApiFileBaseUrl(baseUrl, namespaceName, extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
    }

    public static String createApiFileBaseUrl(String baseUrl, String namespaceName, String extensionName, String targetPlatform, String version) {
        return createApiUrl(baseUrl, createApiVersionSegments(namespaceName, extensionName, targetPlatform, true, version));
    }

    public static String createApiVersionUrl(String baseUrl, ExtensionJson json) {
        return createApiVersionUrl(baseUrl, json.getNamespace(), json.getName(), json.getTargetPlatform(), json.getVersion());
    }

    public static String createApiVersionUrl(String baseUrl, ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        var namespace = extension.getNamespace();
        return createApiVersionUrl(baseUrl, namespace.getName(), extension.getName(), extVersion.getTargetPlatform(), extVersion.getVersion());
    }

    public static String createApiVersionUrl(String baseUrl, String namespaceName, String extensionName, String targetPlatform, String version) {
        return createApiUrl(baseUrl, createApiVersionSegments(namespaceName, extensionName, targetPlatform, false, version));
    }

    public static String createApiVersionBaseUrl(String baseUrl, String namespaceName, String extensionName, String targetPlatform) {
        return createApiUrl(baseUrl, createApiVersionSegments(namespaceName, extensionName, targetPlatform,false, null));
    }

    private static String[] createApiVersionSegments(String namespaceName, String extensionName, String targetPlatform, boolean excludeUniversalTargetPlatform, String version) {
        var segments = new String[]{ "api", namespaceName, extensionName };
        if(excludeUniversalTargetPlatform && TargetPlatform.isUniversal(targetPlatform)) {
            targetPlatform = null;
        }
        if(targetPlatform != null) {
            segments = ArrayUtils.add(segments, targetPlatform);
        }
        if(version != null) {
            segments = ArrayUtils.add(segments, version);
        }

        return segments;
    }

    /**
     * Create a URL pointing to an API path.
     */
    public static String createApiUrl(String baseUrl, String... segments) {
        if(Arrays.stream(segments).anyMatch(Objects::isNull)) {
            return null;
        }

        var path = Arrays.stream(segments)
                .filter(StringUtils::isNotEmpty)
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));

        if (baseUrl.isEmpty() || baseUrl.charAt(baseUrl.length() - 1) != '/') {
            baseUrl += '/';
        }

        return baseUrl + path;
    }

    public static String createApiReviewsUrl(String serverUrl, String namespace, String extension) {
        return createApiUrl(serverUrl, "api", namespace, extension, "reviews");
    }

    /**
     * Creates a URI of the form {@code baseUrl + '/' + encodedPath},
     * ensuring that only a single slash is between baseUrl and path.
     *
     * @param baseUrl the baseURL.
     * @param encodedPath the encoded path to append
     */
    public static URI createURI(String baseUrl, String encodedPath) {
        // ensure that the baseURL always ends with a '/'.
        if (baseUrl.isEmpty() || baseUrl.charAt(baseUrl.length() - 1) != '/') {
            baseUrl += '/';
        }

        // strip a preceding '/' from the path.
        if (encodedPath.startsWith("/")) {
            encodedPath = encodedPath.substring(1);
        }

        return URI.create(baseUrl + encodedPath);
    }

    /**
     * Add a query to a URL. The parameters array must contain a sequence of key and
     * value pairs, so its length is expected to be even.
     */
    public static String addQuery(String url, String... parameters) {
        if (parameters.length % 2 != 0)
            throw new IllegalArgumentException("Expected an even number of parameters.");

        var result = new StringBuilder(url);
        var printedParams = 0;
        for (var i = 0; i < parameters.length; i += 2) {
            var key = parameters[i];
            var value = parameters[i + 1];
            if (key == null)
                throw new NullPointerException("Parameter key must not be null");
            if (value != null) {
                if (printedParams == 0)
                    result.append('?');
                else
                    result.append('&');
                result.append(key).append('=').append(value);
                printedParams++;
            }
        }
        return result.toString();
    }

    /**
     * Get the base URL to use for API requests from configuration.
     * Preference order:
     * 1. ovsx.server.url (if configured and valid)
     * 2. ovsx.webui.url (if configured and valid)
     * 3. Request context (fallback, only works within a servlet request)
     * 
     * @return base URL, or empty string if no valid URL can be determined
     */
    public static String getBaseUrl() {
        // Try server URL first
        if (config != null) {
            String serverUrl = config.getServer().getUrl();
            if (isValidUrl(serverUrl)) {
                logger.debug("Using server URL from config: {}", serverUrl);
                return serverUrl;
            }
            
            // Fallback to webui URL
            String webuiUrl = config.getWebui().getUrl();
            if (isValidUrl(webuiUrl)) {
                logger.debug("Using webui URL from config: {}", webuiUrl);
                return webuiUrl;
            }
        }
        
        // Fallback to request context if available and config not set
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            logger.debug("No base URL configured, falling back to request context");
            return getBaseUrl(attrs.getRequest());
        }
        
        logger.warn("No base URL configured and no request context available");
        return "";
    }

    /**
     * Validate if a URL string is properly formatted and not empty.
     * 
     * @param url the URL string to validate
     * @return true if the URL is valid, false otherwise
     */
    private static boolean isValidUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        try {
            new URI(url);
            return true;
        } catch (URISyntaxException e) {
            logger.warn("Invalid URL format: {}", url, e);
            return false;
        }
    }

    protected static String getBaseUrl(HttpServletRequest request) {
        var url = new StringBuilder();

        var scheme = getBaseUrlScheme(request);
        url.append(scheme).append("://");

        var pair = getBaseUrlHostAndPort(request);
        url.append(pair.getFirst());
        var port = pair.getSecond();
        switch (scheme) {
            case "http":
                if (port != 80 && port > 0)
                    url.append(":").append(port);
                break;
            case "https":
                if (port != 443 && port > 0)
                    url.append(":").append(port);
                break;
            default:
                throw new IllegalArgumentException("Unsupported scheme: " + scheme);
        }

        url.append(getBaseUrlPrefix(request));
        url.append(request.getContextPath());
        return url.toString();
    }

    private static String getBaseUrlPrefix(HttpServletRequest request) {
        var servletPath = request.getServletPath();
        var pathInfo = request.getPathInfo();
        var requestUri = request.getRequestURI();
        var baseUrlPrefix = requestUri.substring(0, requestUri.length() - servletPath.length() - (pathInfo == null ? 0 : pathInfo.length()));
        return baseUrlPrefix;
    }

    private static String getBaseUrlScheme(HttpServletRequest request) {
        if ("on".equals(request.getHeader("X-SSL"))) {
            return "https";
        } else if ("https".equalsIgnoreCase(request.getScheme())) {
            return "https";
        }
        return "http";
    }

    public static Pair<String,Integer> getBaseUrlHostAndPort(HttpServletRequest request) {
        var host = request.getHeader("X-Forwarded-Host");
        if (host == null) {
            host = request.getHeader("Host");
        }
        if (host == null) {
            host = request.getServerName();
            var port = request.getServerPort();
            return Pair.of(host, port);
        }
        var colonIdx = host.lastIndexOf(':');
        if (colonIdx > 0) {
            try {
                var portStr = host.substring(colonIdx + 1);
                var port = Integer.parseInt(portStr);
                host = host.substring(0, colonIdx);
                return Pair.of(host, port);
            } catch (NumberFormatException e) {
                logger.warn("Invalid port in host header: {}", host, e);
            }
        }
        return Pair.of(host, -1);
    }

    public static String extractWildcardPath(HttpServletRequest request) {
        var pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return extractWildcardPath(request, pattern);
    }

    /**
     * Extract the rest ** of a wildcard path /<segments>/**
     * @param request incoming request
     * @param pattern ant pattern to match against
     * @return rest of the path
     */
    public static String extractWildcardPath(HttpServletRequest request, String pattern) {
        var path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var matcher = new AntPathMatcher();
        return matcher.extractPathWithinPattern(pattern, path);
    }

    public static String getPublicKeyUrl(ExtensionVersion extVersion) {
        var extension = extVersion.getExtension();
        var namespaceName = extension.getNamespace().getName();
        return getPublicKeyUrl(namespaceName + "." + extension.getName());
    }

    public static String getPublicKeyUrl(String publicId) {
        return createApiUrl(getBaseUrl(), "api", "-", "public-keys", publicId);
    }

    public static String createAllVersionsUrl(String namespaceName, String extensionName, String targetPlatform) {
        return createAllVersionsUrl(namespaceName, extensionName, targetPlatform, "versions");
    }

    public static String createAllVersionsUrl(String namespaceName, String extensionName, String targetPlatform, String versionsSegment) {
        return createApiUrl(getBaseUrl(), createApiVersionSegments(namespaceName, extensionName, targetPlatform, false, versionsSegment));
    }
}
