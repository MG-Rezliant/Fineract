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

import static org.apache.fineract.portfolio.account.AccountDetailConstants.transferTypeParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.amountParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.instructionTypeParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.nameParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.priorityParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.recurrenceFrequencyParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.recurrenceIntervalParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.recurrenceOnMonthDayParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.recurrenceTypeParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.statusParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.validFromParamName;
import static org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants.validTillParamName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionCreationRequest;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionRequestParser;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionUpdatesRequest;
import org.apache.fineract.portfolio.account.domain.AccountTransferRecurrenceType;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.domain.StandingInstructionType;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.springframework.stereotype.Component;

/**
 * Validates the typed standing instruction request DTOs. Replaces the legacy {@code JsonCommand}-based
 * {@code StandingInstructionDataValidator}: the same field-level and cross-field integrity checks now run against the
 * typed {@link StandingInstructionCreationRequest} / {@link StandingInstructionUpdatesRequest}.
 */
@Component
public class StandingInstructionRequestDataValidator {

    public void validateForCreate(final StandingInstructionCreationRequest request) {
        final Integer status = StandingInstructionRequestParser.parseInteger(request.getStatus());
        final Integer transferType = StandingInstructionRequestParser.parseInteger(request.getTransferType());
        final Integer priority = StandingInstructionRequestParser.parseInteger(request.getPriority());
        final Integer instructionType = StandingInstructionRequestParser.parseInteger(request.getInstructionType());
        final Integer recurrenceType = StandingInstructionRequestParser.parseInteger(request.getRecurrenceType());
        final Integer recurrenceFrequency = StandingInstructionRequestParser.parseInteger(request.getRecurrenceFrequency());
        final Integer recurrenceInterval = StandingInstructionRequestParser.parseInteger(request.getRecurrenceInterval());
        final Integer fromAccountType = StandingInstructionRequestParser.parseInteger(request.getFromAccountType());
        final Integer toAccountType = StandingInstructionRequestParser.parseInteger(request.getToAccountType());
        final BigDecimal amount = StandingInstructionRequestParser.parseBigDecimal(request.getAmount());
        final LocalDate validFrom = StandingInstructionRequestParser.parseDate(request.getValidFrom(), request.getDateFormat(),
                request.getLocale());
        final LocalDate validTill = StandingInstructionRequestParser.parseDate(request.getValidTill(), request.getDateFormat(),
                request.getLocale());
        final MonthDay recurrenceOnMonthDay = StandingInstructionRequestParser.parseMonthDay(request.getRecurrenceOnMonthDay(),
                request.getMonthDayFormat(), request.getLocale());
        final String name = request.getName();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);

        baseDataValidator.reset().parameter(statusParamName).value(status).notNull().inMinMaxRange(1, 2);
        baseDataValidator.reset().parameter(validFromParamName).value(validFrom).notNull();
        baseDataValidator.reset().parameter(validTillParamName).value(validTill).validateDateAfter(validFrom);
        baseDataValidator.reset().parameter(amountParamName).value(amount).positiveAmount();
        baseDataValidator.reset().parameter(transferTypeParamName).value(transferType).notNull().inMinMaxRange(1, 3);
        baseDataValidator.reset().parameter(priorityParamName).value(priority).notNull().inMinMaxRange(1, 4);
        baseDataValidator.reset().parameter(instructionTypeParamName).value(instructionType).notNull().inMinMaxRange(1, 2);
        baseDataValidator.reset().parameter(recurrenceTypeParamName).value(recurrenceType).notNull().inMinMaxRange(1, 2);

        boolean isPeriodic = false;
        if (recurrenceType != null) {
            isPeriodic = AccountTransferRecurrenceType.fromInt(recurrenceType).isPeriodicRecurrence();
        }

        baseDataValidator.reset().parameter(recurrenceFrequencyParamName).value(recurrenceFrequency).inMinMaxRange(0, 3);
        if (recurrenceFrequency != null) {
            final PeriodFrequencyType frequencyType = PeriodFrequencyType.fromInt(recurrenceFrequency);
            if (frequencyType.isMonthly() || frequencyType.isYearly()) {
                baseDataValidator.reset().parameter(recurrenceOnMonthDayParamName).value(recurrenceOnMonthDay).notNull();
            }
        }

        if (isPeriodic) {
            baseDataValidator.reset().parameter(recurrenceIntervalParamName).value(recurrenceInterval).notNull();
            baseDataValidator.reset().parameter(recurrenceFrequencyParamName).value(recurrenceFrequency).notNull();
        }
        baseDataValidator.reset().parameter(recurrenceIntervalParamName).value(recurrenceInterval).integerGreaterThanZero();

        baseDataValidator.reset().parameter(nameParamName).value(name).notNull();

