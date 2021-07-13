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
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;

public class HandlebarsRandomValuesHelper extends HandlebarsHelper<Void> {

  @Override
  public Object apply(Void context, Options options) throws IOException {
    int length = options.hash("length", 36);
    boolean uppercase = options.hash("uppercase", false);

    String type = options.hash("type", "ALPHANUMERIC");
    String rawValue;

    switch (type) {
      case "ALPHANUMERIC":
        rawValue = RandomStringUtils.randomAlphanumeric(length);
        break;
      case "ALPHABETIC":
        rawValue = RandomStringUtils.randomAlphabetic(length);
        break;
      case "NUMERIC":
        rawValue = RandomStringUtils.randomNumeric(length);
        break;
      case "ALPHANUMERIC_AND_SYMBOLS":
        rawValue = RandomStringUtils.random(length, 33, 126, false, false);
        break;
      case "UUID":
        rawValue = UUID.randomUUID().toString();
        break;
      case "HEXADECIMAL":
        rawValue = RandomStringUtils.random(length, "ABCDEF0123456789");
        break;
      default:
        rawValue = RandomStringUtils.randomAscii(length);
        break;
    }
    return uppercase ? rawValue.toUpperCase() : rawValue.toLowerCase();
  }
}
