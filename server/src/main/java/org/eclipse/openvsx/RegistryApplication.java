/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.eclipse.openvsx.mirror.ReadOnlyRequestFilter;
import org.eclipse.openvsx.web.LongRunningRequestFilter;
import org.eclipse.openvsx.web.ShallowEtagHeaderFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;

@SpringBootApplication
@EnableScheduling
@EnableRetry
@EnableAsync
@EnableCaching(proxyTargetClass = true)
public class RegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistryApplication.class, args);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        var registrationBean = new FilterRegistrationBean<ShallowEtagHeaderFilter>();
        registrationBean.setFilter(new ShallowEtagHeaderFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registrationBean;
    }

    @Bean
    @ConditionalOnProperty(value = "ovsx.request.duration.threshold")
    public FilterRegistrationBean<LongRunningRequestFilter> longRunningRequestFilter(@Value("${ovsx.request.duration.threshold}") long threshold) {
        var registrationBean = new FilterRegistrationBean<LongRunningRequestFilter>();
        registrationBean.setFilter(new LongRunningRequestFilter(threshold));
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registrationBean;
    }

    @Bean
    public RequestRejectedHandler requestRejectedHandler() {
        return new HttpStatusRequestRejectedHandler();
    }
    @ConditionalOnProperty(value = "ovsx.data.mirror.enabled", havingValue = "true")
    public FilterRegistrationBean<ReadOnlyRequestFilter> readOnlyRequestFilter(
            @Value("${ovsx.data.mirror.read-only.allowed-endpoints}") String[] allowedEndpoints,
            @Value("${ovsx.data.mirror.read-only.disallowed-methods}") String[] disallowedMethods
    ) {
        var registrationBean = new FilterRegistrationBean<ReadOnlyRequestFilter>();
        registrationBean.setFilter(new ReadOnlyRequestFilter(allowedEndpoints, disallowedMethods));
        registrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);

        return registrationBean;
    }
}
