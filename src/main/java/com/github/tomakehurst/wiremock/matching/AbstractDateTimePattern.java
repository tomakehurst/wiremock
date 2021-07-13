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
package com.github.tomakehurst.wiremock.matching;

import static com.github.tomakehurst.wiremock.common.DateTimeParser.ZONED_PARSERS;
import static java.util.Collections.singletonList;

import com.github.tomakehurst.wiremock.common.DateTimeOffset;
import com.github.tomakehurst.wiremock.common.DateTimeParser;
import com.github.tomakehurst.wiremock.common.DateTimeTruncation;
import com.github.tomakehurst.wiremock.common.DateTimeUnit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public abstract class AbstractDateTimePattern extends StringValuePattern {

  private final ZonedDateTime zonedDateTime;
  private final LocalDateTime localDateTime;
  private String actualDateTimeFormat;
  private DateTimeParser actualDateTimeParser;
  private DateTimeOffset expectedOffset;
  private DateTimeTruncation truncateExpected;
  private DateTimeTruncation truncateActual;

  protected AbstractDateTimePattern(String dateTimeSpec) {
    this(dateTimeSpec, null, (DateTimeTruncation) null, null, null, null);
  }

  protected AbstractDateTimePattern(
      DateTimeOffset offset,
      String actualDateTimeFormat,
      DateTimeTruncation truncateExpected,
      DateTimeTruncation truncateActual) {
    super(buildExpectedString(offset));
    this.expectedOffset = offset;
    localDateTime = null;
    zonedDateTime = null;
    this.actualDateTimeFormat = actualDateTimeFormat;
    this.actualDateTimeParser =
        actualDateTimeFormat != null ? DateTimeParser.forFormat(actualDateTimeFormat) : null;
    this.truncateExpected = truncateExpected;
    this.truncateActual = truncateActual;
  }

  // Call this from JSON creator constructor in subclasses
  protected AbstractDateTimePattern(
      String dateTimeSpec,
      String actualDateFormat,
      String truncateExpected,
      String truncateActual,
      Integer expectedOffsetAmount,
      DateTimeUnit expectedOffsetUnit) {
    this(
        dateTimeSpec,
        actualDateFormat,
        truncateExpected != null ? DateTimeTruncation.fromString(truncateExpected) : null,
        truncateActual != null ? DateTimeTruncation.fromString(truncateActual) : null,
        expectedOffsetAmount,
        expectedOffsetUnit);
  }

  protected AbstractDateTimePattern(
      String dateTimeSpec,
      String actualDateFormat,
      DateTimeTruncation truncateExpected,
      DateTimeTruncation truncateActual,
      Integer expectedOffsetAmount,
      DateTimeUnit expectedOffsetUnit) {
    super(dateTimeSpec);

    if (isNowOffsetExpression(dateTimeSpec)) {
      zonedDateTime = null;
      localDateTime = null;
      expectedOffset =
          expectedOffsetAmount != null && expectedOffsetUnit != null
              ? new DateTimeOffset(expectedOffsetAmount, expectedOffsetUnit)
              : DateTimeOffset.fromString(dateTimeSpec);
    } else {
      zonedDateTime = parseZonedOrNull(dateTimeSpec);
      localDateTime = parseLocalOrNull(dateTimeSpec);
      expectedOffset = null;
    }

    this.actualDateTimeFormat = actualDateFormat;
    this.actualDateTimeParser =
        actualDateTimeFormat != null ? DateTimeParser.forFormat(actualDateTimeFormat) : null;

    this.truncateExpected = truncateExpected;
    this.truncateActual = truncateActual;
  }

  public AbstractDateTimePattern(ZonedDateTime zonedDateTime) {
    this(zonedDateTime.toString(), zonedDateTime, null, null, null, null, null);
  }

  public AbstractDateTimePattern(LocalDateTime localDateTime) {
    this(localDateTime.toString(), null, localDateTime, null, null, null, null);
  }

  private AbstractDateTimePattern(
      String dateTimeSpec,
      ZonedDateTime zonedDateTime,
      LocalDateTime localDateTime,
      DateTimeOffset expectedOffset,
      String actualDatetimeFormat,
      DateTimeTruncation truncateExpected,
      DateTimeTruncation truncateActual) {
    super(dateTimeSpec);
    this.zonedDateTime = zonedDateTime;
    this.localDateTime = localDateTime;
    this.expectedOffset = expectedOffset;
    this.actualDateTimeFormat = actualDatetimeFormat;
    this.actualDateTimeParser =
        actualDateTimeFormat != null ? DateTimeParser.forFormat(actualDateTimeFormat) : null;
    this.truncateExpected = truncateExpected;
    this.truncateActual = truncateActual;
  }

  @Override
  public String getValue() {
    if (expectedValue.equals("now") && expectedOffset != null) {
      return buildExpectedString(expectedOffset);
    }

    return expectedValue;
  }

  private static String buildExpectedString(DateTimeOffset dateTimeOffset) {
    return dateTimeOffset.getAmount() >= 0 ? "now +" + dateTimeOffset : "now " + dateTimeOffset;
  }

  private static boolean isNowOffsetExpression(String dateTimeSpec) {
    return dateTimeSpec.equalsIgnoreCase("now")
        || dateTimeSpec.replaceAll("(?i)now ", "").matches("^[\\-+]?[0-9]+ [a-zA-Z]+$");
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractDateTimePattern> T actualFormat(String format) {
    this.actualDateTimeFormat = format;
    this.actualDateTimeParser = DateTimeParser.forFormat(format);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractDateTimePattern> T expectedOffset(int amount, DateTimeUnit unit) {
    this.expectedOffset = new DateTimeOffset(amount, unit);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractDateTimePattern> T expectedOffset(DateTimeOffset offset) {
    this.expectedOffset = offset;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractDateTimePattern> T truncateExpected(DateTimeTruncation truncation) {
    this.truncateExpected = truncation;
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public <T extends AbstractDateTimePattern> T truncateActual(DateTimeTruncation truncation) {
    this.truncateActual = truncation;
    return (T) this;
  }

  public String getActualFormat() {
    return actualDateTimeFormat;
  }

  public String getTruncateExpected() {
    return stringOrNull(truncateExpected);
  }

  public String getTruncateActual() {
    return stringOrNull(truncateActual);
  }

  private static String stringOrNull(Object obj) {
    return obj != null ? obj.toString() : null;
  }

  @Override
  public MatchResult match(String value) {
    final ZonedDateTime zonedActual =
        truncateActual != null
            ? truncateActual.truncate(parseZonedOrNull(value, actualDateTimeParser))
            : parseZonedOrNull(value, actualDateTimeParser);

    final LocalDateTime localActual = parseLocalOrNull(value, actualDateTimeParser);

    final ZonedDateTime zonedExpectedDateTime =
        isNowOffset() ? calculateExpectedFromNow() : zonedDateTime;

    return getMatchResult(zonedExpectedDateTime, localDateTime, zonedActual, localActual);
  }

  private ZonedDateTime calculateExpectedFromNow() {
    final ZonedDateTime now = ZonedDateTime.now();
    final ZonedDateTime truncated = truncateExpected != null ? truncateExpected.truncate(now) : now;

    return expectedOffset.shift(truncated);
  }

  protected abstract MatchResult getMatchResult(
      ZonedDateTime zonedExpected,
      LocalDateTime localExpected,
      ZonedDateTime zonedActual,
      LocalDateTime localActual);

  private boolean isNowOffset() {
    return expectedOffset != null;
  }

  private static ZonedDateTime parseZonedOrNull(String dateTimeString) {
    return parseZonedOrNull(dateTimeString, (DateTimeParser) null);
  }

  private static ZonedDateTime parseZonedOrNull(String dateTimeString, DateTimeParser parser) {
    final List<DateTimeParser> parsers = parser != null ? singletonList(parser) : ZONED_PARSERS;
    return parseZonedOrNull(dateTimeString, parsers);
  }

  private static ZonedDateTime parseZonedOrNull(
      String dateTimeString, List<DateTimeParser> parsers) {
    if (parsers.isEmpty()) {
      return null;
    }

    try {
      return parsers.get(0).parseZonedDateTime(dateTimeString);
    } catch (DateTimeParseException e) {
      return parseZonedOrNull(dateTimeString, parsers.subList(1, parsers.size()));
    }
  }

  private static LocalDateTime parseLocalOrNull(String dateTimeString) {
    return parseLocalOrNull(dateTimeString, null);
  }

  private static LocalDateTime parseLocalOrNull(String dateTimeString, DateTimeParser parser) {
    try {
      return parser != null
          ? parser.parseLocalDateTime(dateTimeString)
          : LocalDateTime.parse(dateTimeString);
    } catch (DateTimeParseException ignored) {
      try {
        return (parser != null
                ? parser.parseLocalDate(dateTimeString)
                : LocalDate.parse(dateTimeString))
            .atStartOfDay();
      } catch (DateTimeParseException ignored2) {
        return null;
      }
    }
  }
}
