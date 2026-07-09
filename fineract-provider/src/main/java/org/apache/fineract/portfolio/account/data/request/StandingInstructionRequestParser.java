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
package org.apache.fineract.portfolio.account.data.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

/**
 * Locale-aware parsing helpers for the {@code String}-based standing instruction request DTOs. Shared by
 * {@code StandingInstructionRequestDataValidator} (validation) and {@code StandingInstructionWriteServiceImpl} (entity
 * assembly) so both interpret the raw request values identically.
 */
public final class StandingInstructionRequestParser {

    private StandingInstructionRequestParser() {}

    public static Integer parseInteger(final String value) {
        return StringUtils.isBlank(value) ? null : Integer.valueOf(value.trim());
    }

    public static Long parseLong(final String value) {
        return StringUtils.isBlank(value) ? null : Long.valueOf(value.trim());
    }

    public static BigDecimal parseBigDecimal(final String value) {
        return StringUtils.isBlank(value) ? null : new BigDecimal(value.trim());
    }

    public static LocalDate parseDate(final String value, final String dateFormat, final String locale) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (StringUtils.isBlank(dateFormat)) {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return LocalDate.parse(value, formatter(dateFormat, locale));
    }

    public static MonthDay parseMonthDay(final String value, final String monthDayFormat, final String locale) {
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
