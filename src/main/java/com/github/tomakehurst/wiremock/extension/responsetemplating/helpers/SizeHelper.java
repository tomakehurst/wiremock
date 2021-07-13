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
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SizeHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    Class<?> contextClass = context.getClass();

    if (CharSequence.class.isAssignableFrom(contextClass)) {
      return ((CharSequence) context).length();
    }

    if (List.class.isAssignableFrom(contextClass)) {
      return ((List) context).size();
    }

    if (Map.class.isAssignableFrom(contextClass)) {
      return ((Map) context).size();
    }

    return null;
  }
}
