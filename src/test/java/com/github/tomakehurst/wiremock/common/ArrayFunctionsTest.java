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

import static com.github.tomakehurst.wiremock.common.ArrayFunctions.concat;
import static com.github.tomakehurst.wiremock.common.ArrayFunctions.prepend;
import static org.junit.Assert.*;

import org.junit.Test;

public class ArrayFunctionsTest {

  private final Integer[] empty = new Integer[0];

  @Test
  public void concatEmptyAndEmpty() {
    assertArrayEquals(empty, concat(empty, empty));
  }

  @Test
  public void concatNonEmptyAndEmpty() {
    Integer[] first = {1, 2};

    Integer[] result = concat(first, empty);
    assertArrayEquals(new Integer[] {1, 2}, result);

    first[0] = 10;
    assertArrayEquals(new Integer[] {1, 2}, result);
  }

  @Test
  public void concatEmptyAndNonEmpty() {
    Integer[] second = {1, 2};

    Integer[] result = concat(empty, second);
    assertArrayEquals(new Integer[] {1, 2}, result);

    second[0] = 10;
    assertArrayEquals(new Integer[] {1, 2}, result);
  }

  @Test
  public void concatNonEmptyAndNonEmpty() {
    Integer[] first = {1, 2};
    Integer[] second = {3, 4};

    Integer[] result = concat(first, second);
    assertArrayEquals(new Integer[] {1, 2, 3, 4}, result);

    first[0] = 10;
    second[0] = 30;
    assertArrayEquals(new Integer[] {1, 2, 3, 4}, result);
  }

  @Test
  public void prependNullAndEmpty() {
    assertArrayEquals(new Integer[] {null}, prepend(null, empty));
  }

  @Test
  public void prependSomeAndEmpty() {
    Integer[] result = prepend(1, empty);
    assertArrayEquals(new Integer[] {1}, result);
  }

  @Test
  public void prependNullAndNonEmpty() {
    Integer[] second = {1, 2};

    Integer[] result = prepend(null, second);
    assertArrayEquals(new Integer[] {null, 1, 2}, result);

    second[0] = 10;
    assertArrayEquals(new Integer[] {null, 1, 2}, result);
  }

  @Test
  public void prependSomeAndNonEmpty() {
    Integer[] second = {2, 3};

    Integer[] result = prepend(1, second);
    assertArrayEquals(new Integer[] {1, 2, 3}, result);

    second[0] = 30;
    assertArrayEquals(new Integer[] {1, 2, 3}, result);
  }
}
