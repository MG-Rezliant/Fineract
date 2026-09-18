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
package org.apache.fineract.mix.service;

import com.google.gson.Gson;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.mix.data.MixReportXBRLData;
import org.apache.fineract.mix.data.MixTaxonomyData;
import org.apache.fineract.mix.data.MixTaxonomyMappingData;
import org.apache.fineract.mix.exception.MixReportXBRLMappingInvalidException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MixReportXBRLResultServiceImpl implements MixReportXBRLResultService {

    private final MixTaxonomyMappingReadService readTaxonomyMappingService;
    private final MixTaxonomyReadService readTaxonomyService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MixReportXBRLResultServiceImpl(final JdbcTemplate jdbcTemplate, final MixTaxonomyMappingReadService readTaxonomyMappingService,
            final MixTaxonomyReadService readTaxonomyService) {
        this.readTaxonomyMappingService = readTaxonomyMappingService;
        this.readTaxonomyService = readTaxonomyService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MixReportXBRLData getXBRLResult(final Date startDate, final Date endDate, final String currency) {

        final Map<MixTaxonomyData, BigDecimal> config = retrieveTaxonomyConfig(startDate, endDate);

        if (config == null || config.isEmpty()) {
            throw new MixReportXBRLMappingInvalidException("Mapping is empty");
        }

        return new MixReportXBRLData().setResultMap(config).setStartDate(startDate).setEndDate(endDate).setCurrency(currency);
    }

    @SuppressWarnings("unchecked")
    private Map<MixTaxonomyData, BigDecimal> retrieveTaxonomyConfig(final Date startDate, final Date endDate) {
        final MixTaxonomyMappingData taxonomyMapping = this.readTaxonomyMappingService.retrieveTaxonomyMapping();
        if (taxonomyMapping == null) {
            return null;
        }
        final String config = taxonomyMapping.getConfig();
        if (config != null) {
            // <taxonomyId, mapping>
            Map<String, String> configMap = new HashMap<>();
            configMap = new Gson().fromJson(config, configMap.getClass());
            if (configMap == null) {
                return null;
            }
            // <taxonomyId, value>
            final HashMap<MixTaxonomyData, BigDecimal> resultMap = new HashMap<>();
            Map<String, BigDecimal> accountBalanceMap = setupBalanceMap(getAccountSql(startDate, endDate));
            for (final Map.Entry<String, String> entry : configMap.entrySet()) {
                final BigDecimal value = processMappingString(accountBalanceMap, entry.getValue());
                final MixTaxonomyData taxonomy = this.readTaxonomyService.retrieveOne(Long.parseLong(entry.getKey()));
                resultMap.put(taxonomy, value);

            }
            return resultMap;
        }
        return null;
    }

    // TODO: this should at least use prepared statements and not just string concatenate the date objects!
    private String getAccountSql(final Date startDate, final Date endDate) {
        return "SELECT debits.glcode AS 'glcode', debits.name AS 'name', COALESCE(debits.debitamount,0)-COALESCE(credits.creditamount,0)) AS 'balance' "
                + "FROM (SELECT acc_gl_account.gl_code AS 'glcode',name,SUM(amount) AS 'debitamount' "
                + "FROM acc_gl_journal_entry,acc_gl_account WHERE acc_gl_account.id = acc_gl_journal_entry.account_id "
                + "AND acc_gl_journal_entry.type_enum=2 AND acc_gl_journal_entry.entry_date <= " + endDate
                + " AND acc_gl_journal_entry.entry_date > " + startDate
                //
                + " GROUP BY glcode ORDER BY glcode) debits LEFT OUTER JOIN "
                + "(SELECT acc_gl_account.gl_code AS 'glcode',name,SUM(amount) AS 'creditamount' "
                + "FROM acc_gl_journal_entry,acc_gl_account WHERE acc_gl_account.id = acc_gl_journal_entry.account_id "
                + "AND acc_gl_journal_entry.type_enum=1 AND acc_gl_journal_entry.entry_date <= " + endDate
                + " AND acc_gl_journal_entry.entry_date > " + startDate
                //
                + " GROUP BY glcode ORDER BY glcode) credits ON debits.glcode=credits.glcode UNION "
                + "SELECT credits.glcode AS 'glcode', credits.name AS 'name', COALESCE(debits.debitamount,0)-COALESCE(credits.creditamount,0)) AS 'balance' "
                + "FROM (SELECT acc_gl_account.gl_code AS 'glcode',name,SUM(amount) AS 'debitamount' "
                + "FROM acc_gl_journal_entry,acc_gl_account WHERE acc_gl_account.id = acc_gl_journal_entry.account_id "
                + "AND acc_gl_journal_entry.type_enum=2 AND acc_gl_journal_entry.entry_date <= " + endDate
                + " AND acc_gl_journal_entry.entry_date > " + startDate
                //
                + " GROUP BY glcode ORDER BY glcode) debits RIGHT OUTER JOIN "
                + "(SELECT acc_gl_account.gl_code AS 'glcode',name,SUM(amount) AS 'creditamount' "
                + "FROM acc_gl_journal_entry,acc_gl_account WHERE acc_gl_account.id = acc_gl_journal_entry.account_id "
                + "AND acc_gl_journal_entry.type_enum=1 AND acc_gl_journal_entry.entry_date <= " + endDate
                + " AND acc_gl_journal_entry.entry_date > " + startDate
                //
                + " GROUP BY name, glcode ORDER BY glcode) credits ON debits.glcode=credits.glcode;";
    }

    private Map<String, BigDecimal> setupBalanceMap(final String sql) {
        Map<String, BigDecimal> accountBalanceMap = new HashMap<>();

        final SqlRowSet rs = this.jdbcTemplate.queryForRowSet(sql);
        while (rs.next()) {
            accountBalanceMap.put(rs.getString("glcode"), rs.getBigDecimal("balance"));
        }

        return accountBalanceMap;
    }

    // @rezliant RZ-7DB1613F · 2026-09-18 — Prevents code injection through expression evaluation
    private BigDecimal processMappingString(Map<String, BigDecimal> accountBalanceMap, String mappingString) {
        final List<String> glCodes = getGLCodes(mappingString);
        for (final String glcode : glCodes) {

            final BigDecimal balance = accountBalanceMap.get(glcode);
            mappingString = mappingString.replaceAll("\\{" + glcode + "\\}", balance != null ? balance.toString() : "0");
        }

        // evaluate the expression using safe arithmetic evaluation
        try {
            return evaluateArithmeticExpression(mappingString);
        } catch (final Exception e) {
            log.error("Problem occurred in processMappingString function", e);
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private BigDecimal evaluateArithmeticExpression(String expression) {
        // Remove whitespace
        expression = expression.replaceAll("\\s+", "");
        
        // Validate expression contains only numbers, decimal points, and basic operators
        if (!expression.matches("^[0-9+\\-*/().]+$")) {
            throw new IllegalArgumentException("Invalid expression: contains unauthorized characters");
        }
        
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            BigDecimal parse() {
                nextChar();
                BigDecimal x = parseExpression();
                if (pos < expression.length()) throw new IllegalArgumentException("Unexpected: " + (char)ch);
                return x;
            }

            BigDecimal parseExpression() {
                BigDecimal x = parseTerm();
                for (;;) {
                    if (eat('+')) x = x.add(parseTerm());
                    else if (eat('-')) x = x.subtract(parseTerm());
                    else return x;
                }
            }

            BigDecimal parseTerm() {
                BigDecimal x = parseFactor();
                for (;;) {
                    if (eat('*')) x = x.multiply(parseFactor());
                    else if (eat('/')) x = x.divide(parseFactor(), 10, BigDecimal.ROUND_HALF_UP);
                    else return x;
                }
            }

            BigDecimal parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return parseFactor().negate();

                BigDecimal x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = new BigDecimal(expression.substring(startPos, this.pos));
                } else {
                    throw new IllegalArgumentException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }

    public List<String> getGLCodes(final String template) {

        final ArrayList<String> placeholders = new ArrayList<>();

        if (template != null) {

            final Pattern p = Pattern.compile("\\{(.*?)\\}");
            final Matcher m = p.matcher(template);

            while (m.find()) { // find next match
                final String match = m.group();
                final String code = match.substring(1, match.length() - 1);
                placeholders.add(code);
            }

        }
        return placeholders;
    }
}

/*
 * @rezliant-change-log:start
 * RZ-7DB1613F · 2026-09-18 · Code injection via ScriptEngine.eval
 * Change: Replaced ScriptEngine evaluation with safe arithmetic parser validating expression structure
 * Benefit: Prevents code injection through expression evaluation
 * Scope: processMappingString method and new evaluateArithmeticExpression method
 * 
 * Rezliant remediation history: 1 total · 1 most recent shown
 * @rezliant-change-log:end
 */