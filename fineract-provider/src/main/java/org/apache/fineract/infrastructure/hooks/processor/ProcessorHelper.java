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
package org.apache.fineract.infrastructure.hooks.processor;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
public final class ProcessorHelper {

    // Nota bene: Similar code to insecure HTTPS is also in Fineract Client's
    // org.apache.fineract.client.util.FineractClient.Builder.insecure()

    private static final Logger LOG = LoggerFactory.getLogger(ProcessorHelper.class);

    // @rezliant RZ-666B87DE · 2026-09-18 — Enforces platform certificate validation for all webhook HTTPS connections
    public ProcessorHelper() {
    }

    private OkHttpClient createClient() {
        return new OkHttpClient.Builder().build();
    }

    @SuppressWarnings("rawtypes")
    public Callback createCallback(final String url) {
        return new Callback() {

            @Override
            public void onResponse(@SuppressWarnings("unused") Call call, retrofit2.Response response) {
                LOG.debug("URL: {} - Status: {}", url, response.code());
            }

            @Override
            public void onFailure(@SuppressWarnings("unused") Call call, Throwable t) {
                LOG.error("URL: {} - Retrofit failure occurred", url, t);
            }
        };
    }

    public WebHookService createWebHookService(final String url) {
        final OkHttpClient client = createClient();
        final Retrofit.Builder retrofitBuilder = new Retrofit.Builder();
        retrofitBuilder.baseUrl(url);
        retrofitBuilder.client(client);
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
        final Retrofit retrofit = retrofitBuilder.build();
        return retrofit.create(WebHookService.class);
    }

    @SuppressWarnings("rawtypes")
    public Callback createCallback(final String url, String payload) {

        return new Callback() {

            @Override
            public void onResponse(@SuppressWarnings("unused") Call call, retrofit2.Response response) {
                LOG.debug("URL: {} - Status: {}", url, response.code());
            }

            @Override
            public void onFailure(@SuppressWarnings("unused") Call call, Throwable t) {
                LOG.error("URL: {} - Retrofit failure occured", url, t);
            }
        };
    }
}

// @rezliant-change-log:start
// RZ-666B87DE · 2026-09-18 · Trust-all TLS bypass capability present in production-distributable code controlled by runtime flag
// Change: Removed insecureX509TrustManager, insecureSSLContext field, insecureHttpClient flag check, configureInsecureClient() and createInsecureSSLContext() methods
// Benefit: Enforces platform certificate validation for all webhook HTTPS connections
// Scope: ProcessorHelper class constructor and createClient() method
//
// Rezliant remediation history: 1 total · 1 most recent shown
// @rezliant-change-log:end