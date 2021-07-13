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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class MatchResultTest {

  @Test
  public void aggregatesLazily() {
    final MatchResult result1 = new ExceptionThrowingMatchResult();
    final MatchResult result2 = new ExceptionThrowingMatchResult();
    final MatchResult result3 = new ExceptionThrowingMatchResult();

    MatchResult.aggregate(
        result1, result2,
        result3); // Expecting no exception to be thrown because getDistance is never called
  }

  @Test
  public void aggregatesDistanceCorrectly() {
    MatchResult matchResult =
        MatchResult.aggregate(
            MatchResult.partialMatch(0.5),
            MatchResult.partialMatch(0.6),
            MatchResult.partialMatch(0.7));

    assertThat(matchResult.getDistance(), is(0.6));
  }

  @Test
  public void aggregatesExactMatchCorrectly() {
    MatchResult matchResult =
        MatchResult.aggregate(
            MatchResult.exactMatch(),
            MatchResult.exactMatch(),
            MatchResult.exactMatch(),
            MatchResult.exactMatch());

    assertThat(matchResult.isExactMatch(), is(true));
  }

  @Test
  public void aggregatesNonExactMatchCorrectly() {
    MatchResult matchResult =
        MatchResult.aggregate(
            MatchResult.exactMatch(),
            MatchResult.exactMatch(),
            MatchResult.partialMatch(0.99),
            MatchResult.exactMatch());

    assertThat(matchResult.isExactMatch(), is(false));
  }

  public static class ExceptionThrowingMatchResult extends MatchResult {

    @Override
    public boolean isExactMatch() {
      return false;
    }

    @Override
    public double getDistance() {
      throw new UnsupportedOperationException();
    }
  }
}
