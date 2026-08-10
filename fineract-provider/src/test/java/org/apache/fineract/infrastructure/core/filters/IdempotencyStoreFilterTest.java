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

package org.apache.fineract.infrastructure.core.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.domain.FineractRequestContextHolder;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotencyStoreFilterTest {

    private static final String IDEMPOTENCY_KEY_HEADER_NAME = "Idempotency-Key";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private FineractRequestContextHolder fineractRequestContextHolder;

    @Mock
    private IdempotencyStoreHelper helper;

    @Mock
    private ConfigurationDomainService configurationDomainService;

    @Mock
    private FineractPlatformTenant mockTenant;

    private MockedStatic<ThreadLocalContextUtil> threadLocalContextUtilMock;

    private FineractProperties fineractProperties;

    private IdempotencyStoreFilter underTest;

    @BeforeEach
    void setUp() {
        threadLocalContextUtilMock = mockStatic(ThreadLocalContextUtil.class);
        threadLocalContextUtilMock.when(ThreadLocalContextUtil::getTenant).thenReturn(mockTenant);

        fineractProperties = new FineractProperties();
        fineractProperties.setIdempotencyKeyHeaderName(IDEMPOTENCY_KEY_HEADER_NAME);
        underTest = new IdempotencyStoreFilter(fineractRequestContextHolder, helper, fineractProperties, configurationDomainService);
    }

    @AfterEach
    void tearDown() {
        if (threadLocalContextUtilMock != null) {
            threadLocalContextUtilMock.close();
        }
    }

    @Test
    public void testDoFilterInternalShouldNotCheckHeaderWhenIdempotencyValidationIsDisabled() throws Exception {
        // given
        given(configurationDomainService.isIdempotencyValidationEnabled()).willReturn(false);

        // when
        underTest.doFilterInternal(request, response, filterChain);

        // then
        verify(configurationDomainService).isIdempotencyValidationEnabled();
        // isAllowedContentTypeRequest is still called once for the response-wrapper decision,
        // but never as part of the (skipped) idempotency-header warning check
        verify(helper, times(1)).isAllowedContentTypeRequest(request);
        verify(request, times(1)).getHeader(IDEMPOTENCY_KEY_HEADER_NAME);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternalShouldNotCheckHeaderWhenContentTypeIsNotAllowed() throws Exception {
        // given
        given(configurationDomainService.isIdempotencyValidationEnabled()).willReturn(true);
        given(helper.isAllowedContentTypeRequest(request)).willReturn(false);

        // when
        underTest.doFilterInternal(request, response, filterChain);

        // then
        verify(configurationDomainService).isIdempotencyValidationEnabled();
        // called twice: once for the (short-circuited) warning check, once for the wrapper decision
        verify(helper, times(2)).isAllowedContentTypeRequest(request);
        verify(request, times(1)).getHeader(IDEMPOTENCY_KEY_HEADER_NAME);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    public void testDoFilterInternalShouldCheckHeaderButNotWarnWhenHeaderIsPresent() throws Exception {
        // given
        given(configurationDomainService.isIdempotencyValidationEnabled()).willReturn(true);
        given(helper.isAllowedContentTypeRequest(request)).willReturn(true);
        given(request.getHeader(IDEMPOTENCY_KEY_HEADER_NAME)).willReturn("some-idempotency-key");

        // when
        underTest.doFilterInternal(request, response, filterChain);

        // then
        verify(request, times(2)).getHeader(IDEMPOTENCY_KEY_HEADER_NAME);
        verify(filterChain).doFilter(eq(request), any(HttpServletResponse.class));
    }

    @Test
    public void testDoFilterInternalShouldCompleteNormallyWhenValidationEnabledAndHeaderMissing() throws Exception {
        // given
        given(configurationDomainService.isIdempotencyValidationEnabled()).willReturn(true);
        given(helper.isAllowedContentTypeRequest(request)).willReturn(true);
        given(request.getHeader(IDEMPOTENCY_KEY_HEADER_NAME)).willReturn(null);

        // when
        underTest.doFilterInternal(request, response, filterChain);

        // then
        verify(request, times(2)).getHeader(IDEMPOTENCY_KEY_HEADER_NAME);
        verify(filterChain).doFilter(eq(request), any(HttpServletResponse.class));
        verifyNoInteractions(fineractRequestContextHolder);
    }
}
