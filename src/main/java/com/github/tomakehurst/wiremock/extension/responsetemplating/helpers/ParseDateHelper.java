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
package com.github.tomakehurst.wiremock.extension.responsetemplating.helpers;

import static com.github.tomakehurst.wiremock.common.DateTimeParser.ZONED_PARSERS;
import static java.util.Collections.singletonList;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.DateTimeParser;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

public class ParseDateHelper extends HandlebarsHelper<String> {

  @Override
  public Object apply(String dateTimeString, Options options) throws IOException {
    String format = options.hash("format", null);

    return format == null
        ? parseOrNull(dateTimeString)
        : parseOrNull(dateTimeString, DateTimeParser.forFormat(format));
  }

  private static Date parseOrNull(String dateTimeString) {
    return parseOrNull(dateTimeString, (DateTimeParser) null);
  }

  private static Date parseOrNull(String dateTimeString, DateTimeParser parser) {
    final List<DateTimeParser> parsers = parser != null ? singletonList(parser) : ZONED_PARSERS;
    return parseOrNull(dateTimeString, parsers);
  }

  private static Date parseOrNull(String dateTimeString, List<DateTimeParser> parsers) {
    if (parsers.isEmpty()) {
      return null;
    }

    try {
      final DateTimeParser headParser = parsers.get(0);
      return headParser.parseDate(dateTimeString);
    } catch (DateTimeParseException e) {
      return parseOrNull(dateTimeString, parsers.subList(1, parsers.size()));
    }
  }
}
