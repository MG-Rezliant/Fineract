/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.core.jersey;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

@Provider
@Produces(MediaType.APPLICATION_JSON_VALUE)
@Consumes(MediaType.APPLICATION_JSON_VALUE)
@Component
@RequiredArgsConstructor
public class JerseyJacksonObjectArgumentHandler<T> implements MessageBodyReader<T>, MessageBodyWriter<T> {

    // The Jersey REST layer stays on Jackson 2; Boot 4 no longer auto-configures the Jackson 2
    // HTTP message converter bean, so bind directly to the Jersey object mapper instead
    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType) {
        return true;
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        if (String.class == genericType) {
            // If the request type is String, keep it that way.
            StringWriter writer = new StringWriter();
            IOUtils.copy(entityStream, writer, UTF_8);
            String json = writer.toString();
            return type.cast(json);
        } else {
            // Create the proper type from the JSON
            try {
                return (T) objectMapper.readValue(entityStream, objectMapper.getTypeFactory().constructType(genericType));
            } catch (JsonProcessingException e) {
                // preserve the contract of the removed MappingJackson2HttpMessageConverter: malformed or unbindable
                // JSON must surface as HttpMessageNotReadableException so HttpMessageNotReadableErrorController
                // renders the platform JSON validation error instead of a raw Jackson message
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(httpHeaders);
                throw new HttpMessageNotReadableException("JSON parse error: " + e.getOriginalMessage(), e, new HttpInputMessage() {

                    @Override
                    public InputStream getBody() {
                        return entityStream;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        return headers;
                    }
                });
            }
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, jakarta.ws.rs.core.MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        if (String.class == genericType) {
            // If the response type is String, keep it that way.
            IOUtils.write((String) t, entityStream, UTF_8);
        } else {
            // Create the proper JSON string from the object
            objectMapper.writeValue(entityStream, t);
        }
    }
}
