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

public class WeightedMatchResult {

  private final MatchResult matchResult;
  private final double weighting;

  public static WeightedMatchResult weight(MatchResult matchResult, double weighting) {
    return new WeightedMatchResult(matchResult, weighting);
  }

  public static WeightedMatchResult weight(MatchResult matchResult) {
    return new WeightedMatchResult(matchResult);
  }

  public WeightedMatchResult(MatchResult matchResult) {
    this(matchResult, 1.0);
  }

  public WeightedMatchResult(MatchResult matchResult, double weighting) {
    this.matchResult = matchResult;
    this.weighting = weighting;
  }

  public boolean isExactMatch() {
    return matchResult.isExactMatch();
  }

  public double getDistance() {
    return weighting * matchResult.getDistance();
  }

  public double getWeighting() {
    return weighting;
  }
}
