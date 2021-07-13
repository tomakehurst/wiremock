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

import static com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.HelperUtils.coerceToInt;

import com.github.jknack.handlebars.Options;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RangeHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    Integer lowerBound = coerceToInt(context);
    Integer upperBound = options.params.length > 0 ? coerceToInt(options.param(0)) : null;

    if (lowerBound == null || upperBound == null) {
      return handleError(
          "The range helper requires both lower and upper bounds as integer parameters");
    }

    int limit = (upperBound - lowerBound) + 1;
    return Stream.iterate(lowerBound, n -> n + 1).limit(limit).collect(Collectors.toList());
  }
}
