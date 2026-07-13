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
package org.apache.fineract.commands.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandSourceRepository extends JpaRepository<CommandSource, Long>, JpaSpecificationExecutor<CommandSource> {

    CommandSource findByActionNameAndEntityNameAndIdempotencyKey(String actionName, String entityName, String idempotencyKey);

    @Query(value = """
            select distinct c.* from m_portfolio_command_source c
            where upper(c.action_name) = upper(?1)
            and upper(c.entity_name) = upper(?2)
            and (
                c.resource_id = ?3
                or c.subresource_id = ?3
                or c.client_id = ?3
                or c.loan_id = ?3
                or c.savings_account_id = ?3
                or c.group_id = ?3
                or c.office_id = ?3
                or c.product_id = ?3
                or c.creditbureau_id = ?3
                or c.organisation_creditbureau_id = ?3
            )
            and c.status = ?4
            order by c.made_on_date_utc desc
            """, nativeQuery = true)
    List<CommandSource> findPendingByActionAndEntityAndResource(@Param("actionName") String actionName,
            @Param("entityName") String entityName, @Param("resourceId") Long resourceId, @Param("status") Integer status);

    @Modifying(flushAutomatically = true)
    @Query("delete from CommandSource c where c.status = :status and c.madeOnDate is not null and c.madeOnDate <= :dateForPurgeCriteria")
    void deleteOlderEventsWithStatus(@Param("status") Integer status, @Param("dateForPurgeCriteria") OffsetDateTime dateForPurgeCriteria);

}
