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

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Calendar;

public enum DateTimeUnit {
  SECONDS(Calendar.SECOND),
  MINUTES(Calendar.MINUTE),
  HOURS(Calendar.HOUR),
  DAYS(Calendar.DAY_OF_MONTH),
  MONTHS(Calendar.MONTH),
  YEARS(Calendar.YEAR);

  private final int calendarField;

  DateTimeUnit(int calendarField) {
    this.calendarField = calendarField;
  }

  public int getCalendarField() {
    return calendarField;
  }

  public TemporalUnit toTemporalUnit() {
    return ChronoUnit.valueOf(name());
  }
}
