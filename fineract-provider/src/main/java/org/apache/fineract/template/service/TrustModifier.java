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

// @rezliant RZ-A9FC1F63 · 2026-09-27 — Eliminates trust-all TLS bypass enabling MITM attacks
// This class provided insecure trust-all TLS bypass capability enabling man-in-the-middle attacks.
// Proper certificate validation using the platform default trust store is required.

/*
@rezliant-change-log:start
RZ-A9FC1F63 · 2026-09-27 · Insecure trust-all TLS bypass in AlwaysTrustManager and TrustingHostnameVerifier
Change: Removed entire TrustModifier class providing relaxHostChecking API with trust-all capability
Benefit: Eliminates callable MITM attack vector from distributable library
Scope: TrustModifier.java

Rezliant remediation history: 1 total · 1 most recent shown
@rezliant-change-log:end
*/