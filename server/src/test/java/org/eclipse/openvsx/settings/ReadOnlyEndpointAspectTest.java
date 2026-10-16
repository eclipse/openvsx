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
package org.eclipse.openvsx.settings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ClassUtils;

import org.eclipse.openvsx.RegistryAPI;
import org.eclipse.openvsx.json.ResultJson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReadOnlyEndpointAspectTest {

    private final ReadOnlySetting readOnly = mock(ReadOnlySetting.class);
    private final ReadOnlyEndpointAspect advisor = new ReadOnlyEndpointAspect(provider(readOnly));

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ReadOnlySetting> provider(ReadOnlySetting readOnly) {
        var provider = (ObjectProvider<ReadOnlySetting>) mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(readOnly);
        return provider;
    }

    @Test
    void matchesEveryMutatingOperationInTheRegistry() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(
                (reader, factory) -> reader.getAnnotationMetadata()
                        .hasAnnotatedMethods(MutatingOperation.class.getName()));
        scanner.addExcludeFilter(new AssignableTypeFilter(ReadOnlyEndpointAspectTest.class));
        var unguarded = new ArrayList<String>();
        var guarded = 0;
        for (var candidate : scanner.findCandidateComponents("org.eclipse.openvsx")) {
            var type = ClassUtils.resolveClassName(candidate.getBeanClassName(), null);
            for (var method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(MutatingOperation.class)) {
                    continue;
                }
                if (advisor.getClassFilter().matches(type) && advisor.matches(method, type)) {
                    guarded++;
                } else {
                    unguarded.add(type.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertThat(unguarded).isEmpty();
        assertThat(guarded).isGreaterThan(40);
    }

    @Test
    void classFilterRejectsClassesNotNamedApi() {
        assertThat(advisor.getClassFilter().matches(RegistryAPI.class)).isTrue();
        assertThat(advisor.getClassFilter().matches(ReadOnlyEndpointAspectTest.class)).isFalse();
    }

    @Test
    void matchesOnlyMutatingOperationsReturningResponseEntity() {
        // By annotation and return type rather than a specific method: RegistryAPI's exact method
        // signatures (e.g. which token/header parameters they take) change independently of what
        // this advisor actually cares about.
        var methods = Arrays.stream(RegistryAPI.class.getMethods())
                .filter(m -> m.getReturnType() == ResponseEntity.class)
                .toList();
        var mutating = methods.stream().filter(m -> m.isAnnotationPresent(MutatingOperation.class)).findFirst();
        var readOnly = methods.stream().filter(m -> !m.isAnnotationPresent(MutatingOperation.class)).findFirst();

        assertThat(mutating).isPresent();
        assertThat(readOnly).isPresent();
        assertThat(advisor.matches(mutating.orElseThrow(), RegistryAPI.class)).isTrue();
        assertThat(advisor.matches(readOnly.orElseThrow(), RegistryAPI.class)).isFalse();
    }

    @Test
    void rejectsAMutatingOperationThatDoesNotReturnResponseEntity() throws NoSuchMethodException {
        var method = ReadOnlyEndpointAspectTest.class.getDeclaredMethod("dummyMutatingOperation");
        assertThat(advisor.matches(method, RegistryAPI.class)).isFalse();
    }

    @MutatingOperation
    private void dummyMutatingOperation() {
        // exists only to be reflected on above
    }

    @Test
    void blocksTheCallWhenTheRegistryIsReadOnly() throws Throwable {
        when(readOnly.isEnabled()).thenReturn(true);
        var invocation = mock(MethodInvocation.class);

        var result = (ResponseEntity<?>) interceptor().invoke(invocation);

        assertThat(result.getStatusCode().value()).isEqualTo(409);
        assertThat(((ResultJson) result.getBody()).getError()).isEqualTo("Registry is in read-only mode.");
        verify(invocation, never()).proceed();
    }

    @Test
    void proceedsWhenTheRegistryIsWritable() throws Throwable {
        when(readOnly.isEnabled()).thenReturn(false);
        var invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenReturn("ok");

        assertThat(interceptor().invoke(invocation)).isEqualTo("ok");
    }

    private MethodInterceptor interceptor() {
        return (MethodInterceptor) advisor.getAdvice();
    }
}
