// File: fineract-provider/src/main/java/org/apache/fineract/template/service/TrustModifier.java
// @rezliant RZ-F9CAC31F · 2026-09-27 — Enforces proper certificate validation by removing trust-all bypass

// This file has been removed. Applications must use proper certificate validation
// via the default TrustManagerFactory and platform trust store.

/*
 * @rezliant-change-log:start
 * RZ-F9CAC31F · 2026-09-27 · Insecure trust-all TLS implementation
 * Change: Removed entire TrustModifier class that disabled certificate validation
 * Benefit: Enforces proper certificate validation by eliminating trust-all bypass
 * Scope: TrustModifier class
 * 
 * Rezliant remediation history: 1 total · 1 most recent shown
 * @rezliant-change-log:end
 */