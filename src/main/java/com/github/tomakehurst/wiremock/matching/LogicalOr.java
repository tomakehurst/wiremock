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

import static java.util.Arrays.asList;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.stream.Collectors;

public class LogicalOr extends StringValuePattern {

  private final List<StringValuePattern> operands;

  public LogicalOr(StringValuePattern... operands) {
    this(asList(operands));
  }

  public LogicalOr(@JsonProperty("or") List<StringValuePattern> operands) {
    super(
        operands.stream()
            .findFirst()
            .map(ContentPattern::getValue)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Logical OR must be constructed with at least two matchers")));
    this.operands = operands;
  }

  @Override
  public String getExpected() {
    return operands.stream()
        .map(contentPattern -> contentPattern.getName() + " " + contentPattern.getExpected())
        .collect(Collectors.joining(" OR "));
  }

  public List<StringValuePattern> getOr() {
    return operands;
  }

  @Override
  public MatchResult match(String value) {
    final List<MatchResult> matchResults =
        operands.stream().map(matcher -> matcher.match(value)).collect(Collectors.toList());

    return new MatchResult() {
      @Override
      public boolean isExactMatch() {
        return matchResults.stream().anyMatch(MatchResult::isExactMatch);
      }

      @Override
      public double getDistance() {
        return matchResults.stream().map(MatchResult::getDistance).sorted().findFirst().get();
      }
    };
  }
}
