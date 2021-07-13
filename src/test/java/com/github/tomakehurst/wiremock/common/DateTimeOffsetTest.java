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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import java.text.DateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import org.junit.Test;

public class DateTimeOffsetTest {

  static final DateFormat ISO8601 = new ISO8601DateFormat();

  @Test
  public void parsesSecondsOffset() {
    DateTimeOffset offset = DateTimeOffset.fromString("7 seconds");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.SECONDS));
    assertThat(offset.getAmount(), is(7));
  }

  @Test
  public void parsesMinutesOffset() {
    DateTimeOffset offset = DateTimeOffset.fromString("78 minutes");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.MINUTES));
    assertThat(offset.getAmount(), is(78));
  }

  @Test
  public void parsesHoursOffset() {
    DateTimeOffset offset = DateTimeOffset.fromString("-12 hours");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.HOURS));
    assertThat(offset.getAmount(), is(-12));
  }

  @Test
  public void parsesDaysOffset() {
    DateTimeOffset offset = DateTimeOffset.fromString("1 days");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.DAYS));
    assertThat(offset.getAmount(), is(1));
  }

  @Test
  public void parsesMonthsOffset() {
    DateTimeOffset offset = DateTimeOffset.fromString("-12 months");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.MONTHS));
    assertThat(offset.getAmount(), is(-12));
  }

  @Test
  public void parsesYearsOffset() {
    DateTimeOffset offset = DateTimeOffset.fromString("101 years");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.YEARS));
    assertThat(offset.getAmount(), is(101));
  }

  @Test
  public void parsesPositiveLongForm() {
    DateTimeOffset offset = DateTimeOffset.fromString("now +101 years");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.YEARS));
    assertThat(offset.getAmount(), is(101));
  }

  @Test
  public void parsesNegativeLongForm() {
    DateTimeOffset offset = DateTimeOffset.fromString("now -5 months");
    assertThat(offset.getAmountUnit(), is(DateTimeUnit.MONTHS));
    assertThat(offset.getAmount(), is(-5));
  }

  @Test
  public void returnsCorrectToString() {
    assertThat(DateTimeOffset.fromString("123 minutes").toString(), is("123 minutes"));
    assertThat(DateTimeOffset.fromString("-72 hours").toString(), is("-72 hours"));
  }

  @Test
  public void canBeConstructedFromParts() {
    assertThat(new DateTimeOffset(67, DateTimeUnit.DAYS).toString(), is("67 days"));
    assertThat(new DateTimeOffset(-12, DateTimeUnit.SECONDS).toString(), is("-12 seconds"));
  }

  @Test
  public void shiftsZonedDateTimes() {
    DateTimeOffset positiveDateOffset = new DateTimeOffset(10, DateTimeUnit.DAYS);
    assertThat(
        positiveDateOffset.shift(ZonedDateTime.parse("2021-06-18T00:00:00Z")),
        is(ZonedDateTime.parse("2021-06-28T00:00:00Z")));

    DateTimeOffset negativeDateOffset = new DateTimeOffset(-4, DateTimeUnit.MONTHS);
    assertThat(
        negativeDateOffset.shift(ZonedDateTime.parse("2021-06-18T00:00:00Z")),
        is(ZonedDateTime.parse("2021-02-18T00:00:00Z")));
  }

  @Test
  public void offsetsProvidedDateByConfiguredAmount() throws Exception {
    DateTimeOffset offset = DateTimeOffset.fromString("3 days");
    Date startingDate = ISO8601.parse("2018-04-16T12:01:01Z");
    Date finalDate = offset.shift(startingDate);

    assertThat(ISO8601.format(finalDate), is("2018-04-19T12:01:01Z"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionWhenUnparseableStringProvided() {
    DateTimeOffset.fromString("101");
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionWhenUnparseableUnitProvided() {
    DateTimeOffset.fromString("101 squillions");
  }
}
