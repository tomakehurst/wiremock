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
package com.github.tomakehurst.wiremock.common;

import java.net.URI;
import org.apache.commons.lang3.StringUtils;

public class UniqueFilenameGenerator {

  public static String generate(String url, String prefix, String id) {
    return generate(url, prefix, id, "json");
  }

  public static String generate(String url, String prefix, String id, String extension) {
    String pathPart = Urls.urlToPathParts(URI.create(url));
    pathPart = pathPart.equals("") ? "(root)" : sanitise(pathPart);

    if (pathPart.length() > 150) {
      pathPart = StringUtils.truncate(pathPart, 150);
    }

    return new StringBuilder(prefix)
        .append("-")
        .append(pathPart)
        .append("-")
        .append(id)
        .append(".")
        .append(extension)
        .toString();
  }

  private static String sanitise(String input) {
    return input.replaceAll("[,~:/?#\\[\\]@!\\$&'()*+;=]", "_");
  }
}
