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

import com.fasterxml.jackson.annotation.JsonInclude;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.apache.fineract.client.models.PutLoanProductsProductIdResponse;

/**
 * Loan-product updates that have to send an explicit JSON {@code null}.
 *
 * <p>
 * {@code LoanProductWritePlatformServiceJpaRepositoryImpl} gates the update on
 * {@code command.parameterExists("delinquencyBucketId")}, so clearing the bucket requires the key to be
 * <em>present</em> with a null value. The generated {@code PutLoanProductsProductIdRequest} cannot express that: its
 * properties are {@code @JsonInclude(USE_DEFAULTS)} and {@code ObjectMapperFactory} sets {@code NON_NULL} globally, so
 * a null field is dropped from the body and the server never takes the clearing branch.
 *
 * <p>
 * Marking the field {@code nullable} in the Swagger DTO does not help - the generator emits a plain
 * {@code @Nullable Long}, not a {@code JsonNullable}, so serialisation is unchanged. A property-level
 * {@code @JsonInclude(ALWAYS)} is what actually overrides the mapper default, hence this request model.
 */
@Headers({ "Accept: application/json", "Content-Type: application/json" })
public interface LoanProductCommandsApi {

    @RequestLine("PUT /v1/loanproducts/{productId}")
    PutLoanProductsProductIdResponse clearDelinquencyBucket(@Param("productId") Long productId, ClearDelinquencyBucketRequest request);

    /**
     * Body for {@link #clearDelinquencyBucket}: serialises to {@code {"delinquencyBucketId":null}}.
     */
    class ClearDelinquencyBucketRequest {

        @JsonInclude(JsonInclude.Include.ALWAYS)
        private final Long delinquencyBucketId = null;

        public Long getDelinquencyBucketId() {
            return delinquencyBucketId;
        }
    }
}
