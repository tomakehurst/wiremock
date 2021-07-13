/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.common;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Arrays.asList;
import static java.util.Locale.US;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Date;
import java.util.List;

public class DateTimeParser {

  private static final DateTimeFormatter RFC_1036_DATE_TIME =
      DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz").withLocale(US);
  private static final DateTimeFormatter ASCTIME1 =
      DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy").withZone(ZoneId.of("GMT"));
  private static final DateTimeFormatter ASCTIME2 =
      DateTimeFormatter.ofPattern("EEE MMM  d HH:mm:ss yyyy").withZone(ZoneId.of("GMT"));

  public static final List<DateTimeParser> ZONED_PARSERS =
      asList(
          DateTimeParser.forFormatter(ISO_ZONED_DATE_TIME),
          DateTimeParser.forFormatter(RFC_1123_DATE_TIME),
          DateTimeParser.forFormatter(RFC_1036_DATE_TIME),
          DateTimeParser.forFormatter(ASCTIME1),
          DateTimeParser.forFormatter(ASCTIME2));

  private final DateTimeFormatter dateTimeFormatter;
  private final boolean isUnix;
  private final boolean isEpoch;

  private DateTimeParser(DateTimeFormatter dateTimeFormatter, boolean isUnix, boolean isEpoch) {
    this.dateTimeFormatter = dateTimeFormatter;
    this.isUnix = isUnix;
    this.isEpoch = isEpoch;
  }

  public static DateTimeParser forFormat(String format) {
    if (format.equalsIgnoreCase("unix")) {
      return new DateTimeParser(null, true, false);
    }

    if (format.equalsIgnoreCase("epoch")) {
      return new DateTimeParser(null, false, true);
    }

    return DateTimeParser.forFormatter(DateTimeFormatter.ofPattern(format));
  }

  public static DateTimeParser forFormatter(DateTimeFormatter dateTimeFormatter) {
    return new DateTimeParser(dateTimeFormatter, false, false);
  }

  public ZonedDateTime parseZonedDateTime(String dateTimeString) {
    if (dateTimeFormatter != null) {
      return ZonedDateTime.parse(dateTimeString, dateTimeFormatter);
    }

    if (isUnix) {
      long epochMillis = Long.parseLong(dateTimeString) * 1000;
      return Instant.ofEpochMilli(epochMillis).atZone(UTC);
    }

    if (isEpoch) {
      long epochMillis = Long.parseLong(dateTimeString);
      return Instant.ofEpochMilli(epochMillis).atZone(UTC);
    }

    return null;
  }

  public LocalDateTime parseLocalDateTime(String dateTimeString) {
    if (dateTimeFormatter != null) {
      return LocalDateTime.parse(dateTimeString, dateTimeFormatter);
    }

    if (isUnix) {
      long epochMillis = Long.parseLong(dateTimeString) * 1000;
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), UTC);
    }

    if (isEpoch) {
      long epochMillis = Long.parseLong(dateTimeString);
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), UTC);
    }

    return null;
  }

  public LocalDate parseLocalDate(String dateTimeString) {
    if (dateTimeFormatter != null) {
      return LocalDate.parse(dateTimeString, dateTimeFormatter);
    }

    return null;
  }

  public Date parseDate(String dateTimeString) {
    if (isUnix || isEpoch) {
      return Date.from(parseZonedDateTime(dateTimeString).toInstant());
    }

    if (dateTimeFormatter == null) {
      return null;
    }

    final TemporalAccessor parseResult = dateTimeFormatter.parse(dateTimeString);
    if (parseResult.query(TemporalQueries.zone()) != null) {
      return Date.from(Instant.from(parseResult));
    }

    if (parseResult.query(TemporalQueries.localTime()) != null) {
      return Date.from(LocalDateTime.from(parseResult).toInstant(UTC));
    }

    return Date.from(LocalDate.from(parseResult).atStartOfDay(UTC).toInstant());
  }
}
