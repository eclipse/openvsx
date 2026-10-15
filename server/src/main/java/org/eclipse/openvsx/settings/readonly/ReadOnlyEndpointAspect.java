/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package org.eclipse.openvsx.settings.readonly;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.function.SingletonSupplier;

import org.eclipse.openvsx.RegistryAPI;
import org.eclipse.openvsx.UserAPI;
import org.eclipse.openvsx.json.ResultJson;

/**
 * A hand-written {@link StaticMethodMatcherPointcutAdvisor} rather than an AspectJ {@code @Aspect}: an
 * AspectJ {@code execution()} pointcut has to spin up its own PointcutParser/World and scan the classpath
 * to resolve types on every candidate bean, on every Spring context boot - this was the single largest
 * cost in the test suite's context startup time (roughly 70% of it), since this aspect (unlike the other
 * four in {@code mirror.aop}) has no {@code @ConditionalOnProperty} and so is always active. A plain
 * Method/Class check has none of that cost.
 * <p>
 * {@link ReadOnlySetting} is looked up lazily: any bean implementing {@code Advisor} (this one
 * included, via {@link StaticMethodMatcherPointcutAdvisor}) is resolved eagerly by Spring's auto-proxying
 * machinery before all {@code BeanPostProcessor}s are registered, and {@code ReadOnlySetting}'s
 * own dependency chain reaches all the way to the JPA/DataSource (and therefore Flyway) infrastructure -
 * pulling that in this early produced a
 * "not eligible for getting processed by all BeanPostProcessors" warning at startup. Deferring the
 * lookup until the advice actually fires keeps this bean itself cheap to construct. An
 * {@link ObjectProvider} rather than a {@code @Lazy} proxy keeps that deferral visible at the call site,
 * and {@link SingletonSupplier} keeps the lookup to one, where the provider alone would repeat it on
 * every guarded call.
 * <p>
 * {@code @Role(ROLE_INFRASTRUCTURE)}: this bean is itself resolved before all {@code BeanPostProcessor}s
 * are registered (see above) and is never meant to be proxied/advised by anything else, which otherwise
 * trips the same "not eligible for getting processed by all BeanPostProcessors" warning - this is
 * Spring's own documented way to tell that check the early creation is expected.
 */
@Component
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ReadOnlyEndpointAspect extends StaticMethodMatcherPointcutAdvisor {

    public ReadOnlyEndpointAspect(ObjectProvider<ReadOnlySetting> readOnly) {
        super(interceptor(SingletonSupplier.of(readOnly::getObject)));
        setClassFilter(
                type -> type == RegistryAPI.class || type == UserAPI.class
                        || (type.getPackageName().equals("org.eclipse.openvsx.admin")
                                && type.getSimpleName().endsWith("API")));
    }

    private static MethodInterceptor interceptor(SingletonSupplier<ReadOnlySetting> readOnly) {
        return invocation -> readOnly.obtain().isEnabled()
                ? ResponseEntity.status(409).body(ResultJson.error("Registry is in read-only mode."))
                : invocation.proceed();
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.getReturnType() == ResponseEntity.class && method.isAnnotationPresent(MutatingOperation.class);
    }
}
