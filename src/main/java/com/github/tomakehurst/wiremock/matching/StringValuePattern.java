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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.lang.reflect.Constructor;

@JsonDeserialize(using = StringValuePatternJsonDeserializer.class)
public abstract class StringValuePattern extends ContentPattern<String> {

  public StringValuePattern(String expectedValue) {
    super(expectedValue);
  }

  @JsonIgnore
  public boolean isPresent() {
    return !nullSafeIsAbsent();
  }

  public Boolean isAbsent() {
    return !nullSafeIsAbsent() ? null : true;
  }

  @JsonIgnore
  public boolean nullSafeIsAbsent() {
    return false;
  }

  @Override
  public String toString() {
    return getName() + " " + getValue();
  }

  public final String getName() {
    Constructor<?> constructor =
        FluentIterable.from(this.getClass().getDeclaredConstructors())
            .firstMatch(
                new Predicate<Constructor<?>>() {
                  @Override
                  public boolean apply(Constructor<?> input) {
                    return (input.getParameterAnnotations().length > 0
                        && input.getParameterAnnotations()[0].length > 0
                        && input.getParameterAnnotations()[0][0] instanceof JsonProperty);
                  }
                })
            .orNull();

    if (constructor == null) {
      throw new IllegalStateException(
          "Constructor must have a first parameter annotatated with JsonProperty(\"<operator name>\")");
    }
    JsonProperty jsonPropertyAnnotation =
        (JsonProperty) constructor.getParameterAnnotations()[0][0];
    return jsonPropertyAnnotation.value();
  }

  @Override
  public String getExpected() {
    return getValue();
  }

  public LogicalAnd and(StringValuePattern other) {
    return new LogicalAnd(this, other);
  }

  public LogicalOr or(StringValuePattern other) {
    return new LogicalOr(this, other);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StringValuePattern that = (StringValuePattern) o;
    return Objects.equal(expectedValue, that.expectedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(expectedValue);
  }
}
