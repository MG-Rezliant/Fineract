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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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

    // Modified by Rezilant AI, 2026-08-19 16:55:11 GMT, Removed insecure X509TrustManager that blindly accepts all certificates
    // Original Code
    // @SuppressWarnings("unused")
    // private static final X509TrustManager insecureX509TrustManager = new X509TrustManager() {
    //
    //     @Override
    //     public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {}// NOSONAR
    //
    //     @Override
    //     public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {}// NOSONAR
    //
    //     @Override
    //     public X509Certificate[] getAcceptedIssuers() {
    //         return new X509Certificate[] {};
    //     }
    // };

    /**
     * Configure HTTP client to be "insecure", as in skipping host SSL certificate verification. While this can be
     * useful during development e.g. when using self-signed certificates, it should never be enabled in production (due
     * to "man in the middle").
     */
    private final boolean insecureHttpClient = Boolean.getBoolean("fineract.insecureHttpClient");
    private final SSLContext insecureSSLContext;

    public ProcessorHelper() throws KeyManagementException, NoSuchAlgorithmException {
        if (insecureHttpClient) {
            insecureSSLContext = createInsecureSSLContext();
        } else {
            insecureSSLContext = null;
        }
    }

    private OkHttpClient createClient() {
        var okBuilder = new OkHttpClient.Builder();
        if (insecureHttpClient) {
            configureInsecureClient(okBuilder);
        }
        return okBuilder.build();
    }

    private void configureInsecureClient(final OkHttpClient.Builder okBuilder) {
        // Modified by Rezilant AI, 2026-08-19 16:55:11 GMT, Updated to use proper TrustManagerFactory with secure certificate validation
        try {
            // Load the default trust store (contains trusted CA certificates)
            javax.net.ssl.TrustManagerFactory trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
                javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
            );
            trustManagerFactory.init((java.security.KeyStore) null); // Uses default system trust store

            // Get the trust managers
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            // Find X509TrustManager from the array
            X509TrustManager x509TrustManager = null;
            for (TrustManager tm : trustManagers) {
                if (tm instanceof X509TrustManager) {
                    x509TrustManager = (X509TrustManager) tm;
                    break;
                }
            }

            if (x509TrustManager != null) {
                okBuilder.sslSocketFactory(insecureSSLContext.getSocketFactory(), x509TrustManager);
            } else {
                LOG.error("No X509TrustManager found in default trust managers");
            }
        } catch (Exception e) {
            LOG.error("Failed to configure secure SSL context", e);
        }
        // Original Code
        // okBuilder.sslSocketFactory(insecureSSLContext.getSocketFactory(), insecureX509TrustManager);
        HostnameVerifier insecureHostnameVerifier = (hostname, session) -> true;// NOSONAR
        okBuilder.hostnameVerifier(insecureHostnameVerifier);
    }

    private SSLContext createInsecureSSLContext() throws NoSuchAlgorithmException, KeyManagementException {
        // Modified by Rezilant AI, 2026-08-19 16:55:11 GMT, Initialize SSL context with proper trust managers for secure certificate validation
        SSLContext sslContext = SSLContext.getInstance("TLS");
        
        // Load the default trust store (contains trusted CA certificates)
        javax.net.ssl.TrustManagerFactory trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
            javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
        );
        trustManagerFactory.init((java.security.KeyStore) null); // Uses default system trust store

        // Get the trust managers
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

        // Initialize SSL context with proper trust managers
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext;
        // Original Code
        // SSLContext insecureSSLContext = SSLContext.getInstance("TLS"); // TODO "TLS" or "SSL" as in
        // // FineractClient.Builder?
        // insecureSSLContext.init(null, new TrustManager[] { insecureX509TrustManager }, new SecureRandom());
        // return insecureSSLContext;
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