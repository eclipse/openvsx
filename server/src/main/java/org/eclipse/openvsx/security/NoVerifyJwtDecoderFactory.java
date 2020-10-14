/********************************************************************************
 * Copyright (c) 2020 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.security;

import java.net.URL;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;

import org.apache.jena.ext.com.google.common.collect.Maps;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.converter.ClaimConversionService;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;

public class NoVerifyJwtDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

    private static final Converter<Map<String, Object>, Map<String, Object>> DEFAULT_CLAIM_TYPE_CONVERTER
            = new ClaimTypeConverter(createDefaultClaimTypeConverters());

    public static Map<String, Converter<Object, ?>> createDefaultClaimTypeConverters() {
        var booleanConverter = getConverter(TypeDescriptor.valueOf(Boolean.class));
        var instantConverter = getConverter(TypeDescriptor.valueOf(Instant.class));
        var urlConverter = getConverter(TypeDescriptor.valueOf(URL.class));
        var stringConverter = getConverter(TypeDescriptor.valueOf(String.class));
        var collectionStringConverter = getConverter(TypeDescriptor.collection(Collection.class, TypeDescriptor.valueOf(String.class)));

        Map<String, Converter<Object, ?>> claimTypeConverters = Maps.newHashMapWithExpectedSize(10);
        claimTypeConverters.put(IdTokenClaimNames.ISS, urlConverter);
        claimTypeConverters.put(IdTokenClaimNames.AUD, collectionStringConverter);
        claimTypeConverters.put(IdTokenClaimNames.NONCE, stringConverter);
        claimTypeConverters.put(IdTokenClaimNames.EXP, instantConverter);
        claimTypeConverters.put(IdTokenClaimNames.IAT, instantConverter);
        claimTypeConverters.put(IdTokenClaimNames.AUTH_TIME, instantConverter);
        claimTypeConverters.put(IdTokenClaimNames.AMR, collectionStringConverter);
        claimTypeConverters.put(StandardClaimNames.EMAIL_VERIFIED, booleanConverter);
        claimTypeConverters.put(StandardClaimNames.PHONE_NUMBER_VERIFIED, booleanConverter);
        claimTypeConverters.put(StandardClaimNames.UPDATED_AT, instantConverter);
        return claimTypeConverters;
    }

    private static Converter<Object, ?> getConverter(TypeDescriptor targetDescriptor) {
        var sourceDescriptor = TypeDescriptor.valueOf(Object.class);
        return source -> ClaimConversionService.getSharedInstance().convert(source, sourceDescriptor, targetDescriptor);
    }

    @Override
    public JwtDecoder createDecoder(ClientRegistration context) {
        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                try {
                    var parsedJwt = JWTParser.parse(token);
                    if (parsedJwt instanceof PlainJWT) {
                        throw new JwtException("Unsupported algorithm of " + parsedJwt.getHeader().getAlgorithm());
                    }
                    Map<String, Object> headers = new LinkedHashMap<>(parsedJwt.getHeader().toJSONObject());
                    var claims = DEFAULT_CLAIM_TYPE_CONVERTER.convert(parsedJwt.getJWTClaimsSet().getClaims());
                    return Jwt.withTokenValue(token)
                            .headers(h -> h.putAll(headers))
                            .claims(c -> c.putAll(claims))
                            .build();
                } catch (Exception exc) {
                    throw new JwtException("An error occurred while attempting to decode the Jwt: " + exc.getMessage(), exc);
                }
            }
        };
    }

}