        if (toAccountType != null && PortfolioAccountType.SAVINGS.equals(PortfolioAccountType.fromInt(toAccountType))) {
            baseDataValidator.reset().parameter(instructionTypeParamName).value(instructionType).notNull().inMinMaxRange(1, 1);
            baseDataValidator.reset().parameter(recurrenceTypeParamName).value(recurrenceType).notNull().inMinMaxRange(1, 1);
        }
        if (instructionType != null && StandingInstructionType.fromInt(instructionType).isFixedAmoutTransfer()) {
            baseDataValidator.reset().parameter(amountParamName).value(amount).notNull();
        }
        if (instructionType != null && StandingInstructionType.fromInt(instructionType).isDuesAmoutTransfer()) {
            baseDataValidator.reset().parameter(amountParamName).value(amount).mustBeBlankWhenParameterProvidedIs(instructionTypeParamName,
                    instructionType);
        }

        if (transferType != null && fromAccountType != null && toAccountType != null) {
            final AccountTransferType accountTransferType = AccountTransferType.fromInt(transferType);
            final PortfolioAccountType fromPortfolioAccountType = PortfolioAccountType.fromInt(fromAccountType);
            final PortfolioAccountType toPortfolioAccountType = PortfolioAccountType.fromInt(toAccountType);
            String errorCode = null;
            if (accountTransferType.isAccountTransfer() && (PortfolioAccountType.LOAN.equals(fromPortfolioAccountType)
                    || PortfolioAccountType.LOAN.equals(toPortfolioAccountType))) {
                errorCode = "not.account.transfer";
            } else if (accountTransferType.isLoanRepayment() && (PortfolioAccountType.LOAN.equals(fromPortfolioAccountType)
                    || PortfolioAccountType.SAVINGS.equals(toPortfolioAccountType))) {
                errorCode = "not.loan.repayment";
            }
            if (errorCode != null) {
                baseDataValidator.reset().parameter(transferTypeParamName).failWithCode(errorCode);
            }
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateForUpdate(final StandingInstructionUpdatesRequest request) {
        final BigDecimal amount = StandingInstructionRequestParser.parseBigDecimal(request.getAmount());
        final Integer priority = StandingInstructionRequestParser.parseInteger(request.getPriority());
        final Integer status = StandingInstructionRequestParser.parseInteger(request.getStatus());
        final Integer instructionType = StandingInstructionRequestParser.parseInteger(request.getInstructionType());
        final Integer recurrenceType = StandingInstructionRequestParser.parseInteger(request.getRecurrenceType());
        final Integer recurrenceFrequency = StandingInstructionRequestParser.parseInteger(request.getRecurrenceFrequency());
        final Integer recurrenceInterval = StandingInstructionRequestParser.parseInteger(request.getRecurrenceInterval());
        final LocalDate validFrom = StandingInstructionRequestParser.parseDate(request.getValidFrom(), request.getDateFormat(),
                request.getLocale());
        final LocalDate validTill = StandingInstructionRequestParser.parseDate(request.getValidTill(), request.getDateFormat(),
                request.getLocale());
        final String name = request.getName();

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(StandingInstructionApiConstants.STANDING_INSTRUCTION_RESOURCE_NAME);

        if (validFrom != null) {
            baseDataValidator.reset().parameter(validFromParamName).value(validFrom).notNull();
        }
        if (validTill != null) {
            baseDataValidator.reset().parameter(validTillParamName).value(validTill).notNull();
        }
        if (amount != null) {
            baseDataValidator.reset().parameter(amountParamName).value(amount).positiveAmount();
        }
        if (status != null) {
            baseDataValidator.reset().parameter(statusParamName).value(status).notNull().inMinMaxRange(1, 2);
        }
        if (priority != null) {
            baseDataValidator.reset().parameter(priorityParamName).value(priority).notNull().inMinMaxRange(1, 4);
        }
        if (instructionType != null) {
            baseDataValidator.reset().parameter(instructionTypeParamName).value(instructionType).notNull().inMinMaxRange(1, 2);
        }
        if (recurrenceType != null) {
            baseDataValidator.reset().parameter(recurrenceTypeParamName).value(recurrenceType).notNull().inMinMaxRange(1, 2);
        }
        if (recurrenceFrequency != null) {
            baseDataValidator.reset().parameter(recurrenceFrequencyParamName).value(recurrenceFrequency).inMinMaxRange(0, 3);
        }
        if (recurrenceInterval != null) {
            baseDataValidator.reset().parameter(recurrenceIntervalParamName).value(recurrenceInterval).integerGreaterThanZero();
        }
        if (name != null) {
            baseDataValidator.reset().parameter(nameParamName).value(name).notNull();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
