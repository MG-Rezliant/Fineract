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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.data.StandingInstructionRequestDataValidator;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionCreationRequest;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionDeleteRequest;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionRequestParser;
import org.apache.fineract.portfolio.account.data.request.StandingInstructionUpdatesRequest;
import org.apache.fineract.portfolio.account.data.response.StandingInstructionCreateResponse;
import org.apache.fineract.portfolio.account.data.response.StandingInstructionDeleteResponse;
import org.apache.fineract.portfolio.account.data.response.StandingInstructionUpdateResponse;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetailAssembler;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetailRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetails;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.StandingInstructionRepository;
import org.apache.fineract.portfolio.account.exception.StandingInstructionNotFoundException;
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

    private final StandingInstructionRequestDataValidator standingInstructionRequestDataValidator;
    private final AccountTransferDetailAssembler accountTransferDetailAssembler;
    private final AccountTransferDetailRepository accountTransferDetailRepository;
    private final StandingInstructionRepository standingInstructionRepository;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final LoanAssembler loanAccountAssembler;

    @Transactional
    @Override
    public StandingInstructionCreateResponse create(final StandingInstructionCreationRequest request) {
        this.standingInstructionRequestDataValidator.validateForCreate(request);

        final Long fromClientId = StandingInstructionRequestParser.parseLong(request.getFromClientId());
        final Integer fromAccountType = StandingInstructionRequestParser.parseInteger(request.getFromAccountType());
        final Integer toAccountType = StandingInstructionRequestParser.parseInteger(request.getToAccountType());
        final Integer transferType = StandingInstructionRequestParser.parseInteger(request.getTransferType());
        final Integer priority = StandingInstructionRequestParser.parseInteger(request.getPriority());
        final Integer status = StandingInstructionRequestParser.parseInteger(request.getStatus());
        final Integer instructionType = StandingInstructionRequestParser.parseInteger(request.getInstructionType());
        final Integer recurrenceType = StandingInstructionRequestParser.parseInteger(request.getRecurrenceType());
        final Integer recurrenceFrequency = StandingInstructionRequestParser.parseInteger(request.getRecurrenceFrequency());
        final Integer recurrenceInterval = StandingInstructionRequestParser.parseInteger(request.getRecurrenceInterval());
        final BigDecimal amount = StandingInstructionRequestParser.parseBigDecimal(request.getAmount());
        final LocalDate validFrom = StandingInstructionRequestParser.parseDate(request.getValidFrom(), request.getDateFormat(),
                request.getLocale());
        final LocalDate validTill = StandingInstructionRequestParser.parseDate(request.getValidTill(), request.getDateFormat(),
                request.getLocale());
        final MonthDay recurrenceOnMonthDay = StandingInstructionRequestParser.parseMonthDay(request.getRecurrenceOnMonthDay(),
                request.getMonthDayFormat(), request.getLocale());
        final String name = request.getName();

        final PortfolioAccountType fromType = PortfolioAccountType.fromInt(fromAccountType);
        final PortfolioAccountType toType = PortfolioAccountType.fromInt(toAccountType);

        Long standingInstructionId = null;
        try {
            final AccountTransferDetails details = assembleAccountTransferDetails(
                    StandingInstructionRequestParser.parseLong(request.getFromAccountId()),
                    StandingInstructionRequestParser.parseLong(request.getToAccountId()), transferType, fromType, toType);

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
        this.standingInstructionRequestDataValidator.validateForUpdate(request);

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
        final MonthDay recurrenceOnMonthDay = StandingInstructionRequestParser.parseMonthDay(request.getRecurrenceOnMonthDay(),
                request.getMonthDayFormat(), request.getLocale());

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

    private void handleDataIntegrityIssues(final String name, final Throwable realCause, final NonTransientDataAccessException dve) {
        if (realCause.getMessage() != null && realCause.getMessage().contains("name")) {
            throw new PlatformDataIntegrityException("error.msg.standinginstruction.duplicate.name",
                    "Standinginstruction with name `" + name + "` already exists", "name", name);
        }
        log.error("Error occured.", dve);
        throw ErrorHandler.getMappable(dve, "error.msg.client.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }
}
