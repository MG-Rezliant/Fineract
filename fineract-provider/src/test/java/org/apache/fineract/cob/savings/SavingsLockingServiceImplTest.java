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
package org.apache.fineract.cob.savings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.cob.domain.LockOwner;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SavingsLockingServiceImplTest {

    private static final LocalDate COB_BUSINESS_DATE = LocalDate.parse("2026-06-09");

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private FineractProperties fineractProperties;
    @Mock
    private FineractProperties.FineractQueryProperties queryProperties;

    private MockedConstruction<NamedParameterJdbcTemplate> namedJdbcMock;
    private NamedParameterJdbcTemplate namedJdbc;
    private SavingsLockingServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(fineractProperties.getQuery()).thenReturn(queryProperties);
        lenient().when(queryProperties.getInClauseParameterSizeLimit()).thenReturn(1000);
        namedJdbcMock = mockConstruction(NamedParameterJdbcTemplate.class);
        service = new SavingsLockingServiceImpl(jdbcTemplate, fineractProperties);
        namedJdbc = namedJdbcMock.constructed().get(0);

        HashMap<BusinessDateType, LocalDate> dates = new HashMap<>();
        dates.put(BusinessDateType.COB_DATE, COB_BUSINESS_DATE);
        ThreadLocalContextUtil.setBusinessDates(dates);
    }

    @AfterEach
    void tearDown() {
        namedJdbcMock.close();
        ThreadLocalContextUtil.reset();
    }

    // ---------- applyLock ----------

    @Test
    void applyLockInsertsRowPerSavingsIdWithBusinessDate() throws Exception {
        List<Long> savingsIds = List.of(10L, 20L);

        service.applyLock(savingsIds, LockOwner.SAVINGS_COB_CHUNK_PROCESSING);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ParameterizedPreparedStatementSetter<Long>> setterCaptor = ArgumentCaptor
                .forClass(ParameterizedPreparedStatementSetter.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), eq(savingsIds), eq(savingsIds.size()), setterCaptor.capture());

        assertThat(normalize(sqlCaptor.getValue()))
                .isEqualTo("INSERT INTO m_savings_account_locks (savings_id, version, lock_owner, lock_placed_on, "
                        + "lock_placed_on_cob_business_date) VALUES (?,?,?,?,?)");

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps, 42L);
        verify(ps).setLong(1, 42L);
        verify(ps).setLong(2, 1L);
        verify(ps).setString(3, "SAVINGS_COB_CHUNK_PROCESSING");
        verify(ps).setObject(eq(4), any());
        verify(ps).setObject(5, COB_BUSINESS_DATE);
    }

    // ---------- upgradeLock ----------

    @Test
    void upgradeLockBatchUpdatesWithConfiguredChunkSize() throws Exception {
        List<Long> savingsIds = List.of(10L, 20L);
        when(queryProperties.getInClauseParameterSizeLimit()).thenReturn(500);

        service.upgradeLock(savingsIds, LockOwner.SAVINGS_COB_CHUNK_PROCESSING);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ParameterizedPreparedStatementSetter<Long>> setterCaptor = ArgumentCaptor
                .forClass(ParameterizedPreparedStatementSetter.class);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), eq(savingsIds), eq(500), setterCaptor.capture());

        assertThat(normalize(sqlCaptor.getValue())).isEqualTo(
                "UPDATE m_savings_account_locks SET version = version + 1, lock_owner = ?, lock_placed_on = ? WHERE savings_id = ?");

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps, 99L);
        verify(ps).setString(1, "SAVINGS_COB_CHUNK_PROCESSING");
        verify(ps).setObject(eq(2), any());
        verify(ps).setLong(3, 99L);
    }

    // ---------- updateLockError ----------

    @Test
    void updateLockErrorRunsParameterisedUpdate() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(1);

        service.updateLockError(77L, LockOwner.SAVINGS_COB_CHUNK_PROCESSING, "boom", "stack");

        verify(jdbcTemplate).update(
                "UPDATE m_savings_account_locks SET error = ?, stacktrace = ? WHERE savings_id = ? AND lock_owner = ?", "boom", "stack",
                77L, "SAVINGS_COB_CHUNK_PROCESSING");
    }

    @Test
    void updateLockErrorSucceedsSilentlyWhenNoRowMatches() {
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any())).thenReturn(0);

        service.updateLockError(77L, LockOwner.SAVINGS_COB_CHUNK_PROCESSING, "boom", "stack");

        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any());
    }

    // ---------- findLockIdsByLoanIdIn ----------

    @Test
    void findLockIdsByLoanIdInReturnsEmptyWithoutQueryingWhenInputEmpty() {
        List<Long> result = service.findLockIdsByLoanIdIn(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(namedJdbc);
    }

    @Test
    void findLockIdsByLoanIdInQueriesByIdList() {
        when(namedJdbc.queryForList(anyString(), anyMap(), eq(Long.class))).thenReturn(List.of(10L, 30L));

        List<Long> result = service.findLockIdsByLoanIdIn(List.of(10L, 20L, 30L));

        assertThat(result).containsExactly(10L, 30L);
        verify(namedJdbc).queryForList("SELECT savings_id FROM m_savings_account_locks WHERE savings_id IN (:ids)",
                Map.of("ids", List.of(10L, 20L, 30L)), Long.class);
    }

    // ---------- findLockIdsByLoanIdInAndLockOwner ----------

    @Test
    void findLockIdsByLoanIdInAndLockOwnerReturnsEmptyWithoutQueryingWhenInputEmpty() {
        List<Long> result = service.findLockIdsByLoanIdInAndLockOwner(List.of(), LockOwner.SAVINGS_COB_CHUNK_PROCESSING);

        assertThat(result).isEmpty();
        verifyNoInteractions(namedJdbc);
    }

    @Test
    void findLockIdsByLoanIdInAndLockOwnerQueriesByIdAndOwner() {
        when(namedJdbc.queryForList(anyString(), anyMap(), eq(Long.class))).thenReturn(List.of(20L));

        List<Long> result = service.findLockIdsByLoanIdInAndLockOwner(List.of(10L, 20L),
                LockOwner.SAVINGS_COB_CHUNK_PROCESSING);

        assertThat(result).containsExactly(20L);
        verify(namedJdbc).queryForList(
                "SELECT savings_id FROM m_savings_account_locks WHERE savings_id IN (:ids) AND lock_owner = :owner",
                Map.of("ids", List.of(10L, 20L), "owner", "SAVINGS_COB_CHUNK_PROCESSING"), Long.class);
    }

    // ---------- deleteByLoanIdInAndLockOwner ----------

    @Test
    void deleteByLoanIdInAndLockOwnerSkipsQueryWhenInputEmpty() {
        service.deleteByLoanIdInAndLockOwner(List.of(), LockOwner.SAVINGS_COB_CHUNK_PROCESSING);

        verifyNoInteractions(namedJdbc);
    }

    @Test
    void deleteByLoanIdInAndLockOwnerIssuesParameterisedDelete() {
        service.deleteByLoanIdInAndLockOwner(List.of(10L, 20L), LockOwner.SAVINGS_COB_CHUNK_PROCESSING);

        verify(namedJdbc).update("DELETE FROM m_savings_account_locks WHERE savings_id IN (:ids) AND lock_owner = :owner",
                Map.of("ids", List.of(10L, 20L), "owner", "SAVINGS_COB_CHUNK_PROCESSING"));
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
