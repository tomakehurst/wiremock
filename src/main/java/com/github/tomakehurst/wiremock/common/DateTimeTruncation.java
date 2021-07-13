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
import static java.time.temporal.ChronoUnit.*;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.function.Function;

public enum DateTimeTruncation {
  FIRST_SECOND_OF_MINUTE(input -> input.truncatedTo(MINUTES)),
  FIRST_MINUTE_OF_HOUR(input -> input.truncatedTo(HOURS)),
  FIRST_HOUR_OF_DAY(input -> input.truncatedTo(DAYS)),

  FIRST_DAY_OF_MONTH(input -> input.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(DAYS)),
  FIRST_DAY_OF_NEXT_MONTH(
      input -> input.with(TemporalAdjusters.firstDayOfNextMonth()).truncatedTo(DAYS)),
  LAST_DAY_OF_MONTH(input -> input.with(TemporalAdjusters.lastDayOfMonth()).truncatedTo(DAYS)),

  FIRST_DAY_OF_YEAR(input -> input.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(DAYS)),
  FIRST_DAY_OF_NEXT_YEAR(
      input -> input.with(TemporalAdjusters.firstDayOfNextYear()).truncatedTo(DAYS)),
  LAST_DAY_OF_YEAR(input -> input.with(TemporalAdjusters.lastDayOfYear()).truncatedTo(DAYS));

  private final Function<ZonedDateTime, ZonedDateTime> fn;

  DateTimeTruncation(Function<ZonedDateTime, ZonedDateTime> fn) {
    this.fn = fn;
  }

  public ZonedDateTime truncate(ZonedDateTime input) {
    return fn.apply(input);
  }

  public Date truncate(Date input) {
    final ZonedDateTime zonedInput = input.toInstant().atZone(UTC);
    return Date.from(truncate(zonedInput).toInstant());
  }

  @Override
  public String toString() {
    return name().replace('_', ' ').toLowerCase();
  }

  public static DateTimeTruncation fromString(String value) {
    final String normalisedKey = value.toUpperCase().replace(' ', '_');
    return valueOf(normalisedKey);
  }
}
