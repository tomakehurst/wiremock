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
import com.github.tomakehurst.wiremock.common.ListOrSingle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexExtractHelper extends HandlebarsHelper<Object> {

  @Override
  public Object apply(Object context, Options options) {
    List<String> groups = new ArrayList<>();
    String regexString = options.param(0);
    Pattern regex;

    try {
      regex = Pattern.compile(regexString);
    } catch (PatternSyntaxException ex) {
      return handleError("Invalid regex string " + regexString);
    }

    Matcher matcher = regex.matcher(context.toString());

    while (matcher.find()) {

      if (options.params.length == 1) {
        return matcher.group();
      }

      for (int i = 1; i <= matcher.groupCount(); i++) {
        groups.add(matcher.group(i));
      }
    }

    if (groups.isEmpty()) {
      Object defaultValue = options.hash("default");
      return defaultValue != null ? defaultValue : handleError("Nothing matched " + regexString);
    }

    String variableName = options.param(1);
    options.context.data(variableName, new ListOrSingle<>(groups));

    return null;
  }
}
