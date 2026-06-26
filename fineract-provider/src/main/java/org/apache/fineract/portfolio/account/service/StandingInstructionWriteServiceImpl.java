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
package org.apache.fineract.portfolio.account.service;

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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.api.StandingInstructionApiConstants;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionCreationRequest;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionDeleteRequest;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionUpdatesRequest;
import org.apache.fineract.portfolio.account.data.response.StandingInstructionCreateResponse;
import org.apache.fineract.portfolio.account.data.response.StandingInstructionDeleteResponse;
import org.apache.fineract.portfolio.account.data.response.StandingInstructionUpdateResponse;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetailAssembler;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetailRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetails;
import org.apache.fineract.portfolio.account.domain.AccountTransferRecurrenceType;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.AccountTransferType;
import org.apache.fineract.portfolio.account.domain.StandingInstructionRepository;
import org.apache.fineract.portfolio.account.domain.StandingInstructionType;
import org.apache.fineract.portfolio.account.exception.StandingInstructionNotFoundException;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class StandingInstructionWriteServiceImpl implements StandingInstructionWriteService {

    private final AccountTransferDetailAssembler accountTransferDetailAssembler;
    private final AccountTransferDetailRepository accountTransferDetailRepository;
    private final StandingInstructionRepository standingInstructionRepository;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final LoanAssembler loanAccountAssembler;

    @Transactional
    @Override
    public StandingInstructionCreateResponse create(final StandingInstructionCreationRequest request) {
        final Long fromClientId = parseLong(request.getFromClientId());
        final Integer fromAccountType = parseInteger(request.getFromAccountType());
        final Integer toAccountType = parseInteger(request.getToAccountType());
        final Integer transferType = parseInteger(request.getTransferType());
        final Integer priority = parseInteger(request.getPriority());
        final Integer status = parseInteger(request.getStatus());
        final Integer instructionType = parseInteger(request.getInstructionType());
        final Integer recurrenceType = parseInteger(request.getRecurrenceType());
        final Integer recurrenceFrequency = parseInteger(request.getRecurrenceFrequency());
        final Integer recurrenceInterval = parseInteger(request.getRecurrenceInterval());
        final BigDecimal amount = parseBigDecimal(request.getAmount());
        final LocalDate validFrom = parseDate(request.getValidFrom(), request.getDateFormat(), request.getLocale());
        final LocalDate validTill = parseDate(request.getValidTill(), request.getDateFormat(), request.getLocale());
        final MonthDay recurrenceOnMonthDay = parseMonthDay(request.getRecurrenceOnMonthDay(), request.getMonthDayFormat(),
                request.getLocale());
        final String name = request.getName();

        validateForCreate(status, validFrom, validTill, amount, transferType, priority, instructionType, recurrenceType,
                recurrenceFrequency, recurrenceInterval, recurrenceOnMonthDay, name, fromAccountType, toAccountType);

        final PortfolioAccountType fromType = PortfolioAccountType.fromInt(fromAccountType);
        final PortfolioAccountType toType = PortfolioAccountType.fromInt(toAccountType);

        Long standingInstructionId = null;
        try {
            final AccountTransferDetails details = assembleAccountTransferDetails(parseLong(request.getFromAccountId()),
                    parseLong(request.getToAccountId()), transferType, fromType, toType);

            BigDecimal transferAmount = amount;
            if (transferAmount != null && details.fromSavingsAccount() != null) {
                transferAmount = Money.of(details.fromSavingsAccount().getCurrency(), transferAmount).getAmount();
            }

            final AccountTransferStandingInstruction standingInstruction = AccountTransferStandingInstruction.create(details, name,
                    priority, instructionType, status, transferAmount, validFrom, validTill, recurrenceType, recurrenceFrequency,
                    recurrenceInterval, recurrenceOnMonthDay);
            details.updateAccountTransferStandingInstruction(standingInstruction);

            this.accountTransferDetailRepository.saveAndFlush(details);
            standingInstructionId = details.accountTransferStandingInstruction().getId();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(name, dve.getMostSpecificCause(), dve);
        }

        return StandingInstructionCreateResponse.builder().resourceId(standingInstructionId).clientId(fromClientId).build();
    }

    @Transactional
    @Override
    public StandingInstructionUpdateResponse update(final StandingInstructionUpdatesRequest request) {
        final BigDecimal amount = parseBigDecimal(request.getAmount());
        final Integer priority = parseInteger(request.getPriority());
        final Integer status = parseInteger(request.getStatus());
        final Integer instructionType = parseInteger(request.getInstructionType());
        final Integer recurrenceType = parseInteger(request.getRecurrenceType());
        final Integer recurrenceFrequency = parseInteger(request.getRecurrenceFrequency());
        final Integer recurrenceInterval = parseInteger(request.getRecurrenceInterval());
        final LocalDate validFrom = parseDate(request.getValidFrom(), request.getDateFormat(), request.getLocale());
        final LocalDate validTill = parseDate(request.getValidTill(), request.getDateFormat(), request.getLocale());
        final MonthDay recurrenceOnMonthDay = parseMonthDay(request.getRecurrenceOnMonthDay(), request.getMonthDayFormat(),
                request.getLocale());

        validateForUpdate(validFrom, validTill, amount, status, priority, instructionType, recurrenceType, recurrenceFrequency,
                recurrenceInterval, request.getName());

        final AccountTransferStandingInstruction standingInstruction = this.standingInstructionRepository.findById(request.getId())
                .orElseThrow(() -> new StandingInstructionNotFoundException(request.getId()));

        final Map<String, Object> changes = standingInstruction.update(amount, validFrom, validTill, status, priority, instructionType,
                recurrenceType, recurrenceFrequency, recurrenceInterval, recurrenceOnMonthDay);

        if (!changes.isEmpty()) {
            this.standingInstructionRepository.save(standingInstruction);
        }

        return StandingInstructionUpdateResponse.builder().resourceId(request.getId()).changes(changes).build();
    }

    @Transactional
    @Override
    public StandingInstructionDeleteResponse delete(final StandingInstructionDeleteRequest request) {
        final AccountTransferStandingInstruction standingInstruction = this.standingInstructionRepository.findById(request.getId())
                .orElseThrow(() -> new StandingInstructionNotFoundException(request.getId()));
        standingInstruction.delete();
        this.standingInstructionRepository.save(standingInstruction);
        return StandingInstructionDeleteResponse.builder().resourceId(request.getId()).build();
    }

    private AccountTransferDetails assembleAccountTransferDetails(final Long fromAccountId, final Long toAccountId,
            final Integer transferType, final PortfolioAccountType fromAccountType, final PortfolioAccountType toAccountType) {
        if (PortfolioAccountType.SAVINGS.equals(fromAccountType) && PortfolioAccountType.SAVINGS.equals(toAccountType)) {
            final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromAccountId, false);
            final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toAccountId, false);
            return this.accountTransferDetailAssembler.assembleSavingsToSavingsTransfer(fromSavingsAccount, toSavingsAccount, transferType);
        } else if (PortfolioAccountType.SAVINGS.equals(fromAccountType) && PortfolioAccountType.LOAN.equals(toAccountType)) {
            final SavingsAccount fromSavingsAccount = this.savingsAccountAssembler.assembleFrom(fromAccountId, false);
            final Loan toLoanAccount = this.loanAccountAssembler.assembleFrom(toAccountId);
            return this.accountTransferDetailAssembler.assembleSavingsToLoanTransfer(fromSavingsAccount, toLoanAccount, transferType);
        } else if (PortfolioAccountType.LOAN.equals(fromAccountType) && PortfolioAccountType.SAVINGS.equals(toAccountType)) {
            final Loan fromLoanAccount = this.loanAccountAssembler.assembleFrom(fromAccountId);
            final SavingsAccount toSavingsAccount = this.savingsAccountAssembler.assembleFrom(toAccountId, false);
            return this.accountTransferDetailAssembler.assembleLoanToSavingsTransfer(fromLoanAccount, toSavingsAccount, transferType);
        }
        throw new PlatformDataIntegrityException("error.msg.standinginstruction.transfer.type.not.supported",
                "Transfer between the given account types is not supported for standing instructions");
    }

    private void validateForCreate(final Integer status, final LocalDate validFrom, final LocalDate validTill, final BigDecimal amount,
            final Integer transferType, final Integer priority, final Integer instructionType, final Integer recurrenceType,
            final Integer recurrenceFrequency, final Integer recurrenceInterval, final MonthDay recurrenceOnMonthDay, final String name,
            final Integer fromAccountType, final Integer toAccountType) {
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

    private void validateForUpdate(final LocalDate validFrom, final LocalDate validTill, final BigDecimal amount, final Integer status,
            final Integer priority, final Integer instructionType, final Integer recurrenceType, final Integer recurrenceFrequency,
            final Integer recurrenceInterval, final String name) {
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

    private void handleDataIntegrityIssues(final String name, final Throwable realCause, final NonTransientDataAccessException dve) {
        if (realCause.getMessage() != null && realCause.getMessage().contains("name")) {
            throw new PlatformDataIntegrityException("error.msg.standinginstruction.duplicate.name",
                    "Standinginstruction with name `" + name + "` already exists", "name", name);
        }
        log.error("Error occured.", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.client.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }

    private static Integer parseInteger(final String value) {
        return StringUtils.isBlank(value) ? null : Integer.valueOf(value.trim());
    }

    private static Long parseLong(final String value) {
        return StringUtils.isBlank(value) ? null : Long.valueOf(value.trim());
    }

    private static BigDecimal parseBigDecimal(final String value) {
        return StringUtils.isBlank(value) ? null : new BigDecimal(value.trim());
    }

    private static LocalDate parseDate(final String value, final String dateFormat, final String locale) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (StringUtils.isBlank(dateFormat)) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return LocalDate.parse(value, formatter(dateFormat, locale));
    }

    private static MonthDay parseMonthDay(final String value, final String monthDayFormat, final String locale) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        final String pattern = StringUtils.isBlank(monthDayFormat) ? "dd MMMM" : monthDayFormat;
        return MonthDay.parse(value, formatter(pattern, locale));
    }

    private static DateTimeFormatter formatter(final String pattern, final String locale) {
        final Locale loc = StringUtils.isBlank(locale) ? Locale.getDefault() : Locale.forLanguageTag(locale.replace('_', '-'));
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(loc);
    }
}
