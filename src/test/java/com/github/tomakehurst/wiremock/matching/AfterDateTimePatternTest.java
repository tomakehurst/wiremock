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

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.Test;

public class AfterDateTimePatternTest {

  @Test
  public void matchesZonedToZoned() {
    StringValuePattern matcher = WireMock.after("2021-06-14T12:13:14Z");

    assertTrue(matcher.match("2022-06-14T12:13:14Z").isExactMatch());
    assertFalse(matcher.match("2020-06-14T12:13:14Z").isExactMatch());
  }

  @Test
  public void matchesLocalToLocal() {
    StringValuePattern matcher = WireMock.after("2021-06-14T12:13:14");

    assertTrue(matcher.match("2022-06-14T12:13:14").isExactMatch());
    assertFalse(matcher.match("2020-06-14T12:13:14").isExactMatch());
  }

  @Test
  public void matchesLocalToZoned() {
    StringValuePattern matcher = WireMock.after("2021-06-14T12:13:14");

    assertTrue(matcher.match("2022-06-14T12:13:14Z").isExactMatch());
    assertFalse(matcher.match("2020-06-14T12:13:14Z").isExactMatch());
  }

  @Test
  public void matchesZonedToNowOffset() {
    StringValuePattern matcher = WireMock.afterNow().expectedOffset(27, DateTimeUnit.MINUTES);

    ZonedDateTime good = ZonedDateTime.now().plusHours(1);
    ZonedDateTime bad = ZonedDateTime.now().minusMinutes(1);
    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void matchesNowWithExpectedAndActualTruncated() {
    StringValuePattern matcher =
        WireMock.afterNow()
            .truncateExpected(DateTimeTruncation.FIRST_DAY_OF_MONTH)
            .truncateActual(DateTimeTruncation.LAST_DAY_OF_MONTH);

    ZonedDateTime good = ZonedDateTime.now();
    ZonedDateTime bad = ZonedDateTime.now().minusMonths(1).minusHours(1);

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void returnsAReasonableDistanceWhenNoMatchForLocalExpectedZonedActual() {
    StringValuePattern matcher = WireMock.after("2021-01-01T00:00:00Z");
    assertThat(matcher.match("1971-01-01T00:00:00Z").getDistance(), is(0.5));
    assertThat(matcher.match("1921-01-01T00:00:00Z").getDistance(), is(1.0));
    assertThat(matcher.match("2020-01-01T00:00:00Z").getDistance(), is(0.01));
  }

  @Test
  public void serialisesToJson() {
    AfterDateTimePattern matcher =
        WireMock.afterNow()
            .expectedOffset(DateTimeOffset.fromString("now -5 days"))
            .truncateExpected(DateTimeTruncation.LAST_DAY_OF_MONTH)
            .truncateActual(DateTimeTruncation.FIRST_DAY_OF_YEAR);

    assertThat(
        Json.write(matcher),
        jsonEquals(
            "{\n"
                + "  \"after\": \"now -5 days\",\n"
                + "  \"truncateExpected\": \"last day of month\",\n"
                + "  \"truncateActual\": \"first day of year\"\n"
                + "}"));
  }

  @Test
  public void deserialisesFromJson() {
    AfterDateTimePattern matcher =
        Json.read(
            "{\n"
                + "  \"after\": \"now\",\n"
                + "  \"truncateExpected\": \"first hour of day\",\n"
                + "  \"truncateActual\": \"last day of year\"\n"
                + "}",
            AfterDateTimePattern.class);

    assertThat(matcher.getExpected(), is("now +0 seconds"));
    assertThat(matcher.getTruncateExpected(), is("first hour of day"));
    assertThat(matcher.getTruncateActual(), is("last day of year"));
  }

  @Test
  public void deserialisesOffsetWithSeparateAmountAndUnitAttributesFromJson() {
    AfterDateTimePattern matcher =
        Json.read(
            "{\n"
                + "  \"after\": \"now\",\n"
                + "  \"expectedOffset\": -15,\n"
                + "  \"expectedOffsetUnit\": \"days\"\n"
                + "}\n",
            AfterDateTimePattern.class);

    ZonedDateTime good = ZonedDateTime.now().minus(14, ChronoUnit.DAYS);
    ZonedDateTime bad = ZonedDateTime.now().minus(16, ChronoUnit.DAYS);

    assertTrue(matcher.match(good.toString()).isExactMatch());
    assertFalse(matcher.match(bad.toString()).isExactMatch());
  }

  @Test
  public void acceptsJavaZonedDateTimeAsExpected() {
    AfterDateTimePattern matcher = WireMock.after(ZonedDateTime.parse("2020-08-29T00:00:00Z"));
    assertTrue(matcher.match("2021-01-01T00:00:00Z").isExactMatch());
  }

  @Test
  public void acceptsJavaLocalDateTimeAsExpected() {
    AfterDateTimePattern matcher = WireMock.after(LocalDateTime.parse("2020-08-29T00:00:00"));
    assertTrue(matcher.match("2021-01-01T00:00:00").isExactMatch());
  }
}
