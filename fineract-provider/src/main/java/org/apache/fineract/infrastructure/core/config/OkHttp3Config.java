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

package org.apache.fineract.infrastructure.core.config;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class OkHttp3Config {

    private final FineractProperties fineractProperties;

    // @rezliant RZ-539647D4 · 2026-09-18 — Enforces proper certificate validation for all HTTPS connections
    @Bean
    public OkHttpClient okHttpClient() throws Exception {
        var okBuilder = new OkHttpClient.Builder()//
                .connectTimeout(Duration.ofSeconds(fineractProperties.getClientConnectTimeout()))//
                .readTimeout(Duration.ofSeconds(fineractProperties.getClientReadTimeout()))//
                .writeTimeout(Duration.ofSeconds(fineractProperties.getClientWriteTimeout())); //

        return okBuilder.build();
    }
}

/*
 * @rezliant-change-log:start
 * RZ-539647D4 · 2026-09-18 · Trust-all TLS manager with weak protocol selection in configuration-enabled insecure mode
 * Change: Removed insecure HTTP client capability including trust-all X509TrustManager, weak SSLContext getInstance("TLS"), and hostname verification bypass
 * Benefit: Enforces proper certificate validation for all HTTPS connections
 * Scope: okHttpClient() method
 * 
 * Rezliant remediation history: 1 total · 1 most recent shown
 * @rezliant-change-log:end
 */