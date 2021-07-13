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
package com.github.tomakehurst.wiremock.common;

import static com.github.tomakehurst.wiremock.common.ListFunctions.splitByType;
import static com.github.tomakehurst.wiremock.common.Pair.pair;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

import java.util.Collections;
import org.junit.Test;

public class ListFunctionsTest {

  @Test
  public void emptyArrayReturnsTwoEmptyLists() {
    Number[] input = new Number[0];

    assertEquals(
        pair(Collections.<Number>emptyList(), Collections.<Integer>emptyList()),
        splitByType(input, Integer.class));
  }

  @Test
  public void singletonArrayNonMatchingReturnsSingletonAndEmptyList() {
    Number[] input = new Number[] {1L};

    assertEquals(
        pair(singletonList(1L), Collections.<Integer>emptyList()),
        splitByType(input, Integer.class));
  }

  @Test
  public void singletonArrayMatchingReturnsEmptyAndSingletonList() {
    Number[] input = new Number[] {1};

    assertEquals(
        pair(Collections.<Number>emptyList(), singletonList(1)), splitByType(input, Integer.class));
  }

  @Test
  public void splitsTheArrayAsExpected() {
    Number[] input = new Number[] {1, 1L, 2, 2L, 3, 3L};

    assertEquals(pair(asList(1L, 2L, 3L), asList(1, 2, 3)), splitByType(input, Integer.class));
  }
}
