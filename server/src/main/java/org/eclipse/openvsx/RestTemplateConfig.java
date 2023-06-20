/** ******************************************************************************
 * Copyright (c) 2022 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    /**
     * Use to serve requests to ensure that response is given within 30 seconds.
     * VS Code does not wait more than it and will timeout a request.
     */
    @Bean
    public HttpConnPoolConfig foregroundHttpConnPool(
            @Value("${ovsx.foregroundHttpConnPool.maxTotal:20}") int maxTotal,
            @Value("${ovsx.foregroundHttpConnPool.defaultMaxPerRoute:20}") int defaultMaxPerRoute,
            @Value("${ovsx.foregroundHttpConnPool.connectionRequestTimeout:10000}") int connectionRequestTimeout,
            @Value("${ovsx.foregroundHttpConnPool.connectTimeout:10000}") int connectTimeout,
            @Value("${ovsx.foregroundHttpConnPool.socketTimeout:10000}") int socketTimeout
    ) {
        return createHttpConnPoolConfig(maxTotal, defaultMaxPerRoute, connectionRequestTimeout, connectTimeout, socketTimeout);
    }

    /**
     * Use to download files in background processing for requests not requiring redirects.
     * Never use to serve requests. Overall response time should be within 30secs.
     */
    @Bean
    public HttpConnPoolConfig backgroundHttpConnPool(
            @Value("${ovsx.backgroundHttpConnPool.maxTotal:20}") int maxTotal,
            @Value("${ovsx.backgroundHttpConnPool.defaultMaxPerRoute:20}") int defaultMaxPerRoute,
            @Value("${ovsx.backgroundHttpConnPool.connectionRequestTimeout:30000}") int connectionRequestTimeout,
            @Value("${ovsx.backgroundHttpConnPool.connectTimeout:30000}") int connectTimeout,
            @Value("${ovsx.backgroundHttpConnPool.socketTimeout:60000}") int socketTimeout
    ) {
        return createHttpConnPoolConfig(maxTotal, defaultMaxPerRoute, connectionRequestTimeout, connectTimeout, socketTimeout);
    }

    private HttpConnPoolConfig createHttpConnPoolConfig(int maxTotal, int defaultMaxPerRoute, int connectionRequestTimeout, int connectTimeout, int socketTimeout) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotal);
        connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        return new HttpConnPoolConfig(
                connectionManager,
                connectionRequestTimeout,
                connectTimeout,
                socketTimeout
        );
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, HttpConnPoolConfig foregroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(foregroundHttpConnPool).build();
        return builder
                .requestFactory(() -> {
                    HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                    f.setHttpClient(httpClient);
                    return f;
                })
                .messageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Bean
    public RestTemplate nonRedirectingRestTemplate(RestTemplateBuilder builder, HttpConnPoolConfig foregroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(foregroundHttpConnPool).disableRedirectHandling().build();
        return builder
                .requestFactory(() -> {
                    HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                    f.setHttpClient(httpClient);
                    return f;
                })
                .build();
    }

    @Bean
    public RestTemplate backgroundRestTemplate(RestTemplateBuilder builder, HttpConnPoolConfig backgroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(backgroundHttpConnPool).build();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return builder
                .uriTemplateHandler(defaultUriBuilderFactory)
                .messageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter())
                .requestFactory(() -> {
                    HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                    f.setHttpClient(httpClient);
                    return f;
                })
                .build();
    }

    @Bean
    public RestTemplate backgroundNonRedirectingRestTemplate(RestTemplateBuilder builder, HttpConnPoolConfig backgroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(backgroundHttpConnPool).disableRedirectHandling().build();
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        return builder
                .uriTemplateHandler(defaultUriBuilderFactory)
                .requestFactory(() -> {
                    HttpComponentsClientHttpRequestFactory f = new HttpComponentsClientHttpRequestFactory();
                    f.setHttpClient(httpClient);
                    return f;
                })
                .build();
    }

    @Bean
    public RestTemplate vsCodeIdRestTemplate(
            @Value("${ovsx.data.mirror.enabled:false}") boolean mirrorModeEnabled,
            RestTemplate restTemplate,
            RestTemplate backgroundRestTemplate
    ) {
        return mirrorModeEnabled ? backgroundRestTemplate : restTemplate;
    }

    private HttpClientBuilder createHttpClientBuilder(HttpConnPoolConfig httpConnPoolConfig) {
        var requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.of(httpConnPoolConfig.getConnectionRequestTimeout(), TimeUnit.MILLISECONDS))
                .setConnectTimeout(Timeout.of(httpConnPoolConfig.getConnectTimeout(), TimeUnit.MILLISECONDS))
                .build();
        return HttpClientBuilder
                .create()
                .setConnectionManager(httpConnPoolConfig.getConnectionManager())
                .setDefaultRequestConfig(requestConfig);
    }

    private static class HttpConnPoolConfig {

        private final PoolingHttpClientConnectionManager connectionManager;
        private final int connectionRequestTimeout;
        private final int connectTimeout;
        private final int socketTimeout;

        public HttpConnPoolConfig(PoolingHttpClientConnectionManager connectionManager, int connectionRequestTimeout,
                                  int connectTimeout, int socketTimeout) {
            this.connectionManager = connectionManager;
            this.connectionRequestTimeout = connectionRequestTimeout;
            this.connectTimeout = connectTimeout;
            this.socketTimeout = socketTimeout;
        }

        public PoolingHttpClientConnectionManager getConnectionManager() {
            return connectionManager;
        }
        /**
         *  the time to wait for a connection from the connection manager/pool
         */
        public int getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }
        /**
         * the time to establish the connection with the remote host
         */
        public int getConnectTimeout() {
            return connectTimeout;
        }
        /**
         * the time waiting for data – after establishing the connection; maximum time of inactivity between two data packets
         */
        public int getSocketTimeout() {
            return socketTimeout;
        }

    }
}
