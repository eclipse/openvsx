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

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;
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
        var connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.of(connectTimeout, TimeUnit.MILLISECONDS))
                .setSocketTimeout(Timeout.of(socketTimeout, TimeUnit.MILLISECONDS))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotal);
        connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        connectionManager.setDefaultConnectionConfig(connectionConfig);
        return new HttpConnPoolConfig(
                connectionManager,
                connectionRequestTimeout
        );
    }

    @Bean
    public RestTemplate restTemplate(HttpConnPoolConfig foregroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(foregroundHttpConnPool).build();
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        var restTemplate = new RestTemplate(factory);
        restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(),
                new JacksonJsonHttpMessageConverter()));
        return restTemplate;
    }

    @Bean
    public RestTemplate nonRedirectingRestTemplate(HttpConnPoolConfig foregroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(foregroundHttpConnPool).disableRedirectHandling().build();
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        return new RestTemplate(factory);
    }

    @Bean
    public RestTemplate backgroundRestTemplate(HttpConnPoolConfig backgroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(backgroundHttpConnPool).build();
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        var restTemplate = new RestTemplate(factory);
        var defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);
        restTemplate.setMessageConverters(List.of(
                new StringHttpMessageConverter(),
                new JacksonJsonHttpMessageConverter()));
        return restTemplate;
    }

    @Bean
    public RestTemplate backgroundNonRedirectingRestTemplate(HttpConnPoolConfig backgroundHttpConnPool) {
        var httpClient = createHttpClientBuilder(backgroundHttpConnPool).disableRedirectHandling().build();
        var factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        var restTemplate = new RestTemplate(factory);
        var defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);
        return restTemplate;
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
                .build();
        return HttpClientBuilder
                .create()
                .setConnectionManager(httpConnPoolConfig.getConnectionManager())
                .setDefaultRequestConfig(requestConfig);
    }

    public static class HttpConnPoolConfig {

        private final PoolingHttpClientConnectionManager connectionManager;
        private final int connectionRequestTimeout;

        public HttpConnPoolConfig(PoolingHttpClientConnectionManager connectionManager, int connectionRequestTimeout) {
            this.connectionManager = connectionManager;
            this.connectionRequestTimeout = connectionRequestTimeout;
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
    }
}
