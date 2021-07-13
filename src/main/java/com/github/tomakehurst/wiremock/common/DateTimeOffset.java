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

import static com.github.tomakehurst.wiremock.common.DateTimeUnit.SECONDS;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateTimeOffset {

  public static final DateTimeOffset NONE = new DateTimeOffset(0, SECONDS);

  private final DateTimeUnit amountUnit;
  private final int amount;

  public static DateTimeOffset fromString(String offset) {
    if (offset.equalsIgnoreCase("now")) {
      return NONE;
    }

    String[] parts = offset.split(" ");
    if (parts.length < 2 || parts.length > 3) {
      throw new IllegalArgumentException(
          "Offset can be of the short form <amount> <unit> e.g. 8 seconds or long form now +/-<amount> <unit> e.g. now +5 years");
    }

    int amount;
    DateTimeUnit amountUnit;

    if (parts.length == 2) {
      amount = Integer.parseInt(parts[0]);
      amountUnit = DateTimeUnit.valueOf(parts[1].toUpperCase());
    } else {
      amount = Integer.parseInt(parts[1]);
      amountUnit = DateTimeUnit.valueOf(parts[2].toUpperCase());
    }

    return new DateTimeOffset(amount, amountUnit);
  }

  public DateTimeOffset(int amount, DateTimeUnit amountUnit) {
    this.amountUnit = amountUnit;
    this.amount = amount;
  }

  public DateTimeUnit getAmountUnit() {
    return amountUnit;
  }

  public int getAmount() {
    return amount;
  }

  public Date shift(Date date) {
    if (this == NONE) {
      return date;
    }

    final ZonedDateTime input = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.of("Z"));
    ZonedDateTime output = shift(input);
    return Date.from(output.toInstant());
  }

  public ZonedDateTime shift(ZonedDateTime dateTime) {
    if (this == NONE) {
      return dateTime;
    }

    return dateTime.plus(amount, amountUnit.toTemporalUnit());
  }

  @Override
  public String toString() {
    return amount + " " + amountUnit.name().toLowerCase();
  }
}
