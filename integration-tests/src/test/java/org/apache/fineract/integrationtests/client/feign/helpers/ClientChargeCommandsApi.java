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
import java.math.BigDecimal;
import org.apache.fineract.client.models.PostClientsClientIdChargesResponse;

/**
 * Applies a client charge whose {@code amount} carries decimal places.
 *
 * <p>
 * The server parses {@code amount} as a {@code BigDecimal} - rounding it to the currency's decimal places is exactly
 * what {@code ClientChargeRoundingTest} exercises - but {@code ClientChargesApiResourceSwagger} declares it as an
 * {@code Integer}, so the generated {@code PostClientsClientIdChargesRequest} cannot carry 19.876 or 0.55.
 *
 * <p>
 * The Swagger DTO is simply wrong here and correcting it is the right fix, but it is a breaking OpenAPI change
 * (swagger-brake R010, {@code integer} to {@code number}) and belongs in its own change rather than in a test
 * migration. Until then this keeps the call typed end to end, binding the response to the generated model.
 */
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface ClientChargeCommandsApi {

    @RequestLine("POST /v1/clients/{clientId}/charges")
    PostClientsClientIdChargesResponse createClientChargeWithDecimalAmount(@Param("clientId") Long clientId,
            DecimalAmountClientChargeRequest request);

    /**
     * Request body for {@link #createClientChargeWithDecimalAmount}. Mirrors the generated
     * {@code PostClientsClientIdChargesRequest} field for field, except that {@code amount} is a {@link BigDecimal}.
     */
    class DecimalAmountClientChargeRequest {

        private BigDecimal amount;
        private Long chargeId;
        private String dueDate;
        private String locale;
        private String dateFormat;

        public BigDecimal getAmount() {
            return amount;
        }

        public DecimalAmountClientChargeRequest amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Long getChargeId() {
            return chargeId;
        }

        public DecimalAmountClientChargeRequest chargeId(Long chargeId) {
            this.chargeId = chargeId;
            return this;
        }

        public String getDueDate() {
            return dueDate;
        }

        public DecimalAmountClientChargeRequest dueDate(String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public String getLocale() {
            return locale;
        }

        public DecimalAmountClientChargeRequest locale(String locale) {
            this.locale = locale;
            return this;
        }

        public String getDateFormat() {
            return dateFormat;
        }

        public DecimalAmountClientChargeRequest dateFormat(String dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }
    }
}
