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
import com.github.jknack.handlebars.TagType;
import java.io.IOException;
import java.util.Collection;

public class ContainsHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    if (options.params.length < 1) {
      return handleError(
          "You must specify the string or array to be matched and the regular expression");
    }

    String expected = options.param(0);

    if (context == null || expected == null) {
      return false;
    }

    boolean isMatch =
        Collection.class.isAssignableFrom(context.getClass())
            ? ((Collection<?>) context).contains(expected)
            : context.toString().contains(expected);

    if (options.tagType == TagType.SECTION) {
      return isMatch ? options.apply(options.fn) : "";
    }

    return isMatch;
  }
}
