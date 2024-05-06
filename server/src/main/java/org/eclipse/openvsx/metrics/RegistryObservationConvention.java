/** ******************************************************************************
 * Copyright (c) 2024 Precies. Software Ltd and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 * ****************************************************************************** */
package org.eclipse.openvsx.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.aop.ObservedAspect;
import org.aspectj.lang.reflect.MethodSignature;

public class RegistryObservationConvention implements ObservationConvention<ObservedAspect.ObservedAspectContext> {

    private ObjectMapper mapper;

    public RegistryObservationConvention() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ObservedAspect.ObservedAspectContext context) {
//        var joinPoint = context.getProceedingJoinPoint();
//        var args = joinPoint.getArgs();
//        var methodSignature = (MethodSignature) joinPoint.getSignature();
//        var parameterNames = methodSignature.getParameterNames();
//        var argKeyValues = new KeyValue[args.length];
//        for(var i = 0; i < args.length; i++) {
//            var key = "args." + parameterNames[i];
//            var value = convertObjectToString(args[i]);
//            argKeyValues[i] = KeyValue.of(key, value);
//        }

        return ObservationConvention.super.getHighCardinalityKeyValues(context);//.and(argKeyValues);
    }

    private String convertObjectToString(Object arg) {
        if(arg instanceof String) {
            return (String) arg;
        } else if(arg instanceof Number || arg instanceof Boolean) {
            return String.valueOf(arg);
        } else {
            try {
                return mapper.writeValueAsString(arg);
            } catch (JsonProcessingException e) {
                return  "";
            }
        }
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ObservedAspect.ObservedAspectContext;
    }

    @Override
    public String getName() {
        return "org.eclipse.openvsx.observed";
    }

    @Override
    public String getContextualName(ObservedAspect.ObservedAspectContext context) {
        var methodSignature = (MethodSignature) context.getProceedingJoinPoint().getSignature();
        var method = methodSignature.getMethod();
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName();
    }
}
