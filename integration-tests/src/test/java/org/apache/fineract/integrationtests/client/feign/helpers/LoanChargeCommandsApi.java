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
package org.apache.fineract.integrationtests.client.feign.helpers;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.apache.fineract.client.models.PostLoansLoanIdChargesChargeIdResponse;
import org.apache.fineract.client.models.PostLoansLoanIdChargesResponse;

/**
 * Feign interface for the two loan-charge request shapes the generated client cannot express. Both bind to the
 * generated response models, so the calls stay typed end to end; neither builds a JSON body by hand.
 *
 * <p>
 * These are the only loan-charge calls that need it - everything else goes through the generated
 * {@code fineractClient.loanCharges()} API.
 */
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface LoanChargeCommandsApi {

    /**
     * Waives a loan charge in full, sending <em>no</em> request body.
     *
     * <p>
     * The generated {@code executeLoanCharge} always serialises at least <code>{}</code>, which this endpoint answers
     * with a 500. The pre-migration test sent an empty body here, so this reproduces it exactly. A request model cannot
     * express "no body at all", which is why this needs its own declaration rather than a Swagger DTO change.
     *
     * <p>
     * Waiving a single installment of an instalment fee <em>does</em> take a body and goes through the generated
     * client's typed {@code waiveLoanCharge} instead.
     */
    @RequestLine("POST /v1/loans/{loanId}/charges/{loanChargeId}?command=waive")
    PostLoansLoanIdChargesChargeIdResponse waiveLoanChargeInFull(@Param("loanId") Long loanId, @Param("loanChargeId") Long loanChargeId);

    /**
     * Adds a loan charge whose {@code amount} is a locale-formatted decimal <em>string</em> (the German
     * {@code "50,05"}), which is what the server is being asked to parse here.
     *
     * <p>
     * {@code PostLoansLoanIdChargesRequest.amount} is a JSON number, so the generated model cannot carry a decimal
     * comma. Retyping it as a String at source is not the answer: that model is used by 118 call sites across
     * integration-tests, fineract-e2e-tests-core and fineract-e2e-tests-runner, all of which pass a number - the spec
     * would become a lie for every one of them to serve this single case. One schema cannot declare both "number" and
     * "locale-formatted string", so this operation gets its own request model.
     */
    @RequestLine("POST /v1/loans/{loanId}/charges")
    PostLoansLoanIdChargesResponse createLoanChargeWithLocaleFormattedAmount(@Param("loanId") Long loanId,
            LocaleFormattedAmountLoanChargeRequest request);

    /**
     * Request body for {@link #createLoanChargeWithLocaleFormattedAmount}. Mirrors the wire shape the pre-migration
     * test sent: both {@code amount} and {@code chargeId} are JSON strings, the former formatted for {@code locale}.
     */
    class LocaleFormattedAmountLoanChargeRequest {

        private String chargeId;
        private String amount;
        private String locale;
        private String dateFormat;

        public String getChargeId() {
            return chargeId;
        }

        public LocaleFormattedAmountLoanChargeRequest chargeId(String chargeId) {
            this.chargeId = chargeId;
            return this;
        }

        public String getAmount() {
            return amount;
        }

        public LocaleFormattedAmountLoanChargeRequest amount(String amount) {
            this.amount = amount;
            return this;
        }

        public String getLocale() {
            return locale;
        }

        public LocaleFormattedAmountLoanChargeRequest locale(String locale) {
            this.locale = locale;
            return this;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public LocaleFormattedAmountLoanChargeRequest dateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }
    }
}
