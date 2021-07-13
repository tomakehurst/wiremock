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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;

public class ContainsPatternTest {

  @Test
  public void returnsExactMatchWhenExpectedValueWhollyContainedInTestValue() {
    assertTrue(WireMock.containing("thing").match("mythings").isExactMatch());
  }

  @Test
  public void returnsNoMatchWhenExpectedValueNotContainedInTestValue() {
    MatchResult matchResult = WireMock.containing("thing").match("otherstuff");
    assertFalse(matchResult.isExactMatch());
    assertThat(matchResult.getDistance(), is(1.0));
  }
}
