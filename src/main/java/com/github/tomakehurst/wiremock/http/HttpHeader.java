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

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Arrays.asList;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;

public class HttpHeader extends MultiValue {

  public HttpHeader(String key, String... values) {
    super(key, asList(values));
  }

  public HttpHeader(CaseInsensitiveKey key, Collection<String> values) {
    super(key.value(), ImmutableList.copyOf(values));
  }

  public HttpHeader(String key, Collection<String> values) {
    super(key, ImmutableList.copyOf(firstNonNull(values, Collections.<String>emptyList())));
  }

  public static HttpHeader httpHeader(CaseInsensitiveKey key, String... values) {
    return new HttpHeader(key.value(), values);
  }

  public static HttpHeader httpHeader(String key, String... values) {
    return new HttpHeader(key, values);
  }

  public static HttpHeader absent(String key) {
    return new HttpHeader(key);
  }

  public static HttpHeader empty(String key) {
    return httpHeader(key, "");
  }

  public CaseInsensitiveKey caseInsensitiveKey() {
    return CaseInsensitiveKey.from(key);
  }

  public boolean keyEquals(String candidateKey) {
    return CaseInsensitiveKey.from(candidateKey).equals(caseInsensitiveKey());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HttpHeader that = (HttpHeader) o;

    if (key != null ? !key.equalsIgnoreCase(that.key) : that.key != null) return false;
    if (values != null ? !values.equals(that.values) : that.values != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = key != null ? key.toLowerCase().hashCode() : 0;
    result = 31 * result + (values != null ? values.hashCode() : 0);
    return result;
  }
}
