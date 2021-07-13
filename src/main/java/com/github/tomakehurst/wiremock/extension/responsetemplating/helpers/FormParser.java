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

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;

import com.github.tomakehurst.wiremock.common.ListOrSingle;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class FormParser {

  public static Map<String, ListOrSingle<String>> parse(String formText, boolean urlDecode) {
    return parse(formText, urlDecode, "utf-8");
  }

  public static Map<String, ListOrSingle<String>> parse(
      String formText, boolean urlDecode, String encoding) {
    Map<String, ListOrSingle<String>> map = new LinkedHashMap<>();

    for (String formField : formText.split("&")) {
      String[] parts = formField.split("=");
      if (parts.length > 1) {
        String key = parts[0];
        String value = urlDecode ? urlDecode(parts[1].trim(), encoding) : parts[1].trim();

        ListOrSingle<String> existing = map.get(key);
        if (existing != null) {
          existing.add(value);
        } else {
          map.put(key, ListOrSingle.of(value));
        }
      }
    }

    return map;
  }

  private static String urlDecode(String text, String encoding) {
    try {
      return URLDecoder.decode(text, encoding);
    } catch (Exception e) {
      return throwUnchecked(e, String.class);
    }
  }
}
