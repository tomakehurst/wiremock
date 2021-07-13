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

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class SafeNames {

  private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\w-]");
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  public static String makeSafeFileName(StubMapping mapping) {
    return makeSafeFileName(mapping, "json");
  }

  public static String makeSafeFileName(StubMapping mapping, String extension) {
    String suffix = "-" + mapping.getId() + "." + extension;
    if (isNotEmpty(mapping.getName())) {
      return makeSafeName(mapping.getName()) + suffix;
    }

    UrlPattern urlMatcher = mapping.getRequest().getUrlMatcher();

    if (urlMatcher.getPattern() instanceof AnythingPattern) {
      return suffix.substring(1);
    }

    String expectedUrl = urlMatcher.getExpected();
    URI uri = URI.create(sanitise(expectedUrl));
    return makeSafeNameFromUrl(uri.getPath()) + suffix;
  }

  public static String makeSafeNameFromUrl(String urlPath) {
    String startingPath = urlPath.replace("/", "_");
    return makeSafeName(startingPath);
  }

  public static String makeSafeName(String name) {
    String nowhitespace = WHITESPACE.matcher(name).replaceAll("-");
    String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
    String slug = sanitise(normalized);

    slug = slug.replaceAll("^[_]*", "");
    slug = slug.replaceAll("[_]*$", "");

    slug = StringUtils.truncate(slug, 200);

    return slug.toLowerCase(Locale.ENGLISH);
  }

  private static String sanitise(String s) {
    return NON_ALPHANUMERIC.matcher(s).replaceAll("");
  }
}
