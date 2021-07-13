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

import static org.apache.commons.lang3.StringUtils.getLevenshteinDistance;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class EqualToPattern extends StringValuePattern {

  private final Boolean caseInsensitive;

  public EqualToPattern(
      @JsonProperty("equalTo") String testValue,
      @JsonProperty("caseInsensitive") Boolean caseInsensitive) {
    super(testValue);
    this.caseInsensitive = caseInsensitive;
  }

  public EqualToPattern(String expectedValue) {
    this(expectedValue, null);
  }

  public String getEqualTo() {
    return expectedValue;
  }

  public Boolean getCaseInsensitive() {
    return caseInsensitive;
  }

  @Override
  public MatchResult match(final String value) {
    return new MatchResult() {
      @Override
      public boolean isExactMatch() {
        return shouldMatchCaseInsensitive()
            ? value != null && value.equalsIgnoreCase(expectedValue)
            : Objects.equals(expectedValue, value);
      }

      @Override
      public double getDistance() {
        return normalisedLevenshteinDistance(expectedValue, value);
      }
    };
  }

  private boolean shouldMatchCaseInsensitive() {
    return caseInsensitive != null && caseInsensitive;
  }

  private double normalisedLevenshteinDistance(String one, String two) {
    if (one == null || two == null) {
      return 1.0;
    }

    double maxDistance = Math.max(one.length(), two.length());
    double actualDistance = getLevenshteinDistance(one, two);
    return (actualDistance / maxDistance);
  }
}
