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
package org.apache.fineract.cob.workingcapitalloan.businessstep;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.workingcapitalloan.domain.WorkingCapitalLoan;
import org.apache.fineract.portfolio.workingcapitalloan.service.WorkingCapitalLoanPeriodPaymentRateRefreshService;
import org.springframework.stereotype.Component;

/**
 * Brings the loan's period payment rate up to date with its rate-change history.
 *
 * <p>
 * A rate change may be booked with a future effective date, and the rate stored on the loan is refreshed only when a
 * change is submitted. Without this step the loan would keep reporting the old rate after the effective date arrived —
 * the projected schedule already carries the new segment, so it is the summary on the loan that would drift.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PeriodPaymentRateEffectiveDateBusinessStep extends WorkingCapitalLoanCOBBusinessStep {

    private final WorkingCapitalLoanPeriodPaymentRateRefreshService rateRefreshService;

    @Override
    public WorkingCapitalLoan execute(final WorkingCapitalLoan input) {
        rateRefreshService.refreshPeriodPaymentRate(input);
        return input;
    }

    @Override
    public String getEnumStyledName() {
        return "WC_PERIOD_PAYMENT_RATE_EFFECTIVE_DATE";
    }

    @Override
    public String getHumanReadableName() {
        return "WC Period Payment Rate Effective Date";
    }
}
