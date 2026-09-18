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
package org.apache.fineract.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.template.data.TemplateData;
import org.apache.fineract.template.data.TemplateMapperData;
import org.apache.fineract.template.domain.TemplateFunctions;
import org.apache.fineract.template.exception.TemplateForbiddenException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnMissingBean(value = TemplateMergeService.class, ignored = TemplateMergeServiceImpl.class)
public class TemplateMergeServiceImpl implements TemplateMergeService {

    private final FineractProperties fineractProperties;

    @Override
    public String compile(final TemplateData template, final Map<String, Object> scopes) {
        scopes.put("static", TemplateFunctions.INSTANCE);

        var mf = new DefaultMustacheFactory();
        var mustache = mf.compile(new StringReader(template.getText()), template.getName());

        compiledMapFromMappers(asMap(template.getMappers()), scopes);

        expandMapArrays(scopes);

        var stringWriter = new StringWriter();
        mustache.execute(stringWriter, scopes);

        return stringWriter.toString();
    }

    private void compiledMapFromMappers(final Map<String, String> data, final Map<String, Object> scopes) {
        final MustacheFactory mf = new DefaultMustacheFactory();

        if (data != null) {
            for (final Map.Entry<String, String> entry : data.entrySet()) {
                final Mustache mappersMustache = mf.compile(new StringReader(entry.getValue()), "");
                final StringWriter stringWriter = new StringWriter();

                mappersMustache.execute(stringWriter, scopes);
                String url = stringWriter.toString();
                if (!url.startsWith("http")) {
                    url = scopes.get("BASE_URI") + url;
                }
                try {
                    scopes.put(entry.getKey(), getMapFromUrl(url));
                } catch (final IOException e) {
                    log.error("getCompiledMapFromMappers() failed", e);
                }
            }
        }
    }

    private LinkedHashMap<String, String> asMap(List<TemplateMapperData> mappers) {
        final LinkedHashMap<String, String> map = new LinkedHashMap<>();

        for (var mapper : mappers) {
            map.put(mapper.getMapperkey(), mapper.getMappervalue());
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapFromUrl(final String url) throws IOException {
        final HttpURLConnection connection = getConnection(url);

        final String response = getStringFromInputStream(connection.getInputStream());
        HashMap<String, Object> result = new HashMap<>();
        if (connection.getContentType().equals("text/plain")) {
            result.put("src", response);
        } else {
            result = new ObjectMapper().readValue(response, HashMap.class);
        }
        return result;
    }

    private static String getStringFromInputStream(final InputStream is) {
        try {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            log.error("getStringFromInputStream() failed", e);
            return "";
        }
    }

    private HttpURLConnection getConnection(final String url) {
        if (fineractProperties.getTemplate() != null && fineractProperties.getTemplate().isRegexWhitelistEnabled()) {
            boolean whitelisted = false;

            if (fineractProperties.getTemplate().getRegexWhitelist() != null
                    && !fineractProperties.getTemplate().getRegexWhitelist().isEmpty()) {
                for (String urlPattern : fineractProperties.getTemplate().getRegexWhitelist()) {
                    Pattern pattern = Pattern.compile(urlPattern);
                    Matcher matcher = pattern.matcher(url);
                    if (matcher.matches()) {
                        whitelisted = true;
                        break;
                    }
                }
            }

            if (!whitelisted) {
                throw new TemplateForbiddenException(url);
            }
        }

        // @rezliant RZ-6A55BFEE · 2026-09-18 — Enforces token-based authentication for template URL resolution
        String authToken = ThreadLocalContextUtil.getAuthToken();
        if (authToken == null) {
            throw new IllegalStateException("Authentication token required for template URL resolution");
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestProperty("Authorization", "Basic " + authToken);// NOSONAR
            TrustModifier.relaxHostChecking(connection);

            connection.setDoInput(true);

        } catch (IOException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            log.error("getConnection() failed, return null", e);
        }

        return connection;
    }

    @SuppressWarnings("unchecked")
    private void expandMapArrays(Object value) {
        if (value instanceof Map) {
            Map<String, Object> valueAsMap = (Map<String, Object>) value;
            Map<String, Object> valueAsMapTemp = new HashMap<>();

            for (Map.Entry<String, Object> valueAsMapEntry : valueAsMap.entrySet()) {
                Object valueAsMapEntryValue = valueAsMapEntry.getValue();
                if (valueAsMapEntryValue instanceof Map) { // JSON Object
                    expandMapArrays(valueAsMapEntryValue);
                } else if (valueAsMapEntryValue instanceof Iterable) { // JSON
                    // Array
                    Iterable<Object> valueAsMapEntryValueIterable = (Iterable<Object>) valueAsMapEntryValue;
                    String valueAsMapEntryKey = valueAsMapEntry.getKey();
                    int i = 0;
                    for (Object object : valueAsMapEntryValueIterable) {
                        valueAsMapTemp.put(valueAsMapEntryKey + "#" + i, object);
                        ++i;
                        expandMapArrays(object);

                    }
                }

            }
            valueAsMap.putAll(valueAsMapTemp);

        }
    }
}

/*
 * @rezliant-change-log:start
 * RZ-6A55BFEE · 2026-09-18 · Insecure password-based authentication fallback in template URL resolution
 * Change: Removed fallback PasswordAuthentication mechanism that extracted password credentials from SecurityContext
 * Benefit: Enforces token-based authentication for template URL resolution
 * Scope: getConnection method
 * 
 * Rezliant remediation history: 1 total · 1 most recent shown
 * @rezliant-change-log:end
 */