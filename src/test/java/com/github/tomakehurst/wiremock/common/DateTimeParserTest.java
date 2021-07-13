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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Test;

public class DateTimeParserTest {

  @Test
  public void parsesFromDateTimeFormatter() {
    DateTimeParser parser = DateTimeParser.forFormatter(DateTimeFormatter.ISO_DATE_TIME);
    assertThat(
        parser.parseZonedDateTime("2021-06-23T11:12:13Z"),
        is(ZonedDateTime.parse("2021-06-23T11:12:13Z")));

    assertThat(
        parser.parseLocalDateTime("2021-06-23T11:12:13"),
        is(LocalDateTime.parse("2021-06-23T11:12:13")));
  }

  @Test
  public void parsesZonedFromFormatString() {
    DateTimeParser parser = DateTimeParser.forFormat("dd/MM/yyyy HH:mm:ss Z");
    assertThat(
        parser.parseZonedDateTime("23/06/2021 11:22:33 +0000"),
        is(ZonedDateTime.parse("2021-06-23T11:22:33Z")));
  }

  @Test
  public void parsesLocalDateTimeFromFormatString() {
    DateTimeParser parser = DateTimeParser.forFormat("dd/MM/yyyy HH:mm:ss");
    assertThat(
        parser.parseLocalDateTime("23/06/2021 11:12:13"),
        is(LocalDateTime.parse("2021-06-23T11:12:13")));
  }

  @Test
  public void parsesLocalDateFromFormatString() {
    DateTimeParser parser = DateTimeParser.forFormat("dd/MM/yyyy");
    assertThat(parser.parseLocalDate("23/06/2021"), is(LocalDate.parse("2021-06-23")));
  }

  @Test
  public void parsesUnix() {
    DateTimeParser parser = DateTimeParser.forFormat("unix");
    assertThat(
        parser.parseZonedDateTime("1624447353"), is(ZonedDateTime.parse("2021-06-23T11:22:33Z")));

    assertThat(
        parser.parseLocalDateTime("1624447353"), is(LocalDateTime.parse("2021-06-23T11:22:33")));
  }

  @Test
  public void parsesEpoch() {
    DateTimeParser parser = DateTimeParser.forFormat("epoch");
    assertThat(
        parser.parseZonedDateTime("1624447353000"),
        is(ZonedDateTime.parse("2021-06-23T11:22:33Z")));

    assertThat(
        parser.parseLocalDateTime("1624447353000"), is(LocalDateTime.parse("2021-06-23T11:22:33")));
  }
}
