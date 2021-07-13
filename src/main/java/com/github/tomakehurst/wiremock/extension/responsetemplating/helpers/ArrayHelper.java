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

import static java.util.Arrays.asList;

import com.github.jknack.handlebars.Options;
import com.google.common.collect.ImmutableList;
import java.io.IOException;

public class ArrayHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) throws IOException {
    if (context == null || context == options.context.model()) {
      return ImmutableList.of();
    }

    return ImmutableList.builder().add(context).addAll(asList(options.params)).build();
  }
}
