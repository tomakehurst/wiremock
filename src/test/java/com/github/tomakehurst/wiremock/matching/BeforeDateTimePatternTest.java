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

import static com.github.tomakehurst.wiremock.common.DateTimeTruncation.*;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.DateTimeUnit;
import com.github.tomakehurst.wiremock.common.Json;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import org.junit.Test;

public class BeforeDateTimePatternTest {

  @Test
  public void matchesZonedISO8601BeforeZonedLiteralDateTime() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15Z");

    assertTrue(matcher.match("2021-06-01T15:15:15Z").isExactMatch());
    assertFalse(matcher.match("2021-07-01T23:59:59Z").isExactMatch());
  }

  @Test
  public void doesNotMatchLocalISO8601BeforeZonedLiteralDateTime() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15Z");

    // Shouldn't match even if it's apparently correct as a local -> zoned comparison does not make
    // sense
    assertFalse(matcher.match("2021-06-01T15:15:15").isExactMatch());
    assertFalse(matcher.match("2021-07-01T23:59:59").isExactMatch());
  }

  @Test
  public void matchesLocalISO8601BeforeLocalLiteralDateTime() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15");

    assertTrue(matcher.match("2021-06-01T15:15:15").isExactMatch());
    assertFalse(matcher.match("2021-07-01T23:59:59").isExactMatch());
  }

  @Test
  public void matchesZonedISO8601BeforeLocalLiteralDateTime() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15");

    assertTrue(matcher.match("2021-06-01T15:15:15Z").isExactMatch());
    assertFalse(matcher.match("2021-07-01T23:59:59Z").isExactMatch());
  }

  @Test
  public void doesNotMatchWhenActualValueUnparseable() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15");
    assertFalse(matcher.match("2021-06-01T15:15:blahsdfj123").isExactMatch());
  }

  @Test
  public void doesNotMatchWhenExpectedValueUnparseable() {
    StringValuePattern matcher = WireMock.before("2021-06-wrongstuff:15:15");
    assertFalse(matcher.match("2021-06-01T15:15:15Z").isExactMatch());
  }

  @Test
  public void returnsAReasonableDistanceWhenNoMatchForZonedExpectedZonedActual() {
    StringValuePattern matcher = WireMock.before("2021-01-01T00:00:00Z");
    assertThat(matcher.match("2071-01-01T00:00:00Z").getDistance(), is(0.5));
    assertThat(matcher.match("2121-01-01T00:00:00Z").getDistance(), is(1.0));
    assertThat(matcher.match("2022-01-01T00:00:00Z").getDistance(), is(0.01));
  }

  @Test
  public void returnsAReasonableDistanceWhenNoMatchForLocalExpectedZonedActual() {
    StringValuePattern matcher = WireMock.before("2021-01-01T00:00:00");
    assertThat(matcher.match("2071-01-01T00:00:00Z").getDistance(), is(0.5));
    assertThat(matcher.match("2121-01-01T00:00:00Z").getDistance(), is(1.0));
    assertThat(matcher.match("2022-01-01T00:00:00Z").getDistance(), is(0.01));
  }

  @Test
  public void returnsAReasonableDistanceWhenNoMatchForLocalExpectedLocalActual() {
    StringValuePattern matcher = WireMock.before("2021-01-01T00:00:00");
    assertThat(matcher.match("2071-01-01T00:00:00").getDistance(), is(0.5));
    assertThat(matcher.match("2121-01-01T00:00:00").getDistance(), is(1.0));
    assertThat(matcher.match("2022-01-01T00:00:00").getDistance(), is(0.01));
  }

  @Test
  public void matchesZonedRFC1123ActualDate() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15Z");

    assertTrue(matcher.match("Tue, 01 Jun 2021 15:16:17 GMT").isExactMatch());
    assertFalse(matcher.match("Thu, 01 Jul 2021 15:16:17 GMT").isExactMatch());
  }

  @Test
  public void matchesZonedRFC1036ActualDate() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15Z");

    assertTrue(matcher.match("Tuesday, 01-Jun-21 14:14:14 GMT").isExactMatch());
    assertFalse(matcher.match("Thursday, 01-Jul-21 15:16:17 GMT").isExactMatch());
  }

  @Test
  public void matchesZonedSingleDigitDayAsctimeActualDate() {
    StringValuePattern matcher = WireMock.before("2021-06-14T01:01:01Z");

    assertTrue(matcher.match("Tue Jun  1 01:01:01 2021").isExactMatch());
    assertFalse(matcher.match("Thu Jul  1 01:01:01 2021").isExactMatch());
  }

  @Test
  public void matchesZonedDoubleDigitDayAsctimeActualDate() {
    StringValuePattern matcher = WireMock.before("2021-06-14T01:01:01Z");

    assertTrue(matcher.match("Thu Jun 10 01:01:01 2021").isExactMatch());
    assertFalse(matcher.match("Sat Jul 10 01:01:01 2021").isExactMatch());
  }

  @Test
  public void matchesNonUTCZonedISO8601ActualDate() {
    StringValuePattern matcher = WireMock.before("2021-06-14T15:15:15Z");

    assertTrue(matcher.match("2021-06-14T15:15:15+01:00[Europe/London]").isExactMatch());
    assertFalse(matcher.match("2021-06-14T16:15:15+01:00[Europe/London]").isExactMatch());
  }

  @Test
  public void matchesActualDateAccordingToSpecifiedFormat() {
    StringValuePattern matcher = WireMock.before("2021-06-14").actualFormat("dd/MM/yyyy");

    assertTrue(matcher.match("01/06/2021").isExactMatch());
    assertFalse(matcher.match("01/07/2021").isExactMatch());
  }

  @Test
  public void matchesAgainstNow() {
    StringValuePattern matcher = WireMock.beforeNow();

    String right = ZonedDateTime.now().minusDays(2).toString();
    assertTrue(matcher.match(right).isExactMatch());

    String wrong = ZonedDateTime.now().plusHours(4).toString();
    assertFalse(matcher.match(wrong).isExactMatch());
  }

  @Test
  public void matchesAgainstOffsetFromNow() {
    StringValuePattern matcher = WireMock.before("now -5 days");

    String right = ZonedDateTime.now().minusDays(7).toString();
    assertTrue(matcher.match(right).isExactMatch());

    String wrong = ZonedDateTime.now().minusDays(4).toString();
    assertFalse(matcher.match(wrong).isExactMatch());
  }

  @Test
  public void truncatesExpectedDateToSpecifiedUnit() {
    StringValuePattern matcher =
        WireMock.before("15 days")
            .truncateExpected(FIRST_DAY_OF_MONTH); // Before the 15th of this month

    TemporalAdjuster truncateToMonth = TemporalAdjusters.firstDayOfMonth();
    ZonedDateTime good = ZonedDateTime.now().with(truncateToMonth).plus(14, ChronoUnit.DAYS);
    ZonedDateTime bad = ZonedDateTime.now().with(truncateToMonth).plus(16, ChronoUnit.DAYS);

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void truncatesActualDateToSpecifiedUnit() {
    StringValuePattern matcher =
        WireMock.before("15 days")
            .truncateExpected(FIRST_DAY_OF_MONTH)
            .truncateActual(LAST_DAY_OF_MONTH);

    ZonedDateTime good = ZonedDateTime.now().minusMonths(1); // A month ago from now
    ZonedDateTime bad =
        ZonedDateTime.now()
            .with(TemporalAdjusters.lastDayOfMonth())
            .minusDays(1); // Second-last day of this month

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void serialisesLiteralDateTimeAndFormatFormToJson() {
    StringValuePattern matcher = WireMock.before("2021-06-01T00:00:00").actualFormat("dd/MM/yyyy");

    assertThat(
        Json.write(matcher),
        jsonEquals(
            "{\n"
                + "  \"before\": \"2021-06-01T00:00:00\",\n"
                + "  \"actualFormat\": \"dd/MM/yyyy\"\n"
                + "}"));
  }

  @Test
  public void serialisesOffsetWithActualTruncationFormToJson() {
    StringValuePattern matcher =
        WireMock.beforeNow()
            .expectedOffset(15, DateTimeUnit.DAYS)
            .truncateActual(FIRST_DAY_OF_MONTH);

    assertThat(
        Json.write(matcher),
        jsonEquals(
            "{\n"
                + "  \"before\": \"now +15 days\",\n"
                + "  \"truncateActual\": \"first day of month\"\n"
                + "}"));
  }

  @Test
  public void serialisesOffsetWithExpectedAndActualTruncationFormToJson() {
    StringValuePattern matcher =
        WireMock.beforeNow()
            .expectedOffset(15, DateTimeUnit.DAYS)
            .truncateExpected(FIRST_HOUR_OF_DAY)
            .truncateActual(FIRST_DAY_OF_MONTH);

    assertThat(
        Json.write(matcher),
        jsonEquals(
            "{\n"
                + "  \"before\": \"now +15 days\",\n"
                + "  \"truncateExpected\": \"first hour of day\",\n"
                + "  \"truncateActual\": \"first day of month\"\n"
                + "}"));
  }

  @Test
  public void deserialisesLiteralDateAndTimeWithFormatFromJson() {
    BeforeDateTimePattern matcher =
        Json.read(
            "{\n"
                + "  \"before\": \"2021-06-15T00:00:00\",\n"
                + "  \"actualFormat\": \"dd/MM/yyyy\"\n"
                + "}",
            BeforeDateTimePattern.class);

    assertThat(matcher.getExpected(), is("2021-06-15T00:00:00"));
    assertThat(matcher.getActualFormat(), is("dd/MM/yyyy"));
  }

  @Test
  public void deserialisesPositiveOffsetAndTruncateFormFromJson() {
    BeforeDateTimePattern matcher =
        Json.read(
            "{\n"
                + "  \"before\": \"15 days\",\n"
                + "  \"truncateActual\": \"first day of year\"\n"
                + "}",
            BeforeDateTimePattern.class);

    assertThat(matcher.getTruncateExpected(), nullValue());
    assertThat(matcher.getTruncateActual(), is("first day of year"));
  }

  @Test
  public void deserialisesNegativeOffsetFormFromJson() {
    StringValuePattern matcher =
        Json.read("{\n" + "  \"before\": \"-15 days\"\n" + "}", BeforeDateTimePattern.class);

    ZonedDateTime good = ZonedDateTime.now().minus(16, ChronoUnit.DAYS);
    ZonedDateTime bad = ZonedDateTime.now().minus(14, ChronoUnit.DAYS);

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void deserialisesOffsetWithSeparateAmountAndUnitAttributesFromJson() {
    BeforeDateTimePattern matcher =
        Json.read(
            "{\n"
                + "  \"before\": \"now\",\n"
                + "  \"expectedOffset\": -15,\n"
                + "  \"expectedOffsetUnit\": \"days\"\n"
                + "}\n",
            BeforeDateTimePattern.class);

    ZonedDateTime good = ZonedDateTime.now().minus(16, ChronoUnit.DAYS);
    ZonedDateTime bad = ZonedDateTime.now().minus(14, ChronoUnit.DAYS);

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void acceptsJavaZonedDateTimeAsExpected() {
    BeforeDateTimePattern matcher = WireMock.before(ZonedDateTime.parse("2020-08-29T00:00:00Z"));
    assertTrue(matcher.match("2019-01-01T00:00:00Z").isExactMatch());
  }

  @Test
  public void acceptsJavaLocalDateTimeAsExpected() {
    BeforeDateTimePattern matcher = WireMock.before(LocalDateTime.parse("2020-08-29T00:00:00"));
    assertTrue(matcher.match("2019-01-01T00:00:00").isExactMatch());
  }
}
