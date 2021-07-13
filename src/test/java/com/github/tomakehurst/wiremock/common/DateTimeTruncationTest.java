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

import java.time.ZonedDateTime;
import org.junit.Test;

public class DateTimeTruncationTest {

  @Test
  public void firstSecondOfMinute() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_SECOND_OF_MINUTE.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-06-18T10:11:00Z")));
  }

  @Test
  public void firstMinuteOfHour() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_MINUTE_OF_HOUR.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-06-18T10:00:00Z")));
  }

  @Test
  public void firstHourOfDay() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_HOUR_OF_DAY.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-06-18T00:00:00Z")));
  }

  @Test
  public void firstDayOfMonth() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_DAY_OF_MONTH.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-06-01T00:00:00Z")));
  }

  @Test
  public void firstDayOfNextMonth() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_DAY_OF_NEXT_MONTH.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-07-01T00:00:00Z")));
  }

  @Test
  public void lastDayOfMonth() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.LAST_DAY_OF_MONTH.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-06-30T00:00:00Z")));
  }

  @Test
  public void firstDayOfYear() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_DAY_OF_YEAR.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-01-01T00:00:00Z")));
  }

  @Test
  public void firstDayOfNextYear() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.FIRST_DAY_OF_NEXT_YEAR.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2022-01-01T00:00:00Z")));
  }

  @Test
  public void lastDayOfYear() {
    ZonedDateTime input = ZonedDateTime.parse("2021-06-18T10:11:12Z");
    ZonedDateTime output = DateTimeTruncation.LAST_DAY_OF_YEAR.truncate(input);
    assertThat(output, is(ZonedDateTime.parse("2021-12-31T00:00:00Z")));
  }

  @Test
  public void parsesFromFriendlyString() {
    assertThat(
        DateTimeTruncation.fromString("last day of year"), is(DateTimeTruncation.LAST_DAY_OF_YEAR));
  }

  @Test
  public void toStringReturnsFriendlyString() {
    assertThat(DateTimeTruncation.FIRST_DAY_OF_MONTH.toString(), is("first day of month"));
  }
}
