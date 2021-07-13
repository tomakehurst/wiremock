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

import static com.google.common.base.MoreObjects.firstNonNull;

import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import java.util.Map;

public class FormDataHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) {
    Map<String, ListOrSingle<String>> formData =
        FormParser.parse(
            context.toString(),
            Boolean.TRUE.equals(options.hash.get("urlDecode")),
            firstNonNull(options.hash.get("encoding"), "utf-8").toString());

    if (options.params.length > 0) {
      String variableName = options.param(0);
      options.context.data(variableName, formData);
      return null;
    }

    return formData;
  }
}
