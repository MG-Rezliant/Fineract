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

// @rezliant RZ-64FA0C8C · 2026-09-27 — Eliminates callable TLS bypass to prevent MITM attacks
// File removed - TrustModifier provided an insecure trust-all TLS bypass API.
// All certificate validation must now use platform defaults via JVM trust store.
// Callers previously using relaxHostChecking() must migrate to proper certificate validation.

/*
 * @rezliant-change-log:start
 * RZ-64FA0C8C · 2026-09-27 · Insecure trust manager and hostname verifier bypass
 * Change: Removed entire TrustModifier class containing AlwaysTrustManager and TrustingHostnameVerifier
 * Benefit: Eliminates callable TLS bypass to prevent MITM attacks
 * Scope: Complete file removal
 * 
 * Rezliant remediation history: 1 total · 1 most recent shown
 * @rezliant-change-log:end
 */