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

import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.*;
import java.util.Collections;
import java.util.List;

public class QueryParameter extends MultiValue {

  @JsonCreator
  public QueryParameter(
      @JsonProperty("key") String key, @JsonProperty("values") List<String> values) {
    super(key, values);
  }

  public static QueryParameter queryParam(String key, String... values) {
    return new QueryParameter(key, asList(values));
  }

  public static QueryParameter absent(String key) {
    return new QueryParameter(key, Collections.<String>emptyList());
  }

  @JsonIgnore
  @Override
  public boolean isPresent() {
    return super.isPresent();
  }

  @JsonProperty
  @Override
  public String key() {
    return super.key();
  }

  @JsonProperty
  @Override
  public List<String> values() {
    return super.values();
  }

  @JsonIgnore
  @Override
  public boolean isSingleValued() {
    return super.isSingleValued();
  }
}
