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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.DateTimeOffset;
import com.github.tomakehurst.wiremock.common.DateTimeTruncation;
import com.github.tomakehurst.wiremock.common.Json;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import org.junit.Test;

public class EqualToDateTimePatternTest {

  @Test
  public void matchesZonedToZoned() {
    StringValuePattern matcher = WireMock.equalToDateTime("2021-06-14T12:13:14Z");

    assertTrue(matcher.match("2021-06-14T12:13:14Z").isExactMatch());
    assertFalse(matcher.match("1921-06-14T12:13:14Z").isExactMatch());
  }

  @Test
  public void matchesLiteralDateTimesWithDifferentZones() {
    StringValuePattern matcher = WireMock.equalToDateTime("2021-06-24T13:40:27+01:00");

    assertTrue(matcher.match("2021-06-24T12:40:27Z").isExactMatch());
    assertFalse(matcher.match("2021-06-24T13:40:27Z").isExactMatch());
  }

  @Test
  public void matchesLocalToLocal() {
    StringValuePattern matcher = WireMock.equalToDateTime("2021-06-14T12:13:14");

    assertTrue(matcher.match("2021-06-14T12:13:14").isExactMatch());
    assertFalse(matcher.match("1921-06-14T12:13:14").isExactMatch());
  }

  @Test
  public void matchesLocalToZoned() {
    StringValuePattern matcher = WireMock.equalToDateTime("2021-06-14T12:13:14");

    assertTrue(matcher.match("2021-06-14T12:13:14Z").isExactMatch());
    assertFalse(matcher.match("1921-06-14T12:13:14Z").isExactMatch());
  }

  @Test
  public void matchesActualInUnixTimeFormat() {
    String dateTime = "2021-06-14T12:13:14Z";
    StringValuePattern matcher = WireMock.equalToDateTime(dateTime).actualFormat("unix");

    String good = String.valueOf(Instant.parse(dateTime).getEpochSecond());
    String bad = String.valueOf(Instant.parse(dateTime).minusMillis(10).getEpochSecond());

    assertTrue(matcher.match(good).isExactMatch());
    assertFalse(matcher.match(bad).isExactMatch());
  }

  @Test
  public void matchesActualInEpochTimeFormat() {
    String dateTime = "2021-06-14T12:13:14Z";
    StringValuePattern matcher = WireMock.equalToDateTime(dateTime).actualFormat("epoch");

    String good = String.valueOf(Instant.parse(dateTime).toEpochMilli());
    String bad = String.valueOf(Instant.parse(dateTime).minusMillis(10).toEpochMilli());

    assertTrue(matcher.match(good).isExactMatch());
    assertFalse(matcher.match(bad).isExactMatch());
  }

  @Test
  public void returnsAReasonableDistanceWhenNoMatchForLocalExpectedZonedActual() {
    StringValuePattern matcher = WireMock.equalToDateTime("2021-01-01T00:00:00Z");
    assertThat(matcher.match("2071-01-01T00:00:00Z").getDistance(), is(0.5));
    assertThat(matcher.match("2121-01-01T00:00:00Z").getDistance(), is(1.0));
    assertThat(matcher.match("2022-01-01T00:00:00Z").getDistance(), is(0.01));
  }

  @Test
  public void serialisesToJson() {
    EqualToDateTimePattern matcher =
        WireMock.isNow()
            .expectedOffset(DateTimeOffset.fromString("now -5 days"))
            .truncateExpected(DateTimeTruncation.LAST_DAY_OF_MONTH)
            .truncateActual(DateTimeTruncation.FIRST_DAY_OF_YEAR);

    assertThat(
        Json.write(matcher),
        jsonEquals(
            "{\n"
                + "  \"equalToDateTime\": \"now -5 days\",\n"
                + "  \"truncateExpected\": \"last day of month\",\n"
                + "  \"truncateActual\": \"first day of year\"\n"
                + "}"));
  }

  @Test
  public void deserialisesFromJson() {
    StringValuePattern matcher =
        Json.read(
            "{\n"
                + "  \"equalToDateTime\": \"now\",\n"
                + "  \"truncateExpected\": \"first hour of day\",\n"
                + "  \"truncateActual\": \"first hour of day\"\n"
                + "}",
            EqualToDateTimePattern.class);

    ZonedDateTime good = ZonedDateTime.now().truncatedTo(DAYS);
    ZonedDateTime bad = ZonedDateTime.now().truncatedTo(DAYS).minus(5, HOURS);

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void acceptsJavaZonedDateTimeAsExpected() {
    EqualToDateTimePattern matcher =
        WireMock.equalToDateTime(ZonedDateTime.parse("2020-08-29T00:00:00Z"));
    assertTrue(matcher.match("2020-08-29T00:00:00Z").isExactMatch());
  }

  @Test
  public void acceptsJavaLocalDateTimeAsExpected() {
    EqualToDateTimePattern matcher =
        WireMock.equalToDateTime(LocalDateTime.parse("2020-08-29T00:00:00"));
    assertTrue(matcher.match("2020-08-29T00:00:00").isExactMatch());
  }
}
