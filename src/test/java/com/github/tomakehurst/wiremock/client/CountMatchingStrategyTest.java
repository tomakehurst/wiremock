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
package com.github.tomakehurst.wiremock.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

public class CountMatchingStrategyTest {

  @Test
  public void shouldMatchLessThanCorrectly() {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.LESS_THAN, 5);

    assertThat(countStrategy.match(3), is(true));
    assertThat(countStrategy.match(5), is(false));
    assertThat(countStrategy.match(7), is(false));
  }

  @Test
  public void shouldMatchLessThanOrEqualCorrectly() {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.LESS_THAN_OR_EQUAL, 5);

    assertThat(countStrategy.match(3), is(true));
    assertThat(countStrategy.match(5), is(true));
    assertThat(countStrategy.match(7), is(false));
  }

  @Test
  public void shouldMatchEqualToCorrectly() {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 5);

    assertThat(countStrategy.match(3), is(false));
    assertThat(countStrategy.match(5), is(true));
    assertThat(countStrategy.match(7), is(false));
  }

  @Test
  public void shouldMatchGreaterThanOrEqualCorrectly() {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 5);

    assertThat(countStrategy.match(3), is(false));
    assertThat(countStrategy.match(5), is(true));
    assertThat(countStrategy.match(7), is(true));
  }

  @Test
  public void shouldMatchGreaterThanCorrectly() {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN, 5);

    assertThat(countStrategy.match(3), is(false));
    assertThat(countStrategy.match(5), is(false));
    assertThat(countStrategy.match(7), is(true));
  }

  @Test
  public void shouldCorrectlyObtainFriendlyNameForLessThanMode() throws Exception {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.LESS_THAN, 5);
    assertThat(countStrategy.toString(), is("Less than 5"));
  }

  @Test
  public void shouldCorrectlyObtainFriendlyNameForLessThanOrEqualMode() throws Exception {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.LESS_THAN_OR_EQUAL, 5);
    assertThat(countStrategy.toString(), is("Less than or exactly 5"));
  }

  @Test
  public void shouldCorrectlyObtainFriendlyNameForEqualMode() throws Exception {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.EQUAL_TO, 5);
    assertThat(countStrategy.toString(), is("Exactly 5"));
  }

  @Test
  public void shouldCorrectlyObtainFriendlyNameForGreaterThanOrEqualMode() throws Exception {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN_OR_EQUAL, 5);
    assertThat(countStrategy.toString(), is("More than or exactly 5"));
  }

  @Test
  public void shouldCorrectlyObtainFriendlyNameForGreaterThanMode() throws Exception {
    CountMatchingStrategy countStrategy =
        new CountMatchingStrategy(CountMatchingStrategy.GREATER_THAN, 5);
    assertThat(countStrategy.toString(), is("More than 5"));
  }
}
