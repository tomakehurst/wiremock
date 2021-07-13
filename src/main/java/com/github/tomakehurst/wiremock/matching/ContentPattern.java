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
package com.github.tomakehurst.wiremock.matching;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Preconditions;

@JsonDeserialize(using = ContentPatternDeserialiser.class)
public abstract class ContentPattern<T> implements NamedValueMatcher<T> {

  protected final T expectedValue;

  public ContentPattern(T expectedValue) {
    if (!isNullValuePermitted()) {
      Preconditions.checkNotNull(
          expectedValue, "'" + getName() + "' expected value cannot be null");
    }
    this.expectedValue = expectedValue;
  }

  @JsonIgnore
  public T getValue() {
    return expectedValue;
  }

  protected boolean isNullValuePermitted() {
    return false;
  }
}
