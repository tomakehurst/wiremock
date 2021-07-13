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
package com.github.tomakehurst.wiremock.http;

import com.google.common.base.Function;

public class CaseInsensitiveKey {

  private final String key;

  /** Cache the hash code for the key */
  private int hash; // Default to 0

  public CaseInsensitiveKey(String key) {
    this.key = key;
  }

  public static CaseInsensitiveKey from(String key) {
    return new CaseInsensitiveKey(key);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CaseInsensitiveKey that = (CaseInsensitiveKey) o;

    if (key != null ? !key.equalsIgnoreCase(that.key) : that.key != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0 && key.length() > 0) {
      for (int i = 0; i < key.length(); i++) {
        char c = Character.toLowerCase(key.charAt(i));
        h = 31 * h + c;
      }
      hash = h;
    }
    return h;
  }

  @Override
  public String toString() {
    return key;
  }

  public String value() {
    return key;
  }

  public static final Function<String, CaseInsensitiveKey> TO_CASE_INSENSITIVE_KEYS =
      new Function<String, CaseInsensitiveKey>() {
        public CaseInsensitiveKey apply(String input) {
          return from(input);
        }
      };
}
