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
import java.util.Objects;

public abstract class PathPattern extends StringValuePattern {

  protected final StringValuePattern valuePattern;

  protected PathPattern(String expectedValue, StringValuePattern valuePattern) {
    super(expectedValue);
    this.valuePattern = valuePattern;
  }

  public StringValuePattern getValuePattern() {
    return valuePattern;
  }

  @JsonIgnore
  public boolean isSimple() {
    return valuePattern == null;
  }

  @Override
  public MatchResult match(String value) {
    if (isSimple()) {
      return isSimpleMatch(value);
    }

    return isAdvancedMatch(value);
  }

  protected abstract MatchResult isSimpleMatch(String value);

  protected abstract MatchResult isAdvancedMatch(String value);

  public abstract String getExpressionResult(String value);

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    PathPattern that = (PathPattern) o;
    return Objects.equals(expectedValue, that.expectedValue)
        && Objects.equals(valuePattern, that.valuePattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), valuePattern);
  }

  protected static class SubExpressionException extends RuntimeException {
    public SubExpressionException(String message) {
      super(message);
    }

    public SubExpressionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
