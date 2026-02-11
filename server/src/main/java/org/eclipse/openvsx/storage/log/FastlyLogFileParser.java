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
package org.eclipse.openvsx.storage.log;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nullable;
import java.io.IOException;

class FastlyLogFileParser implements LogFileParser {
    private final Logger logger = LoggerFactory.getLogger(FastlyLogFileParser.class);

    private final ObjectMapper mapper;

    public FastlyLogFileParser() {
        this.mapper = new ObjectMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(LogRecord.class, new LogRecordDeserializer());
        mapper.registerModule(module);
    }

    @Override
    public @Nullable LogRecord parse(String line) {
        try {
            var jsonStartIndex = line.indexOf("{");
            if (jsonStartIndex != -1) {
                return mapper.readValue(line.substring(jsonStartIndex), LogRecord.class);
            } else {
                return null;
            }
        } catch (JacksonException ex) {
            logger.error("could not parse log line '{}'", line, ex);
            return null;
        }
    }
}

class LogRecordDeserializer extends StdDeserializer<LogRecord> {

    public LogRecordDeserializer() {
        this(null);
    }

    public LogRecordDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LogRecord deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        String operation = node.get("request_method").asText();
        int status = (Integer) node.get("response_status").numberValue();
        String url = node.get("url").asText();
        return new LogRecord(operation, status, url);
    }
}
