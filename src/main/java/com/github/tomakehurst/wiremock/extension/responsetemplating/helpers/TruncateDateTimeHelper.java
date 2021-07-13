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
import com.github.tomakehurst.wiremock.common.DateTimeTruncation;
import java.io.IOException;
import java.util.Date;

public class TruncateDateTimeHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    if (options.params.length < 1) {
      return handleError(
          "Truncation type must be specified as the first parameter to truncateDate");
    }

    if (context instanceof Date) {
      Date date = (Date) context;
      DateTimeTruncation truncation = DateTimeTruncation.fromString(options.params[0].toString());
      return truncation.truncate(date);
    }

    return handleError("A date object must be passed to the truncateDate helper");
  }
}
