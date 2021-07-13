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

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.DateTimeOffset;
import java.io.IOException;
import java.util.Date;

public class HandlebarsCurrentDateHelper extends HandlebarsHelper<Date> {

  @Override
  public Object apply(Date context, Options options) throws IOException {
    String format = options.hash("format", null);
    String offset = options.hash("offset", null);
    String timezone = options.hash("timezone", null);

    Date date = context != null ? context : new Date();
    if (offset != null) {
      date = DateTimeOffset.fromString(offset).shift(date);
    }

    return new RenderableDate(date, format, timezone);
  }
}
