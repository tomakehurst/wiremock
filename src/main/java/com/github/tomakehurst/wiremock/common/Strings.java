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

import static com.google.common.base.Charsets.UTF_8;

import java.nio.charset.Charset;
import org.apache.commons.lang3.text.WordUtils;

public class Strings {
  public static final Charset DEFAULT_CHARSET = UTF_8;

  public static String stringFromBytes(byte[] bytes) {
    return stringFromBytes(bytes, DEFAULT_CHARSET);
  }

  public static String stringFromBytes(byte[] bytes, Charset charset) {
    if (bytes == null) {
      return null;
    }

    return new String(bytes, charset);
  }

  public static byte[] bytesFromString(String str) {
    return bytesFromString(str, DEFAULT_CHARSET);
  }

  public static byte[] bytesFromString(String str, Charset charset) {
    if (str == null) {
      return null;
    }

    return str.getBytes(charset);
  }

  public static String wrapIfLongestLineExceedsLimit(String s, int maxLineLength) {
    int longestLength = findLongestLineLength(s);
    if (longestLength > maxLineLength) {
      String wrapped = WordUtils.wrap(s, maxLineLength, null, true);
      return wrapped.replaceAll("(?m)^[ \t]*\r?\n", "");
    }

    return s;
  }

  private static int findLongestLineLength(String s) {
    String[] lines = s.split("\n");
    int longestLength = 0;
    for (String line : lines) {
      int length = line.length();
      if (length > longestLength) {
        longestLength = length;
      }
    }

    return longestLength;
  }
}
