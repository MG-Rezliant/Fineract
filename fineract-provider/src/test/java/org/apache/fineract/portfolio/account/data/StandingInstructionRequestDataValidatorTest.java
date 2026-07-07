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
package org.apache.fineract.portfolio.account.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionCreationRequest;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionUpdatesRequest;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StandingInstructionRequestDataValidator}, covering the four valid standing-instruction
 * combinations (Transfer Type x Instruction Type x Recurrence Type x Amount) and the amount integrity rules.
 */
public class StandingInstructionRequestDataValidatorTest {

    private static final String SAVINGS = "2";
    private static final String LOAN = "1";
    private static final String ACCOUNT_TRANSFER = "1";
    private static final String LOAN_REPAYMENT = "2";
    private static final String FIXED = "1";
    private static final String DUES = "2";
    private static final String PERIODIC = "1";
    private static final String AS_PER_DUES = "2";

    private final StandingInstructionRequestDataValidator validator = new StandingInstructionRequestDataValidator();

    /**
     * Baseline valid request: Account Transfer (savings -> savings) / Fixed / Periodic with an amount. Individual tests
     * mutate the fields under test.
     */
    private StandingInstructionCreationRequest baseCreate() {
        final StandingInstructionCreationRequest request = new StandingInstructionCreationRequest();
        request.setName("SI-test");
        request.setStatus("1");
        request.setPriority("1");
        request.setTransferType(ACCOUNT_TRANSFER);
        request.setFromAccountType(SAVINGS);
        request.setToAccountType(SAVINGS);
        request.setInstructionType(FIXED);
        request.setRecurrenceType(PERIODIC);
        request.setRecurrenceFrequency("0"); // daily -> no month/day required
        request.setRecurrenceInterval("1");
        request.setAmount("100");
        request.setValidFrom("2024-01-01");
        request.setValidTill("2024-12-31");
        return request;
    }

    // --- The four valid scenarios from the review (should all pass) ---

    @Test
    public void scenario1_accountTransferFixedPeriodicWithAmount_passes() {
        assertDoesNotThrow(() -> validator.validateForCreate(baseCreate()));
    }

    @Test
    public void scenario2_loanRepaymentFixedPeriodicWithAmount_passes() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setTransferType(LOAN_REPAYMENT);
        request.setToAccountType(LOAN);
        request.setInstructionType(FIXED);
        request.setRecurrenceType(PERIODIC);
        request.setAmount("100");
        assertDoesNotThrow(() -> validator.validateForCreate(request));
    }

    @Test
    public void scenario3_loanRepaymentDuesPeriodicWithoutAmount_passes() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setTransferType(LOAN_REPAYMENT);
        request.setToAccountType(LOAN);
        request.setInstructionType(DUES);
        request.setRecurrenceType(PERIODIC);
        request.setAmount(null);
        assertDoesNotThrow(() -> validator.validateForCreate(request));
    }

    @Test
    public void scenario4_loanRepaymentDuesAsPerDuesWithoutAmount_passes() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setTransferType(LOAN_REPAYMENT);
        request.setToAccountType(LOAN);
        request.setInstructionType(DUES);
        request.setRecurrenceType(AS_PER_DUES);
        request.setRecurrenceInterval(null); // not required for non-periodic recurrence
        request.setAmount(null);
        assertDoesNotThrow(() -> validator.validateForCreate(request));
    }

    // --- The newly restored amount integrity rule ---

    @Test
    public void duesInstructionWithAmount_isRejected() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setTransferType(LOAN_REPAYMENT);
        request.setToAccountType(LOAN);
        request.setInstructionType(DUES);
        request.setRecurrenceType(PERIODIC);
        request.setAmount("100"); // amount not allowed for Dues instruction type
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForCreate(request));
    }

    @Test
    public void fixedInstructionWithoutAmount_isRejected() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setTransferType(LOAN_REPAYMENT);
        request.setToAccountType(LOAN);
        request.setInstructionType(FIXED);
        request.setAmount(null); // amount required for Fixed instruction type
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForCreate(request));
    }

    // --- Field-level checks preserved from the legacy validator ---

    @Test
    public void statusOutOfRange_isRejected() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setStatus("5");
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForCreate(request));
    }

    @Test
    public void missingName_isRejected() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setName(null);
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForCreate(request));
    }

    @Test
    public void validTillBeforeValidFrom_isRejected() {
        final StandingInstructionCreationRequest request = baseCreate();
        request.setValidFrom("2024-12-31");
        request.setValidTill("2024-01-01");
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForCreate(request));
    }

    // --- Update path (partial) ---

    @Test
    public void updateWithinRanges_passes() {
        final StandingInstructionUpdatesRequest request = new StandingInstructionUpdatesRequest();
        request.setStatus("2");
        request.setPriority("3");
        request.setAmount("50");
        assertDoesNotThrow(() -> validator.validateForUpdate(request));
    }

    @Test
    public void updateStatusOutOfRange_isRejected() {
        final StandingInstructionUpdatesRequest request = new StandingInstructionUpdatesRequest();
        request.setStatus("9");
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForUpdate(request));
    }
}
